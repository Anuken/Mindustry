package mindustry.type;

import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public class AmmoTypes implements ContentList{
    public static AmmoType
    powerLow,
    power,
    powerHigh,
    copper,
    graphite,
    coal,
    thorium;

    @Override
    public void load(){
        powerLow = new PowerAmmoType(500);
        power = new PowerAmmoType(1000);
        powerHigh = new PowerAmmoType(2000);
        copper = new ItemAmmoType(Items.copper);
        graphite = new ItemAmmoType(Items.graphite);
        coal = new ItemAmmoType(Items.coal);
        thorium = new ItemAmmoType(Items.thorium);
    }

    public static class PowerAmmoType extends AmmoType{
        public float totalPower = 1000;

        public PowerAmmoType(){
            super(Iconc.power, Pal.powerLight);
            barColor = color;
        }

        public PowerAmmoType(float totalPower){
            this();
            this.totalPower = totalPower;
        }

        @Override
        public void resupply(Unit unit){
            float range = unit.hitSize + this.range;

            Building build = Units.closestBuilding(unit.team, unit.x, unit.y, range, u -> u.block.consumes.hasPower() && u.block.consumes.getPower().buffered);

            if(build != null){
                float amount = build.power.status * build.block.consumes.getPower().capacity;
                float powerPerAmmo = totalPower / unit.type.ammoCapacity;
                float ammoRequired = unit.type.ammoCapacity - unit.ammo;
                float powerRequired = ammoRequired * powerPerAmmo;
                float powerTaken = Math.min(amount, powerRequired);

                if(powerTaken > 1){
                    build.power.status -= powerTaken / build.block.consumes.getPower().capacity;
                    unit.ammo += powerTaken / powerPerAmmo;

                    Fx.itemTransfer.at(build.x, build.y, Math.max(powerTaken / 100f, 1f), Pal.power, unit);
                }
            }
        }
    }

    public static class ItemAmmoType extends AmmoType{
        public int ammoPerItem = 15;
        public Item item;

        public ItemAmmoType(Item item){
            this.item = item;
            this.color = item.color;
        }

        public ItemAmmoType(){
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

        @Override
        public void load(){
            if(item != null){
                icon = item.emoji();
            }
        }
    }
}
