package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;

import static mindustry.Vars.*;

@Component
abstract class WeaponsComp implements Teamc, Posc, Rotc, Velc{
    @Import float x, y, rotation, reloadMultiplier;
    @Import Vec2 vel;
    @Import UnitType type;

    /** minimum cursor distance from unit, fixes 'cross-eyed' shooting */
    static final float minAimDst = 18f;
    /** temporary weapon sequence number */
    static int sequenceNum = 0;

    /** weapon mount array, never null */
    @SyncLocal WeaponMount[] mounts = {};
    @ReadOnly transient float aimX, aimY;
    @ReadOnly transient boolean isRotate;
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
        if(Tmp.v1.len() < minAimDst) Tmp.v1.setLength(minAimDst);

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

    /** Update shooting and rotation for this unit. */
    @Override
    public void update(){
        boolean can = canShoot();

        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;
            mount.reload = Math.max(mount.reload - Time.delta * reloadMultiplier, 0);
            mount.heat = Math.max(mount.heat - Time.delta * reloadMultiplier / mount.weapon.cooldownTime, 0);

            //flip weapon shoot side for alternating weapons at half reload
            if(weapon.otherSide != -1 && weapon.alternate && mount.side == weapon.flipSprite &&
                mount.reload + Time.delta > weapon.reload/2f && mount.reload <= weapon.reload/2f){
                mounts[weapon.otherSide].side = !mounts[weapon.otherSide].side;
                mount.side = !mount.side;
            }

            //rotate if applicable
            if(weapon.rotate && (mount.rotate || mount.shoot) && can){
                float axisX = this.x + Angles.trnsx(rotation - 90,  weapon.x, weapon.y),
                    axisY = this.y + Angles.trnsy(rotation - 90,  weapon.x, weapon.y);

                mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - rotation;
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

                float rotation = this.rotation - 90;
                float weaponRotation = rotation + (weapon.rotate ? mount.rotation : 0);


                float mountX = this.x + Angles.trnsx(rotation, weapon.x, weapon.y),
                    mountY = this.y + Angles.trnsy(rotation, weapon.x, weapon.y);
                float shootX = mountX + Angles.trnsx(weaponRotation, weapon.shootX, weapon.shootY),
                    shootY = mountY + Angles.trnsy(weaponRotation, weapon.shootX, weapon.shootY);
                float shootAngle = weapon.rotate ? weaponRotation + 90 : Angles.angle(shootX, shootY, mount.aimX, mount.aimY) + (this.rotation - angleTo(mount.aimX, mount.aimY));

                shoot(weapon, shootX, shootY, mount.aimX, mount.aimY, shootAngle, Mathf.sign(weapon.x));

                mount.reload = weapon.reload;
                mount.heat = 1f;

                ammo--;
                if(ammo < 0) ammo = 0;
            }
        }
    }

    private void shoot(Weapon weapon, float x, float y, float aimX, float aimY, float rotation, int side){

        float baseX = this.x, baseY = this.y;

        weapon.shootSound.at(x, y, Mathf.random(0.8f, 1.0f));

        BulletType ammo = weapon.bullet;
        float lifeScl = ammo.scaleVelocity ? Mathf.clamp(Mathf.dst(x, y, aimX, aimY) / ammo.range()) : 1f;

        sequenceNum = 0;
        if(weapon.shotDelay > 0.01f){
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                Time.run(sequenceNum * weapon.shotDelay, () -> bullet(weapon, x + this.x - baseX, y + this.y - baseY, f + Mathf.range(weapon.inaccuracy), lifeScl));
                sequenceNum++;
            });
        }else{
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> bullet(weapon, x, y, f + Mathf.range(weapon.inaccuracy), lifeScl));
        }

        vel().add(Tmp.v1.trns(rotation + 180f, ammo.recoil));

        boolean parentize = ammo.keepVelocity;

        Effect.shake(weapon.shake, weapon.shake, x, y);
        weapon.ejectEffect.at(x, y, rotation * side);
        ammo.shootEffect.at(x, y, rotation, parentize ? this : null);
        ammo.smokeEffect.at(x, y, rotation, parentize ? this : null);
    }

    private void bullet(Weapon weapon, float x, float y, float angle, float lifescl){
        float xr = Mathf.range(weapon.xRand);

        weapon.bullet.create(this, team(),
        x + Angles.trnsx(angle, 0, xr),
        y + Angles.trnsy(angle, 0, xr),
        angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd), lifescl);
    }
}
