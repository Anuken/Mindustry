package io.anuke.mindustry.world.blocks.types.defense.turrets;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.type.AmmoEntry;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.Turret;

public class ItemTurret extends Turret {
    protected int maxammo = 100;
    //TODO implement this!
    /**A value of 'null' means this turret does not need ammo.*/
    protected AmmoType[] ammoTypes;
    protected ObjectMap<Item, AmmoType> ammoMap = new ObjectMap<>();

    public ItemTurret(String name) {
        super(name);
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source) {
        TurretEntity entity = tile.entity();

        AmmoType type = ammoMap.get(item);
        entity.totalAmmo += type.quantityMultiplier;

        //find ammo entry by type
        for(int i = 0; i < entity.ammo.size; i ++){
            AmmoEntry entry = entity.ammo.get(i);

            //if found, put it to the right
            if(entry.type == type){
                entry.amount += type.quantityMultiplier;
                entity.ammo.swap(i, entity.ammo.size-1);
                return;
            }
        }

        //must not be found
        AmmoEntry entry = new AmmoEntry(type, (int)type.quantityMultiplier);
        entity.ammo.add(entry);
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        TurretEntity entity = tile.entity();

        return ammoMap != null && ammoMap.get(item) != null && entity.totalAmmo + ammoMap.get(item).quantityMultiplier <= maxammo;
    }

    @Override
    public void setBars(){
        bars.add(new BlockBar(BarType.inventory, true, tile -> (float)tile.<TurretEntity>entity().totalAmmo / maxammo));
    }

    @Override
    public void init(){
        super.init();

        if(ammoTypes != null) {
            for (AmmoType type : ammoTypes) {
                if(type.item == null) continue;
                if (ammoMap.containsKey(type.item)) {
                    throw new RuntimeException("Turret \"" + name + "\" has two conflicting ammo entries on item type " + type.item + "!");
                } else {
                    ammoMap.put(type.item, type);
                }
            }
        }
    }
}
