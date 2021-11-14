package mindustry.type.ammo;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class ItemAmmoType implements AmmoType{
    public float range = 85f;
    public int ammoPerItem = 15;
    public Item item;

    public ItemAmmoType(Item item){
        this.item = item;
    }

    public ItemAmmoType(Item item, int ammoPerItem){
        this.item = item;
        this.ammoPerItem = ammoPerItem;
    }

    public ItemAmmoType(){
    }

    @Override
    public String icon(){
        return item.emoji();
    }

    @Override
    public Color color(){
        return item.color;
    }

    @Override
    public Color barColor(){
        return Pal.ammo;
    }

    @Override
    public void resupply(Unit unit){
        //do not resupply when it would waste resources
        if(unit.type.ammoCapacity - unit.ammo < ammoPerItem) return;

        float range = unit.hitSize + this.range;

        Building build = Units.closestBuilding(unit.team, unit.x, unit.y, range, u -> u.block.allowResupply && u.items.has(item));

        if(build != null){
            Fx.itemTransfer.at(build.x, build.y, ammoPerItem / 2f, item.color, unit);
            unit.ammo = Math.min(unit.ammo + ammoPerItem, unit.type.ammoCapacity);
            build.items.remove(item, 1);
        }
    }
}
