package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.struct.*;
import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.Cicon;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import java.io.*;

import static mindustry.Vars.*;

public class ItemTurret extends CooledTurret{
    public int maxAmmo = 30;
    public ObjectMap<Item, BulletType> ammo = new ObjectMap<>();

    public ItemTurret(String name){
        super(name);
        hasItems = true;
        entityType = ItemTurretEntity::new;
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
        consumes.add(new ConsumeItemFilter(i -> ammo.containsKey(i)){
            @Override
            public void build(Tile tile, Table table){
                MultiReqImage image = new MultiReqImage();
                content.items().each(i -> filter.get(i) && (!world.isZone() || data.isUnlocked(i)), item -> image.add(new ReqImage(new ItemImage(item.icon(Cicon.medium)),
                    () -> tile.entity != null && !((ItemTurretEntity)tile.entity).ammo.isEmpty() && ((ItemEntry)tile.<ItemTurretEntity>ent().ammo.peek()).item == item)));

                table.add(image).size(8 * 4);
            }

            @Override
            public boolean valid(TileEntity entity){
                //valid when there's any ammo in the turret
                return !((ItemTurretEntity)entity).ammo.isEmpty();
            }

            @Override
            public void display(BlockStats stats){
                //don't display
            }
        });
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

        TurretEntity entity = tile.ent();

        bars.add(new Bar("blocks.ammo", Pal.ammo, () -> (float)entity.totalAmmo / maxAmmo)).growX();
        bars.row();
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        TurretEntity entity = tile.ent();

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
        TurretEntity entity = tile.ent();
        if(entity == null) return;

        if(item == Items.pyratite){
            Events.fire(Trigger.flameAmmo);
        }

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

        //fire events for the tutorial
        if(state.rules.tutorial){
            Events.fire(new TurretAmmoDeliverEvent());
        }
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        TurretEntity entity = tile.ent();

        return ammo != null && ammo.get(item) != null && entity.totalAmmo + ammo.get(item).ammoMultiplier <= maxAmmo;
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
