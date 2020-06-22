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
abstract class WeaponsComp implements Teamc, Posc, Rotc{
    @Import float x, y, rotation;

    /** minimum cursor distance from unit, fixes 'cross-eyed' shooting */
    static final float minAimDst = 20f;
    /** temporary weapon sequence number */
    static int sequenceNum = 0;

    /** weapon mount array, never null */
    @ReadOnly transient WeaponMount[] mounts = {};
    @ReadOnly transient float range, aimX, aimY;
    @ReadOnly transient boolean isRotate;
    boolean isShooting;
    int ammo;

    void setWeaponRotation(float rotation){
        for(WeaponMount mount : mounts){
            mount.rotation = rotation;
        }
    }

    boolean inRange(Position other){
        return within(other, range);
    }

    void setupWeapons(UnitType def){
        mounts = new WeaponMount[def.weapons.size];
        range = def.range;
        for(int i = 0; i < mounts.length; i++){
            mounts[i] = new WeaponMount(def.weapons.get(i));
            range = Math.max(range, def.weapons.get(i).bullet.range());
        }
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

    /** Update shooting and rotation for this unit. */
    @Override
    public void update(){
        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;
            mount.reload = Math.max(mount.reload - Time.delta(), 0);

            //rotate if applicable
            if(weapon.rotate && (mount.rotate || mount.shoot)){
                float axisXOffset = weapon.mirror ? 0f : weapon.x;
                float axisX = this.x + Angles.trnsx(rotation, axisXOffset, weapon.y),
                axisY = this.y + Angles.trnsy(rotation, axisXOffset, weapon.y);

                mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - rotation();
                mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, weapon.rotateSpeed * Time.delta());
            }else{
                mount.rotation = 0;
                mount.targetRotation = angleTo(mount.aimX, mount.aimY);
            }

            if(mount.shoot && (ammo > 0 || !state.rules.unitAmmo || team().rules().infiniteAmmo)){
                float rotation = this.rotation - 90;

                //shoot if applicable
                if(mount.reload <= 0.0001f && Angles.within(weapon.rotate ? mount.rotation : this.rotation, mount.targetRotation, mount.weapon.shootCone)){
                    for(int i : (weapon.mirror && !weapon.alternate ? Mathf.signs : Mathf.one)){
                        i *= Mathf.sign(weapon.flipped) * (mount.weapon.mirror ? Mathf.sign(mount.side) : 1);

                        //m a t h
                        float weaponRotation = rotation + (weapon.rotate ? mount.rotation : 0);
                        float mountX = this.x + Angles.trnsx(rotation, weapon.x * i, weapon.y),
                            mountY = this.y + Angles.trnsy(rotation, weapon.x * i, weapon.y);
                        float shootX = mountX + Angles.trnsx(weaponRotation, weapon.shootX * i, weapon.shootY),
                            shootY = mountY + Angles.trnsy(weaponRotation, weapon.shootX * i, weapon.shootY);
                        float shootAngle = weapon.rotate ? weaponRotation + 90 : Angles.angle(shootX, shootY, mount.aimX, mount.aimY) + (this.rotation - angleTo(mount.aimX, mount.aimY));

                        shoot(weapon, shootX, shootY, mount.aimX, mount.aimY, shootAngle, -i);
                    }

                    if(mount.weapon.mirror) mount.side = !mount.side;
                    mount.reload = weapon.reload;

                    ammo --;
                    if(ammo < 0) ammo = 0;
                }
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

        Tmp.v1.trns(rotation + 180f, ammo.recoil);

        if(this instanceof Velc){
            //TODO apply force?
            ((Velc)this).vel().add(Tmp.v1);
        }

        Tmp.v1.trns(rotation, 3f);
        boolean parentize = ammo.keepVelocity;

        Effects.shake(weapon.shake, weapon.shake, x, y);
        weapon.ejectEffect.at(x, y, rotation * side);
        ammo.shootEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? this : null);
        ammo.smokeEffect.at(x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? this : null);
    }

    private void bullet(Weapon weapon, float x, float y, float angle, float lifescl){
        Tmp.v1.trns(angle, 3f);
        weapon.bullet.create(this, team(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd), lifescl);
    }
}
