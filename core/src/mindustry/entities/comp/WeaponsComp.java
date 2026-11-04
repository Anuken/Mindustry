package mindustry.entities.comp;

import arc.math.*;
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
    @NoSerialize float aimX, aimY;
    boolean isShooting;

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
        aim(x, y, false);
    }

    /** Aim at something. This will make all mounts point at it. */
    void aim(float x, float y, boolean clearTarget){
        Tmp.v1.set(x, y).sub(this.x, this.y);
        if(Tmp.v1.len() < type.aimDst) Tmp.v1.setLength(type.aimDst);

        x = Tmp.v1.x + this.x;
        y = Tmp.v1.y + this.y;

        for(WeaponMount mount : mounts){
            if(mount.weapon.controllable){
                if(clearTarget) mount.target = null;
                mount.aimX = x;
                mount.aimY = y;
            }
        }

        aimX = x;
        aimY = y;
    }

    public void switchWeaponSet(int index){
        if(type.weaponSets.size == 0 || index < 0 || index >= type.weaponSets.size) return;

        for(WeaponMount mount : mounts){
            if(mount.weaponSet == index) continue;
            mount.targetWeaponSet = index;
            mount.switching = true;
            mount.switchProgress = 0f;
        }
    }

    boolean canShoot(){
        return !disarmed;
    }

    @Override
    public void remove(){
        for(WeaponMount mount : mounts){
            if(mount.weapon.continuous && mount.bullet != null && mount.bullet.owner == self()){
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
        //allow weapon sets to switch automatically
        if(type.weaponSets.size > 0 && mounts.length > 0){
            Teamc target = mounts[0].target;
            if(target != null){
                float dst = dst(target);
                for(int s = 0; s < type.weaponSets.size; s++){
                    var set = type.weaponSets.get(s);
                    if(set.size > 0){
                        var w = set.first();
                        if((w.minSwitchRange < 0 || dst >= w.minSwitchRange) && (w.maxSwitchRange < 0 || dst <= w.maxSwitchRange) && mounts[0].weaponSet != s && !mounts[0].switching){
                            switchWeaponSet(s);
                            break;
                        }
                    }
                }
            }
        }

        for(WeaponMount mount : mounts){
            if(mount.switching){
                mount.switchProgress = Mathf.approachDelta(mount.switchProgress, 1f, 0.05f);
                if(mount.switchProgress >= 1f){
                    mount.switching = false;
                    mount.weaponSet = mount.targetWeaponSet;
                }
            }
            mount.weapon.update(self(), mount);
        }
    }
}
