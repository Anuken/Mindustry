package io.anuke.mindustry.world.blocks.defense.turrets;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.type.AmmoEntry;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.values.ItemFilterValue;

public class ItemTurret extends CooledTurret{
    protected int maxAmmo = 50;
    protected AmmoType[] ammoTypes;
    protected ObjectMap<Item, AmmoType> ammoMap = new ObjectMap<>();

    public ItemTurret(String name){
        super(name);
        hasItems = true;
    }

    public AmmoType[] getAmmoTypes(){
        return ammoTypes;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.itemCapacity);

        stats.add(BlockStat.inputItems, new ItemFilterValue(item -> ammoMap.containsKey(item)));
    }

    @Override
    public int acceptStack(Item item, int amount, Tile tile, Unit source){
        TurretEntity entity = tile.entity();

        AmmoType type = ammoMap.get(item);

        if(type == null) return 0;

        return Math.min((int) ((maxAmmo - entity.totalAmmo) / ammoMap.get(item).quantityMultiplier), amount);
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

        AmmoType type = ammoMap.get(item);
        entity.totalAmmo += type.quantityMultiplier;
        entity.items.add(item, 1);

        //find ammo entry by type
        for(int i = 0; i < entity.ammo.size; i++){
            AmmoEntry entry = entity.ammo.get(i);

            //if found, put it to the right
            if(entry.type == type){
                entry.amount += type.quantityMultiplier;
                entity.ammo.swap(i, entity.ammo.size - 1);
                return;
            }
        }

        //must not be found
        AmmoEntry entry = new AmmoEntry(type, (int) type.quantityMultiplier);
        entity.ammo.add(entry);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        TurretEntity entity = tile.entity();

        return ammoMap != null && ammoMap.get(item) != null && entity.totalAmmo + ammoMap.get(item).quantityMultiplier <= maxAmmo;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.replace(new BlockBar(BarType.inventory, true, tile -> (float) tile.<TurretEntity>entity().totalAmmo / maxAmmo));
    }

    @Override
    public void init(){
        super.init();

        if(ammoTypes != null){
            for(AmmoType type : ammoTypes){
                if(type.item == null) continue;
                if(ammoMap.containsKey(type.item)){
                    throw new RuntimeException("Turret \"" + name + "\" has two conflicting ammo entries on item type " + type.item + "!");
                }else{
                    ammoMap.put(type.item, type);
                }
            }
        }
    }
}
