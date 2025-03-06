package mindustry.type.weapons;

import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.audio.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

import static mindustry.Vars.headless;

public class ContinuousWeapon extends Weapon{
    /** whether this weapon uses continuous fire without reloading */
    public boolean alwaysContinuous;
    /** Speed at which the turret can change its bullet "aim" distance. This is only used for point laser bullets. */
    public float aimChangeSpeed = Float.POSITIVE_INFINITY;

    public ContinuousWeapon(String name){
        super(name);
    }

    public ContinuousWeapon(){
        super();
    }

    public void addStats(UnitType u, Table t){
        if(inaccuracy > 0){
            t.row();
            t.add("[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)inaccuracy + " " + StatUnit.degrees.localized());
        }
        if(!alwaysContinuous && reload > 0 && !bullet.killShooter){
            t.row();
            t.add("[lightgray]" + Stat.reload.localized() + ": " + (mirror ? "2x " : "") + "[white]" + Strings.autoFixed(60f / reload * shoot.shots, 2) + " " + StatUnit.perSecond.localized());
        }

        StatValues.ammo(ObjectMap.of(u, bullet)).display(t);
    }

    @Override
    public void update(Unit unit, BaseWeaponMount m){
        super.update(unit, m);
    }



    public void updateContinuous(Unit unit, WeaponMount mount){
        float
            mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
            mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
            weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
            bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
            bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
            shootAngle = bulletRotation(unit, mount, bulletX, bulletY);

        Bullet mBullet = mount.bullet;
        if(mBullet != null){
            if(!mBullet.isAdded() || mBullet.time >= mBullet.lifetime || mBullet.type != bullet){
                mount.bullet = null;
            }else{
                mBullet.rotation(weaponRotation + 90);
                mBullet.set(bulletX, bulletY);
                mount.reload = reload;
                mount.recoil = 1f;
                unit.vel.add(Tmp.v1.trns(mBullet.rotation() + 180f, mBullet.type.recoil * Time.delta));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }

                //target length of laser
                float shootLength = Math.min(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY), range());
                //current length of laser
                float curLength = Mathf.dst(bulletX, bulletY, mBullet.aimX, mBullet.aimY);
                //resulting length of the bullet (smoothed)
                float resultLength = Mathf.approachDelta(curLength, shootLength, aimChangeSpeed);
                //actual aim end point based on length
                Tmp.v1.trns(shootAngle, mount.lastLength = resultLength).add(bulletX, bulletY);

                mBullet.aimX = Tmp.v1.x;
                mBullet.aimY = Tmp.v1.y;

                if(alwaysContinuous && mount.shoot){
                    mBullet.time = mBullet.lifetime * mBullet.type.optimalLifeFract * mount.warmup;
                    mBullet.keepAlive = true;

                    unit.apply(shootStatus, shootStatusDuration);
                }
            }
        }else{
            updateCooldown(unit, mount);

            if(mount.sound != null){
                mount.sound.update(bulletX, bulletY, false);
            }
        }
    }
}
