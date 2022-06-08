package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Weapon implements Cloneable{
    /** displayed weapon region */
    public String name;
    /** bullet shot */
    public BulletType bullet = Bullets.placeholder;
    /** shell ejection effect */
    public Effect ejectEffect = Fx.none;
    /** whether to consume ammo when ammo is enabled in rules */
    public boolean useAmmo = true;
    /** whether to create a flipped copy of this weapon upon initialization. default: true */
    public boolean mirror = true;
    /** whether to flip the weapon's sprite when rendering */
    public boolean flipSprite = false;
    /** whether to shoot the weapons in different arms one after another, rather than all at once; only valid when mirror = true */
    public boolean alternate = true;
    /** whether to rotate toward the target independently of unit */
    public boolean rotate = false;
    /** rotation at which this weapon is locked to if rotate = false. TODO buggy!*/
    public float baseRotation = 0f;
    /** whether to draw the outline on top. */
    public boolean top = true;
    /** whether to hold the bullet in place while firing */
    public boolean continuous;
    /** whether this weapon can be aimed manually by players */
    public boolean controllable = true;
    /** whether to automatically target relevant units in update(); only works when controllable = false. */
    public boolean autoTarget = false;
    /** whether to perform target trajectory prediction */
    public boolean predictTarget = true;
    /** ticks to wait in-between targets */
    public float targetInterval = 40f, targetSwitchInterval = 70f;
    /** rotation speed of weapon when rotation is enabled, in degrees/t*/
    public float rotateSpeed = 20f;
    /** weapon reload in frames */
    public float reload = 1;
    /** inaccuracy of degrees of each shot */
    public float inaccuracy = 0f;
    /** intensity and duration of each shot's screen shake */
    public float shake = 0f;
    /** visual weapon knockback. */
    public float recoil = 1.5f;
    /** time taken for weapon to return to starting position in ticks. uses reload time by default */
    public float recoilTime = -1f;
    /** power curve applied to visual recoil */
    public float recoilPow = 1.8f;
    /** ticks to cool down the heat region */
    public float cooldownTime = 20f;
    /** projectile/effect offsets from center of weapon */
    public float shootX = 0f, shootY = 3f;
    /** offsets of weapon position on unit */
    public float x = 5f, y = 0f;
    /** Random spread on the X axis. */
    public float xRand = 0f;
    /** pattern used for bullets */
    public ShootPattern shoot = new ShootPattern();
    /** radius of shadow drawn under the weapon; <0 to disable */
    public float shadow = -1f;
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** The half-radius of the cone in which shooting will start. */
    public float shootCone = 5f;
    /** Cone in which the weapon can rotate relative to its mount. */
    public float rotationLimit = 361f;
    /** minimum weapon warmup before firing (this is not linear, do NOT use 1!) */
    public float minWarmup = 0f;
    /** lerp speed for shoot warmup, only used for parts */
    public float shootWarmupSpeed = 0.1f, smoothReloadSpeed = 0.15f;
    /** random sound pitch range */
    public float soundPitchMin = 0.8f, soundPitchMax = 1f;
    /** whether shooter rotation is ignored when shooting. */
    public boolean ignoreRotation = false;
    /** If true, this weapon cannot be used to attack targets. */
    public boolean noAttack = false;
    /** min velocity required for this weapon to shoot */
    public float minShootVelocity = -1f;
    /** should the shoot effects follow the unit (effects need followParent set to true for this to work) */
    public boolean parentizeEffects;
    /** internal value used for alternation - do not change! */
    public int otherSide = -1;
    /** draw Z offset relative to the default value */
    public float layerOffset = 0f;
    /** sound used for shooting */
    public Sound shootSound = Sounds.pew;
    /** sound used for weapons that have a delay */
    public Sound chargeSound = Sounds.none;
    /** sound played when there is nothing to shoot */
    public Sound noAmmoSound = Sounds.noammo;
    /** displayed region (autoloaded) */
    public TextureRegion region;
    /** heat region, must be same size as region (optional) */
    public TextureRegion heatRegion;
    /** cell region, must be same size as region (optional) */
    public TextureRegion cellRegion;
    /** outline region to display if top is false */
    public TextureRegion outlineRegion;
    /** heat region tint */
    public Color heatColor = Pal.turretHeat;
    /** status effect applied when shooting */
    public StatusEffect shootStatus = StatusEffects.none;
    /** type of weapon mount to be used */
    public Func<Weapon, WeaponMount> mountType = WeaponMount::new;
    /** status effect duration when shot */
    public float shootStatusDuration = 60f * 5f;
    /** whether this weapon should fire when its owner dies */
    public boolean shootOnDeath = false;
    /** extra animated parts */
    public Seq<DrawPart> parts = new Seq<>(DrawPart.class);

    public Weapon(String name){
        this.name = name;
    }

    public Weapon(){
        this("");
    }

    public boolean hasStats(UnitType u){
        return true;
    }

    public void addStats(UnitType u, Table t){
        if(inaccuracy > 0){
            t.row();
            t.add("[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)inaccuracy + " " + StatUnit.degrees.localized());
        }
        if(reload > 0){
            t.row();
            t.add("[lightgray]" + Stat.reload.localized() + ": " + (mirror ? "2x " : "") + "[white]" + Strings.autoFixed(60f / reload * shoot.shots, 2) + " " + StatUnit.perSecond.localized());
        }

        StatValues.ammo(ObjectMap.of(u, bullet)).display(t);
    }

    public float dps(){
        return (bullet.estimateDPS() / reload) * shoot.shots * 60f;
    }

    //TODO copy-pasted code
    public void drawOutline(Unit unit, WeaponMount mount){
        if(!outlineRegion.found()) return;

        float
        rotation = unit.rotation - 90,
        realRecoil = Mathf.pow(mount.recoil, recoilPow) * recoil,
        weaponRotation  = rotation + (rotate ? mount.rotation : 0),
        wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, -realRecoil),
        wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, -realRecoil);

        Draw.xscl = -Mathf.sign(flipSprite);
        Draw.rect(outlineRegion, wx, wy, weaponRotation);
        Draw.xscl = 1f;
    }

    public void draw(Unit unit, WeaponMount mount){
        if(!region.found()) return;

        //apply layer offset, roll it back at the end
        float z = Draw.z();
        Draw.z(z + layerOffset);

        float
        rotation = unit.rotation - 90,
        realRecoil = Mathf.pow(mount.recoil, recoilPow) * recoil,
        weaponRotation  = rotation + (rotate ? mount.rotation : baseRotation),
        wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, -realRecoil),
        wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, -realRecoil);

        if(shadow > 0){
            Drawf.shadow(wx, wy, shadow);
        }

        if(top){
            drawOutline(unit, mount);
        }

        if(parts.size > 0){
            DrawPart.params.set(mount.warmup, mount.reload / reload, mount.smoothReload, mount.heat, mount.recoil, wx, wy, weaponRotation + 90);
            DrawPart.params.sideMultiplier = flipSprite ? -1 : 1;

            for(int i = 0; i < parts.size; i++){
                var part = parts.get(i);
                if(part.under){
                    part.draw(DrawPart.params);
                }
            }
        }

        Draw.xscl = -Mathf.sign(flipSprite);

        Draw.rect(region, wx, wy, weaponRotation);

        if(cellRegion.found()){
            Draw.color(unit.type.cellColor(unit));
            Draw.rect(cellRegion, wx, wy, weaponRotation);
            Draw.color();
        }

        if(heatRegion.found() && mount.heat > 0){
            Draw.color(heatColor, mount.heat);
            Draw.blend(Blending.additive);
            Draw.rect(heatRegion, wx, wy, weaponRotation);
            Draw.blend();
            Draw.color();
        }

        if(parts.size > 0){
            //TODO does it need an outline?
            for(int i = 0; i < parts.size; i++){
                var part = parts.get(i);
                if(!part.under){
                    part.draw(DrawPart.params);
                }
            }
        }

        Draw.xscl = 1f;

        Draw.z(z);
    }

    public float range(){
        return bullet.range;
    }

    public void update(Unit unit, WeaponMount mount){
        boolean can = unit.canShoot();
        float lastReload = mount.reload;
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
        mount.recoil = Mathf.approachDelta(mount.recoil, 0, unit.reloadMultiplier / recoilTime);
        mount.warmup = Mathf.lerpDelta(mount.warmup, (can && mount.shoot) || (continuous && mount.bullet != null) ? 1f : 0f, shootWarmupSpeed);
        mount.smoothReload = Mathf.lerpDelta(mount.smoothReload, mount.reload / reload, smoothReloadSpeed);

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

        //update continuous state
        if(continuous && mount.bullet != null){
            if(!mount.bullet.isAdded() || mount.bullet.time >= mount.bullet.lifetime || mount.bullet.type != bullet){
                mount.bullet = null;
            }else{
                mount.bullet.rotation(weaponRotation + 90);
                mount.bullet.set(bulletX, bulletY);
                mount.reload = reload;
                mount.recoil = 1f;
                unit.vel.add(Tmp.v1.trns(unit.rotation + 180f, mount.bullet.type.recoil));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }

                //TODO: how do continuous flame bullets work here? a new system is needed
                if(mount.bullet.type.optimalLifeFract > 0){
                //    mount.bullet.time = mount.bullet.lifetime * mount.bullet.type.optimalLifeFract * mount.warmup;
                //    mount.bullet.keepAlive = true;
                }
            }
        }else{
            //heat decreases when not firing
            mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / cooldownTime, 0);

            if(mount.sound != null){
                mount.sound.update(bulletX, bulletY, false);
            }
        }

        //flip weapon shoot side for alternating weapons
        boolean wasFlipped = mount.side;
        if(otherSide != -1 && alternate && mount.side == flipSprite && mount.reload <= reload / 2f && lastReload > reload / 2f){
            unit.mounts[otherSide].side = !unit.mounts[otherSide].side;
            mount.side = !mount.side;
        }

        //shoot if applicable
        if(mount.shoot && //must be shooting
        can && //must be able to shoot
        (!useAmmo || unit.ammo > 0 || !state.rules.unitAmmo || unit.team.rules().infiniteAmmo) && //check ammo
        (!alternate || wasFlipped == flipSprite) &&
        mount.warmup >= minWarmup && //must be warmed up
        unit.vel.len() >= minShootVelocity && //check velocity requirements
        mount.reload <= 0.0001f && //reload has to be 0
        Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, shootCone) //has to be within the cone
        ){
            shoot(unit, mount, bulletX, bulletY, shootAngle);

            mount.reload = reload;

            if(useAmmo){
                unit.ammo--;
                if(unit.ammo < 0) unit.ammo = 0;
            }
        }
    }

    protected Teamc findTarget(Unit unit, float x, float y, float range, boolean air, boolean ground){
        return Units.closestTarget(unit.team, x, y, range + Math.abs(shootY), u -> u.checkTarget(air, ground), t -> ground);
    }

    protected boolean checkTarget(Unit unit, Teamc target, float x, float y, float range){
        return Units.invalidateTarget(target, unit.team, x, y, range + Math.abs(shootY));
    }

    protected float bulletRotation(Unit unit, WeaponMount mount, float bulletX, float bulletY){
        return rotate ? unit.rotation + mount.rotation : Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY) + (unit.rotation - unit.angleTo(mount.aimX, mount.aimY)) + baseRotation;
    }

    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float rotation){
        unit.apply(shootStatus, shootStatusDuration);

        if(shoot.firstShotDelay > 0){
            chargeSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));
            bullet.chargeEffect.at(shootX, shootY, rotation, bullet.keepVelocity || parentizeEffects ? unit : null);
        }

        shoot.shoot(mount.totalShots, (xOffset, yOffset, angle, delay, mover) -> {
            if(delay > 0f){
                Time.run(delay, () -> bullet(unit, mount, xOffset, yOffset, angle, mover));
            }else{
                bullet(unit, mount, xOffset, yOffset, angle, mover);
            }
            mount.totalShots ++;
        });
    }

    protected void bullet(Unit unit, WeaponMount mount, float xOffset, float yOffset, float angleOffset, Mover mover){
        if(!unit.isAdded()) return;

        float
        xSpread = Mathf.range(xRand),
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX + xOffset + xSpread, this.shootY + yOffset),
        shootAngle = bulletRotation(unit, mount, bulletX, bulletY) + angleOffset,
        lifeScl = bullet.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY) / bullet.range) : 1f,
        angle = angleOffset + shootAngle + Mathf.range(inaccuracy);

        mount.bullet = bullet.create(unit, unit.team, bulletX, bulletY, angle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, mount.aimX, mount.aimY);
        handleBullet(unit, mount, mount.bullet);

        if(!continuous){
            shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));
        }

        ejectEffect.at(mountX, mountY, angle * Mathf.sign(this.x));
        bullet.shootEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);
        bullet.smokeEffect.at(bulletX, bulletY, angle, bullet.hitColor, unit);

        unit.vel.add(Tmp.v1.trns(shootAngle + 180f, bullet.recoil));
        Effect.shake(shake, shake, bulletX, bulletY);
        mount.recoil = 1f;
        mount.heat = 1f;
    }

    //override to do special things to a bullet after spawning
    protected void handleBullet(Unit unit, WeaponMount mount, Bullet bullet){

    }

    public void flip(){
        x *= -1;
        shootX *= -1;
        baseRotation *= -1f;
        flipSprite = !flipSprite;
        shoot = shoot.copy();
        shoot.flip();
    }

    public Weapon copy(){
        try{
            return (Weapon)clone();
        }catch(CloneNotSupportedException suck){
            throw new RuntimeException("very good language design", suck);
        }
    }

    @CallSuper
    public void init(){

    }

    public void load(){
        region = Core.atlas.find(name);
        heatRegion = Core.atlas.find(name + "-heat");
        cellRegion = Core.atlas.find(name + "-cell");
        outlineRegion = Core.atlas.find(name + "-outline");

        for(var part : parts){
            part.turretShading = false;
            part.load(name);
        }
    }

    @Override
    public String toString(){
        return name == null || name.isEmpty() ? "Weapon" : "Weapon: " + name;
    }

}
