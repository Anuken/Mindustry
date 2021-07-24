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
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Weapon implements Cloneable{
    /** temporary weapon sequence number */
    static int sequenceNum = 0;
    
    /** displayed weapon region */
    public String name = "";
    /** bullet shot */
    public BulletType bullet = Bullets.standardCopper;
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
    public float reload;
    /** amount of shots per fire */
    public int shots = 1;
    /** spacing in degrees between multiple shots, if applicable */
    public float spacing = 0;
    /** inaccuracy of degrees of each shot */
    public float inaccuracy = 0f;
    /** intensity and duration of each shot's screen shake */
    public float shake = 0f;
    /** visual weapon knockback. */
    public float recoil = 1.5f;
    /** projectile/effect offsets from center of weapon */
    public float shootX = 0f, shootY = 3f;
    /** offsets of weapon position on unit */
    public float x = 5f, y = 0f;
    /** random spread on the X axis */
    public float xRand = 0f;
    /** radius of shadow drawn under the weapon; <0 to disable */
    public float shadow = -1f;
    /** fraction of velocity that is random */
    public float velocityRnd = 0f;
    /** delay in ticks between shots */
    public float firstShotDelay = 0;
    /** delay in ticks between shots */
    public float shotDelay = 0;
    /** The half-radius of the cone in which shooting will start. */
    public float shootCone = 5f;
    /** ticks to cool down the heat region */
    public float cooldownTime = 20f;
    /** random sound pitch range */
    public float soundPitchMin = 0.8f, soundPitchMax = 1f;
    /** whether shooter rotation is ignored when shooting. */
    public boolean ignoreRotation = false;
    /** min velocity required for this weapon to shoot */
    public float minShootVelocity = -1f;
    /** internal value used for alternation - do not change! */
    public int otherSide = -1;
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

    public Weapon(String name){
        this.name = name;
    }

    public Weapon(){
        this("");
    }

    public void addStats(UnitType u, Table t){
        if(inaccuracy > 0){
            t.row();
            t.add("[lightgray]" + Stat.inaccuracy.localized() + ": [white]" + (int)inaccuracy + " " + StatUnit.degrees.localized());
        }
        t.row();
        t.add("[lightgray]" + Stat.reload.localized() + ": " + (mirror ? "2x " : "") + "[white]" + Strings.autoFixed(60f / reload * shots, 2) + " " + StatUnit.perSecond.localized());

        StatValues.ammo(ObjectMap.of(u, bullet)).display(t);
    }

    public float dps(){
        return (bullet.estimateDPS() / reload) * shots * 60f;
    }

    //TODO copy-pasted code
    public void drawOutline(Unit unit, WeaponMount mount){
        float
        rotation = unit.rotation - 90,
        weaponRotation  = rotation + (rotate ? mount.rotation : 0),
        recoil = -((mount.reload) / reload * this.recoil),
        wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, recoil),
        wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, recoil);

        if(outlineRegion.found()){
            Draw.rect(outlineRegion,
            wx, wy,
            outlineRegion.width * Draw.scl * -Mathf.sign(flipSprite),
            region.height * Draw.scl,
            weaponRotation);
        }
    }
    
    public void draw(Unit unit, WeaponMount mount){
        float
        rotation = unit.rotation - 90,
        weaponRotation  = rotation + (rotate ? mount.rotation : 0),
        recoil = -((mount.reload) / reload * this.recoil),
        wx = unit.x + Angles.trnsx(rotation, x, y) + Angles.trnsx(weaponRotation, 0, recoil),
        wy = unit.y + Angles.trnsy(rotation, x, y) + Angles.trnsy(weaponRotation, 0, recoil);

        if(shadow > 0){
            Drawf.shadow(wx, wy, shadow);
        }

        if(outlineRegion.found() && top){
            Draw.rect(outlineRegion,
            wx, wy,
            outlineRegion.width * Draw.scl * -Mathf.sign(flipSprite),
            region.height * Draw.scl,
            weaponRotation);
        }

        Draw.rect(region,
        wx, wy,
        region.width * Draw.scl * -Mathf.sign(flipSprite),
        region.height * Draw.scl,
        weaponRotation);

        if(heatRegion.found() && mount.heat > 0){
            Draw.color(heatColor, mount.heat);
            Draw.blend(Blending.additive);
            Draw.rect(heatRegion,
            wx, wy,
            heatRegion.width * Draw.scl * -Mathf.sign(flipSprite),
            heatRegion.height * Draw.scl,
            weaponRotation);
            Draw.blend();
            Draw.color();
        }
    }

    public void update(Unit unit, WeaponMount mount){
        boolean can = unit.canShoot();
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);

        float
        weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : 0),
        mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y),
        mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y),
        bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY),
        bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY),
        shootAngle = rotate ? weaponRotation + 90 : Angles.angle(bulletX, bulletY, mount.aimX, mount.aimY) + (unit.rotation - unit.angleTo(mount.aimX, mount.aimY));

        //find a new target
        if(!controllable && autoTarget){
            if((mount.retarget -= Time.delta) <= 0f){
                mount.target = findTarget(unit, mountX, mountY, bullet.range(), bullet.collidesAir, bullet.collidesGround);
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }

            if(mount.target != null && checkTarget(unit, mount.target, mountX, mountY, bullet.range())){
                mount.target = null;
            }

            boolean shoot = false;

            if(mount.target != null){
                shoot = mount.target.within(mountX, mountY, bullet.range() + Math.abs(shootY) + (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && can;

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
                unit.vel.add(Tmp.v1.trns(unit.rotation + 180f, mount.bullet.type.recoil));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }
            }
        }else{
            //heat decreases when not firing
            mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / mount.weapon.cooldownTime, 0);

            if(mount.sound != null){
                mount.sound.update(bulletX, bulletY, false);
            }
        }

        //flip weapon shoot side for alternating weapons at half reload
        if(otherSide != -1 && alternate && mount.side == flipSprite &&
        mount.reload + Time.delta * unit.reloadMultiplier > reload/2f && mount.reload <= reload/2f){
            unit.mounts[otherSide].side = !unit.mounts[otherSide].side;
            mount.side = !mount.side;
        }

        //rotate if applicable
        if(rotate && (mount.rotate || mount.shoot) && can){
            float axisX = unit.x + Angles.trnsx(unit.rotation - 90,  x, y),
            axisY = unit.y + Angles.trnsy(unit.rotation - 90,  x, y);

            mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);
        }else if(!rotate){
            mount.rotation = 0;
            mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
        }

        //shoot if applicable
        if(mount.shoot && //must be shooting
        can && //must be able to shoot
        (!useAmmo || unit.ammo > 0 || !state.rules.unitAmmo || unit.team.rules().infiniteAmmo) && //check ammo
        (!alternate || mount.side == flipSprite) &&
        unit.vel.len() >= mount.weapon.minShootVelocity && //check velocity requirements
        mount.reload <= 0.0001f && //reload has to be 0
        Angles.within(rotate ? mount.rotation : unit.rotation, mount.targetRotation, mount.weapon.shootCone) //has to be within the cone
        ){
            shoot(unit, mount, bulletX, bulletY, mount.aimX, mount.aimY, mountX, mountY, shootAngle, Mathf.sign(x));

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

    protected void shoot(Unit unit, WeaponMount mount, float shootX, float shootY, float aimX, float aimY, float mountX, float mountY, float rotation, int side){
        float baseX = unit.x, baseY = unit.y;
        boolean delay = firstShotDelay + shotDelay > 0f;

        (delay ? chargeSound : continuous ? Sounds.none : shootSound).at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));

        BulletType ammo = bullet;
        float lifeScl = ammo.scaleVelocity ? Mathf.clamp(Mathf.dst(shootX, shootY, aimX, aimY) / ammo.range()) : 1f;

        sequenceNum = 0;
        if(delay){
            Angles.shotgun(shots, spacing, rotation, f -> {
                Time.run(sequenceNum * shotDelay + firstShotDelay, () -> {
                    if(!unit.isAdded()) return;
                    mount.bullet = bullet(unit, shootX + unit.x - baseX, shootY + unit.y - baseY, f + Mathf.range(inaccuracy), lifeScl);
                    if(!continuous){
                        shootSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));
                    }
                });
                sequenceNum++;
            });
        }else{
            Angles.shotgun(shots, spacing, rotation, f -> mount.bullet = bullet(unit, shootX, shootY, f + Mathf.range(inaccuracy), lifeScl));
        }

        boolean parentize = ammo.keepVelocity;

        if(delay){
            Time.run(firstShotDelay, () -> {
                if(!unit.isAdded()) return;

                unit.vel.add(Tmp.v1.trns(rotation + 180f, ammo.recoil));
                Effect.shake(shake, shake, shootX, shootY);
                mount.heat = 1f;
                if(!continuous){
                    shootSound.at(shootX, shootY, Mathf.random(soundPitchMin, soundPitchMax));
                }
            });
        }else{
            unit.vel.add(Tmp.v1.trns(rotation + 180f, ammo.recoil));
            Effect.shake(shake, shake, shootX, shootY);
            mount.heat = 1f;
        }

        ejectEffect.at(mountX, mountY, rotation * side);
        ammo.shootEffect.at(shootX, shootY, rotation, parentize ? unit : null);
        ammo.smokeEffect.at(shootX, shootY, rotation, parentize ? unit : null);
        unit.apply(shootStatus, shootStatusDuration);
    }

    protected Bullet bullet(Unit unit, float shootX, float shootY, float angle, float lifescl){
        float xr = Mathf.range(xRand);

        return bullet.create(unit, unit.team,
        shootX + Angles.trnsx(angle, 0, xr),
        shootY + Angles.trnsy(angle, 0, xr),
        angle, (1f - velocityRnd) + Mathf.random(velocityRnd), lifescl);
    }

    public Weapon copy(){
        try{
            return (Weapon)clone();
        }catch(CloneNotSupportedException suck){
            throw new RuntimeException("very good language design", suck);
        }
    }

    public void load(){
        region = Core.atlas.find(name, Core.atlas.find("clear"));
        heatRegion = Core.atlas.find(name + "-heat");
        outlineRegion = Core.atlas.find(name + "-outline");
    }

}
