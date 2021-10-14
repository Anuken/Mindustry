package mindustry.type.ammo;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class PowerAmmoType implements AmmoType{
    public float range = 85f;
    public float totalPower = 1000;

    public PowerAmmoType(float totalPower){
        this.totalPower = totalPower;
    }

    public PowerAmmoType(){

    }

    @Override
    public String icon(){
        return Iconc.power + "";
    }

    @Override
    public Color color(){
        return Pal.powerLight;
    }

    @Override
    public Color barColor(){
        return Pal.powerLight;
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
