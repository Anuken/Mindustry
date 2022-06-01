package mindustry.type.weapons;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.audio.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

public class ContinuousWeapon extends Weapon{
    public ContinuousWeapon(String name){
        super(name);
        continuous = true;
    }

    public ContinuousWeapon(){
        this("");
    }

    public void update(Unit unit, WeaponMount mount){
        boolean can = unit.canShoot();
        mount.recoil = Mathf.approachDelta(mount.recoil, 0, unit.reloadMultiplier / recoilTime);
        mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / cooldownTime, 0);

        //rotate if applicable
        if(rotate && (mount.rotate || mount.shoot) && can){
            float axisX = unit.x + Angles.trnsx(unit.rotation - 90,  x, y),
                axisY = unit.y + Angles.trnsy(unit.rotation - 90,  x, y);

            mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);
            if(rotationLimit < 360){
                float dst = Angles.angleDist(mount.rotation, 0f);
                if(dst > rotationLimit/2f){
                    mount.rotation = Angles.moveToward(mount.rotation, 0, dst - rotationLimit/2f);
                }
            }
        }else if(!rotate){
            mount.rotation = baseRotation;
            mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
        }

        float
            weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
            mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
            mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
            bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
            bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
            shootAngle = bulletRotation(unit, mount, bulletX, bulletY);

        //find a new target
        if(!controllable && autoTarget){
            if((mount.retarget -= Time.delta) <= 0f){
                mount.target = findTarget(unit, mountX, mountY, bullet.range, bullet.collidesAir, bullet.collidesGround);
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }

            if(mount.target != null && checkTarget(unit, mount.target, mountX, mountY, bullet.range)){
                mount.target = null;
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, bullet.range + Math.abs(shootY) + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && can;

                if(predictTarget){
                    Vec2 to = Predict.intercept(unit, mount.target, bullet.speed);
                    mount.aimX = to.x;
                    mount.aimY = to.y;
                }else{
                    mount.aimX = mount.target.x();
                    mount.aimY = mount.target.y();
                }
            }

            mount.shoot = mount.rotate = shoot;

            //note that shooting state is not affected, as these cannot be controlled
            //logic will return shooting as false even if these return true, which is fine
        }

        //can the weapon shoot?
        boolean shoot = mount.shoot && //must be shooting
            can && //must be able to shoot
            (!useAmmo || unit.ammo > 0 || !state.rules.unitAmmo || unit.team.rules().infiniteAmmo) && //check ammo
            unit.vel.len() >= minShootVelocity && //check velocity requirements
            Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, shootCone); //has to be within the cone

        mount.warmup = Mathf.lerpDelta(mount.warmup, shoot ? 1f : 0f, shootWarmupSpeed);

        //update continuous state
        if(mount.bullet != null){
            if(!mount.bullet.isAdded() || mount.bullet.type != bullet){
                mount.bullet = null;
            }else{
                mount.bullet.rotation(weaponRotation + 90);
                mount.bullet.set(bulletX, bulletY);
                mount.recoil = 1f;
                unit.vel.add(Tmp.v1.trns(unit.rotation + 180f, mount.bullet.type.recoil));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }
                if(shoot){
                    mount.bullet.time = mount.bullet.lifetime * mount.bullet.type.optimalLifeFract * mount.warmup;
                    mount.bullet.keepAlive = true;
                }
            }
        }

        //create a new bullet if applicable
        if(shoot && mount.bullet == null){
            shoot(unit, mount, bulletX, bulletY, shootAngle);
            if(useAmmo){
                unit.ammo--;
                if(unit.ammo < 0) unit.ammo = 0;
            }
        }
    }
}
