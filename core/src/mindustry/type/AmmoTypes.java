package mindustry.type;

import arc.util.ArcAnnotate.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class AmmoTypes implements ContentList{
    public static AmmoType
    powerLow,
    power,
    powerHigh,
    copper,
    thorium;

    @Override
    public void load(){
        powerLow = new PowerAmmoType(500);
        power = new PowerAmmoType(1000);
        powerHigh = new PowerAmmoType(2000);
        copper = new ItemAmmoType(Items.copper);
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
            float range = unit.hitSize + 60f;
            Tile closest = Vars.indexer.findClosestFlag(unit.x, unit.y, unit.team, BlockFlag.powerResupply);

            if(closest != null && closest.build != null && unit.within(closest.build, range) && closest.build.power != null){
                var build = closest.build;

                if(build.block.consumes.hasPower() && build.block.consumes.getPower().buffered){
                    float amount = closest.build.power.status * build.block.consumes.getPower().capacity;
                    float powerPerAmmo = totalPower / unit.type().ammoCapacity;
                    float ammoRequired = unit.type().ammoCapacity - unit.ammo;
                    float powerRequired = ammoRequired * powerPerAmmo;
                    float powerTaken = Math.min(amount, powerRequired);

                    if(powerTaken > 1){
                        closest.build.power.status -= powerTaken / build.block.consumes.getPower().capacity;
                        unit.ammo += powerTaken / powerPerAmmo;

                        Fx.itemTransfer.at(build.x, build.y, Math.max(powerTaken / 100f, 1f), Pal.power, unit);
                    }
                }
            }
        }
    }

    public static class ItemAmmoType extends AmmoType{
        public @NonNull Item item;

        public ItemAmmoType(Item item){
            this.item = item;
            this.color = item.color;
        }

        public ItemAmmoType(){
        }

        @Override
        public void load(){
            if(item != null){
                icon = item.emoji();
            }
        }
    }
}
