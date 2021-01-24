package mindustry.entities.bullet;

import arc.audio.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public abstract class BulletType extends Content{
    public float lifetime = 40f;
    public float speed;
    public float damage;
    public float hitSize = 4;
    public float drawSize = 40f;
    public float drag = 0f;
    public boolean pierce, pierceBuilding;
    public int pierceCap = -1;
    public Effect hitEffect, despawnEffect;

    /** Effect created when shooting. */
    public Effect shootEffect = Fx.shootSmall;
    /** Extra smoke effect created when shooting. */
    public Effect smokeEffect = Fx.shootSmallSmoke;
    /** Sound made when hitting something or getting removed.*/
    public Sound hitSound = Sounds.none;
    /** Pitch of the sound made when hitting something*/
    public float hitSoundPitch = 1;
    /** Volume of the sound made when hitting something*/
    public float hitSoundVolume = 1;
    /** Extra inaccuracy when firing. */
    public float inaccuracy = 0f;
    /** How many bullets get created per ammo item/liquid. */
    public float ammoMultiplier = 2f;
    /** Multiplied by turret reload speed to get final shoot speed. */
    public float reloadMultiplier = 1f;
    /** Multiplier of how much base damage is done to tiles. */
    public float buildingDamageMultiplier = 1f;
    /** Recoil from shooter entities. */
    public float recoil;
    /** Whether to kill the shooter when this is shot. For suicide bombers. */
    public boolean killShooter;
    /** Whether to instantly make the bullet disappear. */
    public boolean instantDisappear;
    /** Damage dealt in splash. 0 to disable.*/
    public float splashDamage = 0f;
    /** Knockback in velocity. */
    public float knockback;
    /** Status effect applied on hit. */
    public StatusEffect status = StatusEffects.none;
    /** Intensity of applied status effect in terms of duration. */
    public float statusDuration = 60 * 8f;
    /** Whether this bullet type collides with tiles. */
    public boolean collidesTiles = true;
    /** Whether this bullet type collides with tiles that are of the same team. */
    public boolean collidesTeam = false;
    /** Whether this bullet type collides with air/ground units. */
    public boolean collidesAir = true, collidesGround = true;
    /** Whether this bullet types collides with anything at all. */
    public boolean collides = true;
    /** Whether velocity is inherited from the shooter. */
    public boolean keepVelocity = true;
    /** Whether to scale velocity to disappear at the target position. Used for artillery. */
    public boolean scaleVelocity;
    /** Whether this bullet can be hit by point defense. */
    public boolean hittable = true;
    /** Whether this bullet can be reflected. */
    public boolean reflectable = true;
    /** Whether this projectile can be absorbed by shields. */
    public boolean absorbable = true;
    /** Whether to move the bullet back depending on delta to fix some delta-time realted issues.
     * Do not change unless you know what you're doing. */
    public boolean backMove = true;
    /** Bullet range override. */
    public float maxRange = -1f;
    /** % of block health healed **/
    public float healPercent = 0f;
    /** whether to make fire on impact */
    public boolean makeFire = false;

    //additional effects

    public float fragCone = 360f;
    public float fragAngle = 0f;
    public int fragBullets = 9;
    public float fragVelocityMin = 0.2f, fragVelocityMax = 1f, fragLifeMin = 1f, fragLifeMax = 1f;
    public @Nullable BulletType fragBullet = null;
    public Color hitColor = Color.white;

    public Color trailColor = Pal.missileYellowBack;
    public float trailChance = -0.0001f;
    public Effect trailEffect = Fx.missileTrail;
    public float trailParam =  2f;

    /** Use a negative value to disable splash damage. */
    public float splashDamageRadius = -1f;

    public int incendAmount = 0;
    public float incendSpread = 8f;
    public float incendChance = 1f;
    public float homingPower = 0f;
    public float homingRange = 50f;
    /** Use a negative value to disable homing delay. */
    public float homingDelay = -1f;

    public Color lightningColor = Pal.surge;
    public int lightning;
    public int lightningLength = 5, lightningLengthRand = 0;
    /** Use a negative value to use default bullet damage. */
    public float lightningDamage = -1;
    public float lightningCone = 360f;
    public float lightningAngle = 0f;
    /** The bullet created at lightning points. */
    public @Nullable BulletType lightningType = null;

    public float weaveScale = 1f;
    public float weaveMag = -1f;
    public float hitShake = 0f, despawnShake = 0f;

    public int puddles;
    public float puddleRange;
    public float puddleAmount = 5f;
    public Liquid puddleLiquid = Liquids.water;

    public float lightRadius = 16f;
    public float lightOpacity = 0.3f;
    public Color lightColor = Pal.powerLight;

    public BulletType(float speed, float damage){
        this.speed = speed;
        this.damage = damage;
        hitEffect = Fx.hitBulletSmall;
        despawnEffect = Fx.hitBulletSmall;
    }

    public BulletType(){
        this(1f, 1f);
    }

    /** @return estimated damage per shot. this can be very inaccurate. */
    public float estimateDPS(){
        float sum = damage + splashDamage*0.75f;
        if(fragBullet != null && fragBullet != this){
            sum += fragBullet.estimateDPS() * fragBullets / 2f;
        }
        return sum;
    }

    /** Returns maximum distance the bullet this bullet type has can travel. */
    public float range(){
        return Math.max(speed * lifetime * (1f - drag), maxRange);
    }

    public boolean testCollision(Bullet bullet, Building tile){
        return healPercent <= 0.001f || tile.team != bullet.team || tile.healthf() < 1f;
    }

    /** If direct is false, this is an indirect hit and the tile was already damaged.
     * TODO this is a mess. */
    public void hitTile(Bullet b, Building build, float initialHealth, boolean direct){
        if(makeFire && build.team != b.team){
            Fires.create(build.tile);
        }

        if(healPercent > 0f && build.team == b.team && !(build.block instanceof ConstructBlock)){
            Fx.healBlockFull.at(build.x, build.y, build.block.size, Pal.heal);
            build.heal(healPercent / 100f * build.maxHealth());
        }else if(build.team != b.team && direct){
            hit(b);
        }
    }

    public void hitEntity(Bullet b, Hitboxc other, float initialHealth){

    }

    public void hit(Bullet b){
        hit(b, b.x, b.y);
    }

    public void hit(Bullet b, float x, float y){
        b.hit = true;
        hitEffect.at(x, y, b.rotation(), hitColor);
        hitSound.at(x, y, hitSoundPitch, hitSoundVolume);

        Effect.shake(hitShake, hitShake, b);

        if(fragBullet != null){
            for(int i = 0; i < fragBullets; i++){
                float len = Mathf.random(1f, 7f);
                float a = b.rotation() + Mathf.range(fragCone/2) + fragAngle;
                fragBullet.create(b, x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a, Mathf.random(fragVelocityMin, fragVelocityMax), Mathf.random(fragLifeMin, fragLifeMax));
            }
        }

        if(puddleLiquid != null && puddles > 0){
            for(int i = 0; i < puddles; i++){
                Tile tile = world.tileWorld(x + Mathf.range(puddleRange), y + Mathf.range(puddleRange));
                Puddles.deposit(tile, puddleLiquid, puddleAmount);
            }
        }

        if(Mathf.chance(incendChance)){
            Damage.createIncend(x, y, incendSpread, incendAmount);
        }

        if(splashDamageRadius > 0 && !b.absorbed){
            Damage.damage(b.team, x, y, splashDamageRadius, splashDamage * b.damageMultiplier(), collidesAir, collidesGround);

            if(status != StatusEffects.none){
                Damage.status(b.team, x, y, splashDamageRadius, status, statusDuration, collidesAir, collidesGround);
            }

            if(healPercent > 0f){
                indexer.eachBlock(b.team, x, y, splashDamageRadius, Building::damaged, other -> {
                    Fx.healBlockFull.at(other.x, other.y, other.block.size, Pal.heal);
                    other.heal(healPercent / 100f * other.maxHealth());
                });
            }

            if(makeFire){
                indexer.eachBlock(null, x, y, splashDamageRadius, other -> other.team != b.team, other -> {
                    Fires.create(other.tile);
                });
            }
        }

        for(int i = 0; i < lightning; i++){
            Lightning.create(b, lightningColor, lightningDamage < 0 ? damage : lightningDamage, b.x, b.y, b.rotation() + Mathf.range(lightningCone/2) + lightningAngle, lightningLength + Mathf.random(lightningLengthRand));
        }
    }

    public void despawned(Bullet b){
        despawnEffect.at(b.x, b.y, b.rotation(), hitColor);
        hitSound.at(b);

        Effect.shake(despawnShake, despawnShake, b);

        if(!b.hit && (fragBullet != null || splashDamageRadius > 0 || lightning > 0)){
            hit(b);
        }
    }

    public void draw(Bullet b){
    }

    public void drawLight(Bullet b){
        Drawf.light(b.team, b, lightRadius, lightColor, lightOpacity);
    }

    public void init(Bullet b){

        if(killShooter && b.owner() instanceof Healthc h){
            h.kill();
        }

        if(instantDisappear){
            b.time = lifetime;
        }
    }

    public void update(Bullet b){
        if(homingPower > 0.0001f && b.time >= homingDelay){
            Teamc target = Units.closestTarget(b.team, b.x, b.y, homingRange, e -> (e.isGrounded() && collidesGround) || (e.isFlying() && collidesAir), t -> collidesGround);
            if(target != null){
                b.vel.setAngle(Mathf.slerpDelta(b.rotation(), b.angleTo(target), homingPower));
            }
        }

        if(weaveMag > 0){
            b.vel.rotate(Mathf.sin(b.time + Mathf.PI * weaveScale/2f, weaveScale, weaveMag * (Mathf.randomSeed(b.id, 0, 1) == 1 ? -1 : 1)) * Time.delta);
        }

        if(trailChance > 0){
            if(Mathf.chanceDelta(trailChance)){
                trailEffect.at(b.x, b.y, trailParam, trailColor);
            }
        }
    }

    @Override
    public void init(){
        if(pierceCap >= 1){
            pierce = true;
            //pierceBuilding is not enabled by default, because a bullet may want to *not* pierce buildings
        }

        if(lightningType == null){
            lightningType = !collidesAir ? Bullets.damageLightningGround : Bullets.damageLightning;
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.bullet;
    }

    public Bullet create(Teamc owner, float x, float y, float angle){
        return create(owner, owner.team(), x, y, angle);
    }

    public Bullet create(Entityc owner, Team team, float x, float y, float angle){
        return create(owner, team, x, y, angle, 1f);
    }

    public Bullet create(Entityc owner, Team team, float x, float y, float angle, float velocityScl){
        return create(owner, team, x, y, angle, -1, velocityScl, 1f, null);
    }

    public Bullet create(Entityc owner, Team team, float x, float y, float angle, float velocityScl, float lifetimeScl){
        return create(owner, team, x, y, angle, -1, velocityScl, lifetimeScl, null);
    }

    public Bullet create(Bullet parent, float x, float y, float angle){
        return create(parent.owner, parent.team, x, y, angle);
    }

    public Bullet create(Bullet parent, float x, float y, float angle, float velocityScl, float lifeScale){
        return create(parent.owner, parent.team, x, y, angle, velocityScl, lifeScale);
    }

    public Bullet create(Bullet parent, float x, float y, float angle, float velocityScl){
        return create(parent.owner(), parent.team, x, y, angle, velocityScl);
    }

    public Bullet create(@Nullable Entityc owner, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl, Object data){
        Bullet bullet = Bullet.create();
        bullet.type = this;
        bullet.owner = owner;
        bullet.team = team;
        bullet.time = 0f;
        bullet.vel.trns(angle, speed * velocityScl);
        if(backMove){
            bullet.set(x - bullet.vel.x * Time.delta, y - bullet.vel.y * Time.delta);
        }else{
            bullet.set(x, y);
        }
        bullet.lifetime = lifetime * lifetimeScl;
        bullet.data = data;
        bullet.drag = drag;
        bullet.hitSize = hitSize;
        bullet.damage = (damage < 0 ? this.damage : damage) * bullet.damageMultiplier();
        bullet.add();

        if(keepVelocity && owner instanceof Velc v) bullet.vel.add(v.vel().x, v.vel().y);
        return bullet;
    }

    public void createNet(Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl){
        Call.createBullet(this, team, x, y, angle, damage, velocityScl, lifetimeScl);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void createBullet(BulletType type, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl){
        if(type == null) return;
        type.create(null, team, x, y, angle, damage, velocityScl, lifetimeScl, null);
    }
}
