package mindustry.type.weapons;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/** A weapon that visually displays the unit's building beam. */
public class BuildWeapon extends BaseWeapon{
    public BuildWeapon(String name){
        super(name);
    }

    public BuildWeapon(){
        super();
    }

    {
        rotate = true;
        display = false;
    }

    @Override
    public void update(Unit unit, BaseWeaponMount mount){
        mount.shoot = false;
        mount.rotate = true;

        //always aim at build plan
        if(unit.activelyBuilding()){
            mount.aimX = unit.buildPlan().drawx();
            mount.aimY = unit.buildPlan().drawy();
            mount.shoot = Angles.within(mount.rotation, mount.targetRotation, shootCone);
            if(mount.shoot) mount.recoil = mount.heat = 1f;
        }else{
            //aim for baseRotation
            float weaponRotation = unit.rotation - 90 + baseRotation;
            mount.aimX = unit.x + Angles.trnsx(unit.rotation - 90, x, y) + Angles.trnsx(weaponRotation, shootX, shootY);
            mount.aimY = unit.y + Angles.trnsy(unit.rotation - 90, x, y) + Angles.trnsy(weaponRotation, shootX, shootY);
        }

        super.update(unit, mount);
    }

    @Override
    public void drawWeapon(Unit unit, BaseWeaponMount mount, float wx, float wy, float weaponRotation){
        super.drawWeapon(unit, mount, wx, wy, weaponRotation);

        if(unit.activelyBuilding() && mount.shoot){
            float z = Draw.z(),
                px = wx + Angles.trnsx(weaponRotation, shootX, shootY),
                py = wy + Angles.trnsy(weaponRotation, shootX, shootY);
            unit.drawBuildingBeam(px, py);
            Draw.z(z);
        }
    }
}
