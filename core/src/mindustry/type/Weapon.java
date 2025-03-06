package mindustry.type;

import arc.audio.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.weapons.*;
import mindustry.world.meta.*;

public class Weapon extends ReloadWeapon{
    /** bullet shot */
    public BulletType bullet = Bullets.placeholder;
    /** shell ejection effect */
    public Effect ejectEffect = Fx.none;
    /** whether to consume ammo when ammo is enabled in rules */
    public boolean useAmmo = true;
    /** @deprecated use ContinuousWeapon instead. Kept for json parsing. */
    public boolean continuous;
    /** whether to perform target trajectory prediction */
    public boolean predictTarget = true;
    /** if true, this weapon is used for attack range calculations */
    public boolean useAttackRange = true;
    /** inaccuracy of degrees of each shot */
    public float inaccuracy = 0f;
    /** intensity and duration of each shot's screen shake */
    public float shake = 0f;
    /** Number of additional counters for recoil. */
    public int recoils = -1;
    /** time taken for weapon to return to starting position in ticks. uses reload time by default */
    public float recoilTime = -1f;
    /** Random spread on the X axis. */
    public float xRand = 0f;
    /** pattern used for bullets */
    public ShootPattern shoot = new ShootPattern();
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** random sound pitch range */
    public float soundPitchMin = 0.8f, soundPitchMax = 1f;
    /** If true, this weapon cannot be used to attack targets. */
    public boolean noAttack = false;
    /** should the shoot effects follow the unit (effects need followParent set to true for this to work) */
    public boolean parentizeEffects;
    /** sound used for weapons that have a delay */
    public Sound chargeSound = Sounds.none;
    /** sound played when there is nothing to shoot */
    public Sound noAmmoSound = Sounds.noammo;
    /** status effect applied when shooting */
    public StatusEffect shootStatus = StatusEffects.none;
    /** status effect duration when shot */
    public float shootStatusDuration = 60f * 5f;
    /** whether this weapon should fire when its owner dies */
    public boolean shootOnDeath = false;

    public Weapon(String name){
        super(name);
    }

    public Weapon(){
        super();
    }

    {
        mountType = WeaponMount::new;
    }

    public void addStats(UnitType u, Table t){
        if(inaccuracy > 0){
            t.row();
            t.add("[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)inaccuracy + " " + StatUnit.degrees.localized());
        }

        StatValues.ammo(ObjectMap.of(u, bullet)).display(t);
    }

    public float dps(){
        return (bullet.estimateDPS() / reload) * shoot.shots * 60f;
    }

    public float shotsPerSec(){
        return shoot.shots * 60f / reload;
    }

    @Override
    public void update(Unit unit, BaseWeaponMount m){
        WeaponMount mount = (WeaponMount)m;

        if(recoils > 0){
            if(mount.recoils == null) mount.recoils = new float[recoils];
            for(int i = 0; i < recoils; i++){
                mount.recoils[i] = Mathf.approachDelta(mount.recoils[i], 0, unit.reloadMultiplier / recoilTime);
            }
        }

        super.update(unit, m);
    }

    @Override
    public void updateTargeting(Unit unit, TargetWeaponMount mount){
        if(!controllable && autoTarget){
            float
                mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
                mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);

            if((mount.retarget -= Time.delta) <= 0f){
                mount.target = findTarget(unit, mountX, mountY, range(), targetAir, targetGround);
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }

            if(mount.target != null && checkTarget(unit, mount.target, mountX, mountY, range())){
                mount.target = null;
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, range() + Math.abs(shootY) + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && unit.canShoot();

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
    }

    @Override
    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, range + Math.abs(shootY), u -> u.checkTarget(air, ground), t -> ground && (unit.type.targetUnderBlocks || !t.block.underBullets));
    }

    @Override
    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return Units.invalidateTarget(target, unit.team, x, y, range + Math.abs(shootY));
    }

    protected void shoot(Unit unit, ReloadWeaponMount m, float shootX, float shootY, float rotation){
        WeaponMount mount = (WeaponMount)m;

        unit.apply(shootStatus, shootStatusDuration);

        if(shoot.firstShotDelay > 0){
            mount.charging = true;
            chargeSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));
            bullet.chargeEffect.at(shootX, shootY, rotation, bullet.keepVelocity || parentizeEffects ? unit : null);
        }

        shoot.shoot(mount.barrelCounter, (xOffset, yOffset, angle, delay, mover) -> {
            //this is incremented immediately, as it is used for total bullet creation amount detection
            mount.totalShots ++;
            int barrel = mount.barrelCounter;

            if(delay > 0f){
                Time.run(delay, () -> {
                    //hack: make sure the barrel is the same as what it was when the bullet was queued to fire
                    int prev = mount.barrelCounter;
                    mount.barrelCounter = barrel;
                    bullet(unit, mount, xOffset, yOffset, angle, mover);
                    mount.barrelCounter = prev;
                });
            }else{
                bullet(unit, mount, xOffset, yOffset, angle, mover);
            }
        }, () -> mount.barrelCounter++);
    }

    protected void bullet(Unit unit, WeaponMount mount, float xOffset, float yOffset, float angleOffset, Mover mover){
        if(!unit.isAdded()) return;

        mount.charging = false;
        float
        xSpread = Mathf.range(xRand),
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset),
        shootAngle = bulletRotation(unit, mount, bulletX, bulletY) + angleOffset,
        lifeScl = bullet.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY) / bullet.range) : 1f,
        angle = shootAngle + Mathf.range(inaccuracy + bullet.inaccuracy);

        Entityc shooter = unit.controller() instanceof MissileAI ai ? ai.shooter : unit; //Pass the missile's shooter down to its bullets
        mount.bullet = bullet.create(unit, shooter, unit.team, bulletX, bulletY, angle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, mount.aimX, mount.aimY, mount.target);
        handleBullet(unit, mount, mount.bullet);

        shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));

        ejectEffect.at(mountX, mountY, angle * Mathf.sign(this.x));
        bullet.shootEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
        bullet.smokeEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);

        unit.vel.add(Tmp.v1.trns(shootAngle + 180f, bullet.recoil));
        Effect.shake(shake, shake, bulletX, bulletY);
        mount.recoil = 1f;
        if(recoils > 0){
            mount.recoils[mount.barrelCounter % recoils] = 1f;
        }
        mount.heat = 1f;
    }

    //override to do special things to a bullet after spawning
    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){
    }

    @Override
    public void setPartParams(Unit unit, BaseWeaponMount m, float wx, float wy, float weaponRotation){
        WeaponMount mount = (WeaponMount)m;
        DrawPart.params.set(mount.warmup, mount.reload / reload, mount.smoothReload, mount.heat, mount.recoil, mount.charge, wx, wy, weaponRotation + 90);
        DrawPart.params.sideMultiplier = flipSprite ? -1 : 1;
    }

    @Override
    public void flip(){
        super.flip();
        shoot = shoot.copy();
        shoot.flip();
    }

    @Override
    public void init(){
        range = bullet.range;
        shootEffect = bullet.shootEffect;
        smokeEffect = bullet.smokeEffect;
        targetAir = bullet.collidesAir;
        targetGround = bullet.collidesGround;
    }

}
