package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.collection.OrderedMap;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.Bar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.values.AmmoListValue;

import java.io.*;

public class ItemTurret extends CooledTurret{
    protected int maxAmmo = 30;
    protected ObjectMap<Item, BulletType> ammo = new ObjectMap<>();

    public ItemTurret(String name){
        super(name);
        hasItems = true;
    }

    /** Initializes accepted ammo map. Format: [item1, bullet1, item2, bullet2...] */
    protected void ammo(Object... objects){
        ammo = OrderedMap.of(objects);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.itemCapacity);
        stats.add(BlockStat.ammo, new AmmoListValue<>(ammo));
    }

    @Override
    public void onProximityAdded(Tile tile){
        super.onProximityAdded(tile);

        //add first ammo item to cheaty blocks so they can shoot properly
        if(tile.isEnemyCheat() && ammo.size > 0){
            handleItem(ammo.entries().next().key, tile, tile);
        }
    }

    @Override
    public void displayBars(Tile tile, Table bars){
        super.displayBars(tile, bars);

        TurretEntity entity = tile.entity();

        bars.add(new Bar("blocks.ammo", Pal.ammo, () -> (float)entity.totalAmmo / maxAmmo)).growX();
        bars.row();
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        TurretEntity entity = tile.entity();

        BulletType type = ammo.get(item);

        if(type == null) return 0;

        return Math.min((int)((maxAmmo - entity.totalAmmo) / ammo.get(item).ammoMultiplier), amount);
    }

    @Override
    public void handleStack(Item item, int amount, Tile tile, Unit source){
        for(int i = 0; i < amount; i++){
            handleItem(item, tile, null);
        }
    }

    //currently can't remove items from turrets.
    @Override
    public int removeStack(Tile tile, Item item, int amount){
        return 0;
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        TurretEntity entity = tile.entity();
        if(entity == null) return;

        BulletType type = ammo.get(item);
        entity.totalAmmo += type.ammoMultiplier;

        //find ammo entry by type
        for(int i = 0; i < entity.ammo.size; i++){
            ItemEntry entry = (ItemEntry)entity.ammo.get(i);

            //if found, put it to the right
            if(entry.item == item){
                entry.amount += type.ammoMultiplier;
                entity.ammo.swap(i, entity.ammo.size - 1);
                return;
            }
        }

        //must not be found
        entity.ammo.add(new ItemEntry(item, (int)type.ammoMultiplier));
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        TurretEntity entity = tile.entity();

        return ammo != null && ammo.get(item) != null && entity.totalAmmo + ammo.get(item).ammoMultiplier <= maxAmmo;
    }

    @Override
    public TileEntity newEntity(){
        return new ItemTurretEntity();
    }

    public class ItemTurretEntity extends TurretEntity{
        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(ammo.size);
            for(AmmoEntry entry : ammo){
                ItemEntry i = (ItemEntry)entry;
                stream.writeByte(i.item.id);
                stream.writeShort(i.amount);
            }
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            byte amount = stream.readByte();
            for(int i = 0; i < amount; i++){
                Item item = Vars.content.item(stream.readByte());
                short a = stream.readShort();
                totalAmmo += a;
                ammo.add(new ItemEntry(item, a));
            }
        }
    }

    class ItemEntry extends AmmoEntry{
        protected Item item;

        ItemEntry(Item item, int amount){
            this.item = item;
            this.amount = amount;
        }

        @Override
        public BulletType type(){
            return ammo.get(item);
        }
    }
}
