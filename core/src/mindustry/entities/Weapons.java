package mindustry.entities;

import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.entities.type.*;
import mindustry.type.*;

public class Weapons{
    private static final int[] one = {1};

    private WeaponMount[] mounts;
    private UnitDef lastDef;

    public void update(Unit unit){
        check(unit);

        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;

        }
    }

    public void draw(Unit unit){
        check(unit);

        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;

            for(int i : (weapon.mirror ? Mathf.signs : one)){
                i *= Mathf.sign(weapon.flipped);

                float rotation = unit.rotation - 90 + (weapon.rotate ? mount.rotation : 0);
                float trY = weapon.length - mount.reload / weapon.reload * weapon.recoil;
                float width = i > 0 ? -weapon.region.getWidth() : weapon.region.getWidth();

                Draw.rect(weapon.region,
                    unit.x + Angles.trnsx(rotation, weapon.width * i, trY),
                    unit.y + Angles.trnsy(rotation, weapon.width * i, trY),
                    width * Draw.scl,
                    weapon.region.getHeight() * Draw.scl,
                    rotation - 90);
            }
        }
    }

    //check mount validity
    private void check(Unit unit){
        if(mounts == null || mounts.length != unit.type().weapons.size || lastDef != unit.type()){
            mounts = new WeaponMount[unit.type().weapons.size];
            for(int i = 0; i < mounts.length; i++){
                mounts[i] = new WeaponMount(unit.type().weapons.get(i));
            }
            lastDef = unit.type();
        }
    }

    private static class WeaponMount{
        /** reload in frames; 0 means ready to fire */
        float reload;
        /** rotation relative to the unit this mount is on */
        float rotation;
        /** weapon associated with this mount */
        Weapon weapon;

        public WeaponMount(Weapon weapon){
            this.weapon = weapon;
        }
    }
}
