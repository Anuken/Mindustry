package mindustry.type.weapons;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

/** A weapon that visually displays the unit's mining beam. */
public class MineWeaponR extends BaseWeapon{
    public MineWeaponR(String name){
        super(name);
    }

    public MineWeaponR(){
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
        if(unit.mining()){
            mount.aimX = unit.mineTile().drawx();
            mount.aimY = unit.mineTile().drawy();
            mount.shoot = Angles.within(mount.rotation, mount.targetRotation, shootCone);
            if(mount.shoot) mount.recoil = 1f;
        }else{
            //aim for baseRotation
            float weaponRotation = unit.rotation - 90 + baseRotation;
            mount.aimX = unit.x + Angles.trnsx(unit.rotation - 90, x, y) + Angles.trnsx(weaponRotation, this.shootX, this.shootY);
            mount.aimY = unit.y + Angles.trnsy(unit.rotation - 90, x, y) + Angles.trnsy(weaponRotation, this.shootX, this.shootY);
        }

        super.update(unit, mount);
    }

    @Override
    public void drawWeapon(Unit unit, BaseWeaponMount mount, float wx, float wy, float weaponRotation){
        super.drawWeapon(unit, mount, wx, wy, weaponRotation);

        if(unit.mining() && mount.shoot){
            float z = Draw.z(),
                sY = shootY + Mathf.absin(Time.time, 1.1f, 0.5f),
                px = wx + Angles.trnsx(weaponRotation, shootX, sY),
                py = wy + Angles.trnsy(weaponRotation, shootX, sY);

            unit.type.drawMiningBeam(unit, px, py);
            Draw.z(z);
        }
    }
}
