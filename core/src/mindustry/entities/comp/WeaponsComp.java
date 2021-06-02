package mindustry.entities.comp;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

@Component
abstract class WeaponsComp implements Teamc, Posc, Rotc, Velc, Statusc{
    @Import float x, y;
    @Import boolean disarmed;
    @Import UnitType type;

    /** weapon mount array, never null */
    @SyncLocal WeaponMount[] mounts = {};
    @ReadOnly transient boolean isRotate;
    transient float aimX, aimY;
    boolean isShooting;
    float ammo;

    float ammof(){
        return ammo / type.ammoCapacity;
    }

    void setWeaponRotation(float rotation){
        for(WeaponMount mount : mounts){
            mount.rotation = rotation;
        }
    }

    void setupWeapons(UnitType def){
        mounts = new WeaponMount[def.weapons.size];
        for(int i = 0; i < mounts.length; i++){
            mounts[i] = def.weapons.get(i).mountType.get(def.weapons.get(i));
        }
    }

    void controlWeapons(boolean rotateShoot){
        controlWeapons(rotateShoot, rotateShoot);
    }

    void controlWeapons(boolean rotate, boolean shoot){
        for(WeaponMount mount : mounts){
            if(mount.weapon.controllable){
                mount.rotate = rotate;
                mount.shoot = shoot;
            }
        }
        isRotate = rotate;
        isShooting = shoot;
    }

    void aim(Position pos){
        aim(pos.getX(), pos.getY());
    }

    /** Aim at something. This will make all mounts point at it. */
    void aim(float x, float y){
        Tmp.v1.set(x, y).sub(this.x, this.y);
        if(Tmp.v1.len() < type.aimDst) Tmp.v1.setLength(type.aimDst);

        x = Tmp.v1.x + this.x;
        y = Tmp.v1.y + this.y;

        for(WeaponMount mount : mounts){
            if(mount.weapon.controllable){
                mount.aimX = x;
                mount.aimY = y;
            }
        }

        aimX = x;
        aimY = y;
    }

    boolean canShoot(){
        return !disarmed;
    }

    @Override
    public void remove(){
        for(WeaponMount mount : mounts){
            if(mount.bullet != null && mount.bullet.owner == self()){
                mount.bullet.time = mount.bullet.lifetime - 10f;
                mount.bullet = null;
            }

            if(mount.sound != null){
                mount.sound.stop();
            }
        }
    }

    /** Update shooting and rotation for this unit. */
    @Override
    public void update(){
        for(WeaponMount mount : mounts){
            mount.weapon.update(self(), mount);
        }
    }
}
