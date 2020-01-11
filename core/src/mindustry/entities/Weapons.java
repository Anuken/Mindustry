package mindustry.entities;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.type.*;

public class Weapons{
    /** 1 */
    private static final int[] one = {1};
    /** minimum cursor distance from player, fixes 'cross-eyed' shooting */
    private static final float minAimDst = 20f;
    /** temporary weapon sequence number */
    private static int sequenceNum = 0;

    /** weapon mount array, never null */
    private WeaponMount[] mounts = {};

    public void init(Unit unit){
        mounts = new WeaponMount[unit.type().weapons.size];
        for(int i = 0; i < mounts.length; i++){
            mounts[i] = new WeaponMount(unit.type().weapons.get(i));
        }
    }

    /** Aim at something. This will make all mounts point at it. */
    public void aim(Unit unit, float x, float y){
        Tmp.v1.set(x, y).sub(unit.x, unit.y);
        if(Tmp.v1.len() < minAimDst) Tmp.v1.setLength(minAimDst);

        x = Tmp.v1.x + unit.x;
        y = Tmp.v1.y + unit.y;

        for(WeaponMount mount : mounts){
            mount.aimX = x;
            mount.aimY = y;
        }
    }

    /** Update shooting and rotation for this unit. */
    public void update(Unit unit){
        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;
            mount.reload -= Time.delta();

            float rotation = unit.rotation - 90;

            //rotate if applicable
            if(weapon.rotate){
                float axisXOffset = weapon.mirror ? 0f : weapon.x;
                float axisX = unit.x + Angles.trnsx(rotation, axisXOffset, weapon.y),
                    axisY = unit.y + Angles.trnsy(rotation, axisXOffset, weapon.y);

                mount.rotation = Angles.moveToward(mount.rotation, Angles.angle(axisX, axisY, mount.aimX, mount.aimY), weapon.rotateSpeed);
            }

            //shoot if applicable
            //TODO only shoot if angle is reached, don't shoot inaccurately
            if(mount.reload <= 0){
                for(int i : (weapon.mirror && !weapon.alternate ? Mathf.signs : one)){
                    i *= Mathf.sign(weapon.flipped) * Mathf.sign(mount.side);

                    //m a t h
                    float weaponRotation = rotation + (weapon.rotate ? mount.rotation : 0);
                    float mountX = unit.x + Angles.trnsx(rotation, weapon.x * i, weapon.y),
                        mountY = unit.y + Angles.trnsy(rotation, weapon.x * i, weapon.y);
                    float shootX = mountX + Angles.trnsx(weaponRotation, weapon.shootX * i, weapon.shootY),
                        shootY = mountY + Angles.trnsy(weaponRotation, weapon.shootX * i, weapon.shootY);
                    float shootAngle = weapon.rotate ? weaponRotation : Angles.angle(shootX, shootY, mount.aimX, mount.aimY);

                    shoot(unit, weapon, shootX, shootY, shootAngle);
                }

                mount.side = !mount.side;
                mount.reload = weapon.reload;
            }
        }
    }

    /** Draw weapon mounts. */
    public void draw(Unit unit){
        for(WeaponMount mount : mounts){
            Weapon weapon = mount.weapon;

            for(int i : (weapon.mirror ? Mathf.signs : one)){
                i *= Mathf.sign(weapon.flipped);

                float rotation = unit.rotation - 90 + (weapon.rotate ? mount.rotation : 0);
                float trY = weapon.y - (mount.reload / weapon.reload * weapon.recoil) * (weapon.alternate ? Mathf.num(i == Mathf.sign(mount.side)) : 1);
                float width = i > 0 ? -weapon.region.getWidth() : weapon.region.getWidth();

                Draw.rect(weapon.region,
                    unit.x + Angles.trnsx(rotation, weapon.x * i, trY),
                    unit.y + Angles.trnsy(rotation, weapon.x * i, trY),
                    width * Draw.scl,
                    weapon.region.getHeight() * Draw.scl,
                    rotation - 90);
            }
        }
    }

    private void shoot(ShooterTrait shooter, Weapon weapon, float x, float y, float rotation){
        float baseX = shooter.getX(), baseY = shooter.getY();

        weapon.shootSound.at(x, y, Mathf.random(0.8f, 1.0f));

        sequenceNum = 0;
        if(weapon.shotDelay > 0.01f){
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> {
                Time.run(sequenceNum * weapon.shotDelay, () -> bullet(shooter, weapon, x + shooter.getX() - baseX, y + shooter.getY() - baseY, f + Mathf.range(weapon.inaccuracy)));
                sequenceNum++;
            });
        }else{
            Angles.shotgun(weapon.shots, weapon.spacing, rotation, f -> bullet(shooter, weapon, x, y, f + Mathf.range(weapon.inaccuracy)));
        }

        BulletType ammo = weapon.bullet;

        Tmp.v1.trns(rotation + 180f, ammo.recoil);

        shooter.velocity().add(Tmp.v1);

        Tmp.v1.trns(rotation, 3f);
        boolean parentize = ammo.keepVelocity;

        Effects.shake(weapon.shake, weapon.shake, x, y);
        Effects.effect(weapon.ejectEffect, x, y, rotation);
        Effects.effect(ammo.shootEffect, x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? shooter : null);
        Effects.effect(ammo.smokeEffect, x + Tmp.v1.x, y + Tmp.v1.y, rotation, parentize ? shooter : null);
    }

    private void bullet(ShooterTrait owner, Weapon weapon, float x, float y, float angle){
        Tmp.v1.trns(angle, 3f);
        Bullet.create(weapon.bullet, owner, owner.getTeam(), x + Tmp.v1.x, y + Tmp.v1.y, angle, (1f - weapon.velocityRnd) + Mathf.random(weapon.velocityRnd));
    }

    private static class WeaponMount{
        /** reload in frames; 0 means ready to fire */
        float reload;
        /** rotation relative to the unit this mount is on */
        float rotation;
        /** weapon associated with this mount */
        Weapon weapon;
        /** aiming position in world coordinates */
        float aimX, aimY;
        /** side that's being shot - only valid for mirrors */
        boolean side;

        public WeaponMount(Weapon weapon){
            this.weapon = weapon;
        }
    }
}
