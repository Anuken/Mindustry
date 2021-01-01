package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.audio.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

@Component
abstract class WeaponsComp implements Teamc, Posc, Rotc, Velc, Statusc{
    @Import float x, y, rotation, reloadMultiplier;
    @Import Vec2 vel;
    @Import UnitType type;

    /** temporary weapon sequence number */
    static int sequenceNum = 0;

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
            mounts[i] = new WeaponMount(def.weapons.get(i));
        }
    }

    void controlWeapons(boolean rotateShoot){
        controlWeapons(rotateShoot, rotateShoot);
    }

    void controlWeapons(boolean rotate, boolean shoot){
        for(WeaponMount mount : mounts){
            mount.rotate = rotate;
            mount.shoot = shoot;
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
            mount.aimX = x;
            mount.aimY = y;
        }

        aimX = x;
        aimY = y;
    }

    boolean canShoot(){
        return true;
    }

    @Override
    public void remove(){
        for(WeaponMount mount : mounts){
            if(mount.bullet != null){
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
        boolean can = canShoot();

        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;
            mount.reload = Math.max(mount.reload - Time.delta * reloadMultiplier, 0);

            float weaponRotation = this.rotation - 90 + (weapon.rotate ? mount.rotation : 0);
            float mountX = this.x + Angles.trnsx(this.rotation - 90, weapon.x, weapon.y),
                mountY = this.y + Angles.trnsy(this.rotation - 90, weapon.x, weapon.y);
            float shootX = mountX + Angles.trnsx(weaponRotation, weapon.shootX, weapon.shootY),
                shootY = mountY + Angles.trnsy(weaponRotation, weapon.shootX, weapon.shootY);
            float shootAngle = weapon.rotate ? weaponRotation + 90 : Angles.angle(shootX, shootY, mount.aimX, mount.aimY) + (this.rotation - angleTo(mount.aimX, mount.aimY));

            //update continuous state
            if(weapon.continuous && mount.bullet != null){
                if(!mount.bullet.isAdded() || mount.bullet.time >= mount.bullet.lifetime || mount.bullet.type != weapon.bullet){
                    mount.bullet = null;
                }else{
                    mount.bullet.rotation(weaponRotation + 90);
                    mount.bullet.set(shootX, shootY);
                    mount.reload = weapon.reload;
                    vel.add(Tmp.v1.trns(rotation + 180f, mount.bullet.type.recoil));
                    if(weapon.shootSound != Sounds.none && !headless){
                        if(mount.sound == null) mount.sound = new SoundLoop(weapon.shootSound, 1f);
                        mount.sound.update(x, y, true);
                    }
                }
            }else{
                //heat decreases when not firing
                mount.heat = Math.max(mount.heat - Time.delta * reloadMultiplier / mount.weapon.cooldownTime, 0);

                if(mount.sound != null){
                    mount.sound.update(x, y, false);
                }
            }

            //flip weapon shoot side for alternating weapons at half reload
            if(weapon.otherSide != -1 && weapon.alternate && mount.side == weapon.flipSprite &&
                mount.reload + Time.delta * reloadMultiplier > weapon.reload/2f && mount.reload <= weapon.reload/2f){
                mounts[weapon.otherSide].side = !mounts[weapon.otherSide].side;
                mount.side = !mount.side;
            }

            //rotate if applicable
            if(weapon.rotate && (mount.rotate || mount.shoot) && can){
                float axisX = this.x + Angles.trnsx(this.rotation - 90,  weapon.x, weapon.y),
                    axisY = this.y + Angles.trnsy(this.rotation - 90,  weapon.x, weapon.y);

                mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - this.rotation;
                mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, weapon.rotateSpeed * Time.delta);
            }else if(!weapon.rotate){
                mount.rotation = 0;
                mount.targetRotation = angleTo(mount.aimX, mount.aimY);
            }

            //shoot if applicable
            if(mount.shoot && //must be shooting
                can && //must be able to shoot
                (ammo > 0 || !state.rules.unitAmmo || team().rules().infiniteAmmo) && //check ammo
                (!weapon.alternate || mount.side == weapon.flipSprite) &&
                //TODO checking for velocity this way isn't entirely correct
                (vel.len() >= mount.weapon.minShootVelocity || (net.active() && !isLocal())) && //check velocity requirements
                mount.reload <= 0.0001f && //reload has to be 0
                Angles.within(weapon.rotate ? mount.rotation : this.rotation, mount.targetRotation, mount.weapon.shootCone) //has to be within the cone
            ){
                shoot(mount, shootX, shootY, mount.aimX, mount.aimY, mountX, mountY, shootAngle, Mathf.sign(weapon.x));

                mount.reload = weapon.reload;

                ammo--;
                if(ammo < 0) ammo = 0;
            }
        }
    }

    private void shoot(WeaponMount mount, float x, float y, float aimX, float aimY, float mountX, float mountY, float rotation, int side){
        Weapon weapon = mount.weapon;

        float baseX = this.x, baseY = this.y;
        boolean delay = weapon.firstShotDelay + weapon.shotDelay > 0f;

        (delay ? weapon.chargeSound : weapon.continuous ? Sounds.none : weapon.shootSound).at(x, y, Mathf.random(weapon.soundPitchMin, weapon.soundPitchMax));

        BulletType ammo = weapon.bullet;
        float lifeScl = ammo.scaleVelocity ? Mathf.clamp(Mathf.dst(x, y, aimX, aimY) / ammo.range()) : 1f;

        sequenceNum = 0;
        if(delay){
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                Time.run(sequenceNum * weapon.shotDelay + weapon.firstShotDelay, () -> {
                    if(!isAdded()) return;
                    mount.bullet = bullet(weapon, x + this.x - baseX, y + this.y - baseY, f + Mathf.range(weapon.inaccuracy), lifeScl);
                });
                sequenceNum++;
            });
        }else{
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> mount.bullet = bullet(weapon, x, y, f + Mathf.range(weapon.inaccuracy), lifeScl));
        }

        boolean parentize = ammo.keepVelocity;

        if(delay){
            Time.run(weapon.firstShotDelay, () -> {
                if(!isAdded()) return;

                vel.add(Tmp.v1.trns(rotation + 180f, ammo.recoil));
                Effect.shake(weapon.shake, weapon.shake, x, y);
                mount.heat = 1f;
                if(!weapon.continuous){
                    weapon.shootSound.at(x, y, Mathf.random(weapon.soundPitchMin, weapon.soundPitchMax));
                }
            });
        }else{
            vel.add(Tmp.v1.trns(rotation + 180f, ammo.recoil));
            Effect.shake(weapon.shake, weapon.shake, x, y);
            mount.heat = 1f;
        }

        weapon.ejectEffect.at(mountX, mountY, rotation * side);
        ammo.shootEffect.at(x, y, rotation, parentize ? this : null);
        ammo.smokeEffect.at(x, y, rotation, parentize ? this : null);
        apply(weapon.shootStatus, weapon.shootStatusDuration);
    }

    private Bullet bullet(Weapon weapon, float x, float y, float angle, float lifescl){
        float xr = Mathf.range(weapon.xRand);

        return weapon.bullet.create(this, team(),
        x + Angles.trnsx(angle, 0, xr),
        y + Angles.trnsy(angle, 0, xr),
        angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd), lifescl);
    }
}
