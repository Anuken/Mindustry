package mindustry.entities.bullet;

import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.part.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class BulletType extends Content implements Cloneable{
    static final UnitDamageEvent bulletDamageEvent = new UnitDamageEvent();

    /** Lifetime in ticks. */
    public float lifetime = 40f;
    /** Speed in units/tick. */
    public float speed = 1f;
    /** Direct damage dealt on hit. */
    public float damage = 1f;
    /** Hitbox size. */
    public float hitSize = 4;
    /** Clipping hitbox. */
    public float drawSize = 40f;
    /** Drag as fraction of velocity. */
    public float drag = 0f;
    /** Whether to pierce units. */
    public boolean pierce;
    /** Whether to pierce buildings. */
    public boolean pierceBuilding;
    /** Maximum # of pierced objects. */
    public int pierceCap = -1;
    /** Multiplier of damage decreased per health pierced. */
    public float pierceDamageFactor = 0f;
    /** If false, this bullet isn't removed after pierceCap is exceeded. Expert usage only. */
    public boolean removeAfterPierce = true;
    /** For piercing lasers, setting this to true makes it get absorbed by plastanium walls. */
    public boolean laserAbsorb = true;
    /** Life fraction at which this bullet has the best range/damage/etc. Used for lasers and continuous turrets. */
    public float optimalLifeFract = 0f;
    /** Z layer to drawn on. */
    public float layer = Layer.bullet;
    /** Effect shown on direct hit. */
    public Effect hitEffect = Fx.hitBulletSmall;
    /** Effect shown when bullet despawns. */
    public Effect despawnEffect = Fx.hitBulletSmall;
    /** Effect created when shooting. */
    public Effect shootEffect = Fx.shootSmall;
    /** Effect created when charging starts; only usable in single-shot weapons with a firstShotDelay / shotDelay. */
    public Effect chargeEffect = Fx.none;
    /** Extra smoke effect created when shooting. */
    public Effect smokeEffect = Fx.shootSmallSmoke;
    /** Sound made when hitting something or getting removed.*/
    public Sound hitSound = Sounds.none;
    /** Sound made when hitting something or getting removed.*/
    public Sound despawnSound = Sounds.none;
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
    /** If true, splash damage is "correctly" affected by unit hitbox size. Used for projectiles that do not collide / have splash as their main source of damage. */
    public boolean scaledSplashDamage = false;
    /** Knockback in velocity. */
    public float knockback;
    /** Should knockback follow the bullet's direction */
    public boolean impact;
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
    /** If true, this projectile collides with non-surface floors. */
    public boolean collideFloor = false;
    /** If true, this projectile collides with static walls */
    public boolean collideTerrain = false;
    /** Whether velocity is inherited from the shooter. */
    public boolean keepVelocity = true;
    /** Whether to scale lifetime (not actually velocity!) to disappear at the target position. Used for artillery. */
    public boolean scaleLife;
    /** Whether this bullet can be hit by point defense. */
    public boolean hittable = true;
    /** Whether this bullet can be reflected. */
    public boolean reflectable = true;
    /** Whether this projectile can be absorbed by shields. */
    public boolean absorbable = true;
    /** Whether to move the bullet back depending on delta to fix some delta-time related issues.
     * Do not change unless you know what you're doing. */
    public boolean backMove = true;
    /** Bullet range positive override. */
    public float maxRange = -1f;
    /** When > 0, overrides range even if smaller than base range. */
    public float rangeOverride = -1f;
    /** When used in a turret with multiple ammo types, this can be set to a non-zero value to influence range. */
    public float rangeChange = 0f;
    /** Range initialized in init(). */
    public float range = 0f;
    /** % of block health healed **/
    public float healPercent = 0f;
    /** flat amount of block health healed */
    public float healAmount = 0f;
    /** Whether to make fire on impact */
    public boolean makeFire = false;
    /** Whether to create hit effects on despawn. Forced to true if this bullet has any special effects like splash damage. */
    public boolean despawnHit = false;
    /** If true, this bullet will create bullets when it hits anything, not just when it despawns. */
    public boolean fragOnHit = true;
    /** If false, this bullet will not create fraags when absorbed by a shield. */
    public boolean fragOnAbsorb = true;
    /** If true, unit armor is ignored in damage calculations. */
    public boolean pierceArmor = false;
    /** Whether status and despawnHit should automatically be set. */
    public boolean setDefaults = true;
    /** Amount of shaking produced when this bullet hits something or despawns. */
    public float hitShake = 0f, despawnShake = 0f;

    /** Bullet type that is created when this bullet expires. */
    public @Nullable BulletType fragBullet = null;
    /** Degree spread range of fragmentation bullets. */
    public float fragRandomSpread = 360f;
    /** Uniform spread between each frag bullet in degrees. */
    public float fragSpread = 0f;
    /** Angle offset of fragmentation bullets. */
    public float fragAngle = 0f;
    /** Number of fragmentation bullets created. */
    public int fragBullets = 9;
    /** Random range of frag velocity as a multiplier. */
    public float fragVelocityMin = 0.2f, fragVelocityMax = 1f;
    /** Random range of frag lifetime as a multiplier. */
    public float fragLifeMin = 1f, fragLifeMax = 1f;

    /** Bullet that is created at a fixed interval. */
    public @Nullable BulletType intervalBullet;
    /** Interval, in ticks, between which bullet spawn. */
    public float bulletInterval = 20f;
    /** Number of bullet spawned per interval. */
    public int intervalBullets = 1;
    /** Random spread of interval bullets. */
    public float intervalRandomSpread = 360f;
    /** Angle spread between individual interval bullets. */
    public float intervalSpread = 0f;
    /** Angle offset for interval bullets. */
    public float intervalAngle = 0f;
    /** Use a negative value to disable interval bullet delay. */
    public float intervalDelay = -1f;

    /** Color used for hit/despawn effects. */
    public Color hitColor = Color.white;
    /** Color used for block heal effects. */
    public Color healColor = Pal.heal;
    /** Effect emitted upon blocks that are healed. */
    public Effect healEffect = Fx.healBlockFull;
    /** Bullets spawned when this bullet is created. Rarely necessary, used for visuals. */
    public Seq<BulletType> spawnBullets = new Seq<>();
    /** Unit spawned _instead of_ this bullet. Useful for missiles. */
    public @Nullable UnitType spawnUnit;
    /** Unit spawned when this bullet hits something or despawns due to it hitting the end of its lifetime. */
    public @Nullable UnitType despawnUnit;
    /** Amount of units spawned when this bullet despawns. */
    public int despawnUnitCount = 1;
    /** Random offset distance from the original bullet despawn/hit coordinate. */
    public float despawnUnitRadius = 0.1f;
    /** Extra visual parts for this bullet. */
    public Seq<DrawPart> parts = new Seq<>();

    /** Color of trail behind bullet. */
    public Color trailColor = Pal.missileYellowBack;
    /** Chance of trail effect spawning on bullet per tick. */
    public float trailChance = -0.0001f;
    /** Uniform interval in which trail effect is spawned. */
    public float trailInterval = 0f;
    /** Trail effect that is spawned. */
    public Effect trailEffect = Fx.missileTrail;
    /** Rotation/size parameter that is passed to trail. Usually, this controls size. */
    public float trailParam =  2f;
    /** Whether the parameter passed to the trail is the bullet rotation, instead of a flat value. */
    public boolean trailRotation = false;
    /** Interpolation for trail width as function of bullet lifetime */
    public Interp trailInterp = Interp.one;
    /** Length of trail quads. Any value <= 0 disables the trail. */
    public int trailLength = -1;
    /** Width of trail, if trailLength > 0 */
    public float trailWidth = 2f;
    /** If trailSinMag > 0, these values are applied as a sine curve to trail width. */
    public float trailSinMag = 0f, trailSinScl = 3f;

    /** Use a negative value to disable splash damage. */
    public float splashDamageRadius = -1f;
    /** If true, splash damage pierces through tiles. */
    public boolean splashDamagePierce = false;

    /** Amount of fires attempted around bullet. */
    public int incendAmount = 0;
    /** Spread of fires around bullet. */
    public float incendSpread = 8f;
    /** Chance of fire being created. */
    public float incendChance = 1f;

    /** Power of bullet ability. Usually a number between 0 and 1; try 0.1 as a starting point. */
    public float homingPower = 0f;
    /** Range of homing effect around bullet. */
    public float homingRange = 50f;
    /** Use a negative value to disable homing delay. */
    public float homingDelay = -1f;

    /** Range of healing block suppression effect. */
    public float suppressionRange = -1f;
    /** Duration of healing block suppression effect. */
    public float suppressionDuration = 60f * 8f;
    /** Chance of suppression effect occurring on block, scaled down by number of blocks. */
    public float suppressionEffectChance = 50f;

    /** Color of lightning created by bullet. */
    public Color lightningColor = Pal.surge;
    /** Number of separate lightning "roots". */
    public int lightning;
    /** Length of each lightning strand. */
    public int lightningLength = 5;
    /** Extra random length added onto base length of lightning. */
    public int lightningLengthRand = 0;
    /** Use a negative value to use default bullet damage. */
    public float lightningDamage = -1;
    /** Spread of lightning, relative to bullet rotation. */
    public float lightningCone = 360f;
    /** Offset of lightning relative to bullet rotation. */
    public float lightningAngle = 0f;
    /** The bullet created at lightning points. */
    public @Nullable BulletType lightningType = null;

    /** Scale of bullet weave pattern. Higher -> less vibration. */
    public float weaveScale = 1f;
    /** Intensity of bullet weaving. Note that this may make bullets inaccurate. */
    public float weaveMag = 0f;
    /** If true, the bullet weave will randomly switch directions on spawn. */
    public boolean weaveRandom = true;

    /** Number of individual puddles created. */
    public int puddles;
    /** Range of puddles around bullet position. */
    public float puddleRange;
    /** Liquid count of each puddle created. */
    public float puddleAmount = 5f;
    /** Liquid that puddles created are made of. */
    public Liquid puddleLiquid = Liquids.water;

    /** Whether to display the ammo multiplayer for this bullet type in its stats. */
    public boolean displayAmmoMultiplier = true;

    /** Radius of light emitted by this bullet; <0 to use defaults. */
    public float lightRadius = -1f;
    /** Opacity of light color. */
    public float lightOpacity = 0.3f;
    /** Color of light emitted by this bullet. */
    public Color lightColor = Pal.powerLight;

    public BulletType(float speed, float damage){
        this.speed = speed;
        this.damage = damage;
    }

    public BulletType(){
    }

    public BulletType copy(){
        try{
            BulletType copy = (BulletType)clone();
            copy.id = (short)Vars.content.getBy(getContentType()).size;
            Vars.content.handleContent(copy);
            return copy;
        }catch(Exception e){
            throw new RuntimeException("death to checked exceptions", e);
        }
    }

    @Override
    public void load(){
        for(var part : parts){
            part.turretShading = false;
            part.load(null);
        }
    }

    /** @return estimated damage per shot. this can be very inaccurate. */
    public float estimateDPS(){
        if(spawnUnit != null){
            return spawnUnit.estimateDps();
        }

        float sum = damage + splashDamage*0.75f;
        if(fragBullet != null && fragBullet != this){
            sum += fragBullet.estimateDPS() * fragBullets / 2f;
        }
        return sum;
    }

    /** @return maximum distance the bullet this bullet type has can travel. */
    protected float calculateRange(){
        if(rangeOverride > 0) return rangeOverride;
        if(spawnUnit != null) return spawnUnit.lifetime * spawnUnit.speed;
        return Math.max(Mathf.zero(drag) ? speed * lifetime : speed * (1f - Mathf.pow(1f - drag, lifetime)) / drag, maxRange);
    }

    /** @return continuous damage in damage/sec, or -1 if not continuous. */
    public float continuousDamage(){
        return -1f;
    }

    public boolean heals(){
        return healPercent > 0 || healAmount > 0;
    }

    public boolean testCollision(Bullet bullet, Building tile){
        return !heals() || tile.team != bullet.team || tile.healthf() < 1f;
    }

    /** If direct is false, this is an indirect hit and the tile was already damaged.
     * TODO this is a mess. */
    public void hitTile(Bullet b, Building build, float x, float y, float initialHealth, boolean direct){
        if(makeFire && build.team != b.team){
            Fires.create(build.tile);
        }

        if(heals() && build.team == b.team && !(build.block instanceof ConstructBlock)){
            healEffect.at(build.x, build.y, 0f, healColor, build.block);
            build.heal(healPercent / 100f * build.maxHealth + healAmount);
        }else if(build.team != b.team && direct){
            hit(b);
        }

        handlePierce(b, initialHealth, x, y);
    }

    public void hitEntity(Bullet b, Hitboxc entity, float health){
        boolean wasDead = entity instanceof Unit u && u.dead;

        if(entity instanceof Healthc h){
            if(pierceArmor){
                h.damagePierce(b.damage);
            }else{
                h.damage(b.damage);
            }
        }

        if(entity instanceof Unit unit){
            Tmp.v3.set(unit).sub(b).nor().scl(knockback * 80f);
            if(impact) Tmp.v3.setAngle(b.rotation() + (knockback < 0 ? 180f : 0f));
            unit.impulse(Tmp.v3);
            unit.apply(status, statusDuration);

            Events.fire(bulletDamageEvent.set(unit, b));
        }

        if(!wasDead && entity instanceof Unit unit && unit.dead){
            Events.fire(new UnitBulletDestroyEvent(unit, b));
        }

        handlePierce(b, health, entity.x(), entity.y());
    }

    public void handlePierce(Bullet b, float initialHealth, float x, float y){
        float sub = Mathf.zero(pierceDamageFactor) ? 0f : Math.max(initialHealth * pierceDamageFactor, 0);
        //subtract health from each consecutive pierce
        b.damage -= Float.isNaN(sub) ? b.damage : Math.min(b.damage, sub);

        if(removeAfterPierce && b.damage <= 0){
            b.hit = true;
            b.remove();
        }
    }

    public float damageMultiplier(Bullet b){
        if(b.owner instanceof Unit u) return u.damageMultiplier() * state.rules.unitDamage(b.team);
        if(b.owner instanceof Building) return state.rules.blockDamage(b.team);

        return 1f;
    }

    public void hit(Bullet b){
        hit(b, b.x, b.y);
    }

    public void hit(Bullet b, float x, float y){
        hitEffect.at(x, y, b.rotation(), hitColor);
        hitSound.at(x, y, hitSoundPitch, hitSoundVolume);

        Effect.shake(hitShake, hitShake, b);

        if(fragOnHit){
            createFrags(b, x, y);
        }
        createPuddles(b, x, y);
        createIncend(b, x, y);
        createUnits(b, x, y);

        if(suppressionRange > 0){
            //bullets are pooled, require separate Vec2 instance
            Damage.applySuppression(b.team, b.x, b.y, suppressionRange, suppressionDuration, 0f, suppressionEffectChance, new Vec2(b.x, b.y));
        }

        createSplashDamage(b, x, y);

        for(int i = 0; i < lightning; i++){
            Lightning.create(b, lightningColor, lightningDamage < 0 ? damage : lightningDamage, b.x, b.y, b.rotation() + Mathf.range(lightningCone/2) + lightningAngle, lightningLength + Mathf.random(lightningLengthRand));
        }
    }

    public void createIncend(Bullet b, float x, float y){
        if(incendChance > 0 && Mathf.chance(incendChance)){
            Damage.createIncend(x, y, incendSpread, incendAmount);
        }
    }

    public void createPuddles(Bullet b, float x, float y){
        if(puddleLiquid != null && puddles > 0){
            for(int i = 0; i < puddles; i++){
                Tile tile = world.tileWorld(x + Mathf.range(puddleRange), y + Mathf.range(puddleRange));
                Puddles.deposit(tile, puddleLiquid, puddleAmount);
            }
        }
    }

    public void createSplashDamage(Bullet b, float x, float y){
        if(splashDamageRadius > 0 && !b.absorbed){
            Damage.damage(b.team, x, y, splashDamageRadius, splashDamage * b.damageMultiplier(), splashDamagePierce, collidesAir, collidesGround, scaledSplashDamage, b);

            if(status != StatusEffects.none){
                Damage.status(b.team, x, y, splashDamageRadius, status, statusDuration, collidesAir, collidesGround);
            }

            if(heals()){
                indexer.eachBlock(b.team, x, y, splashDamageRadius, Building::damaged, other -> {
                    healEffect.at(other.x, other.y, 0f, healColor, other.block);
                    other.heal(healPercent / 100f * other.maxHealth() + healAmount);
                });
            }

            if(makeFire){
                indexer.eachBlock(null, x, y, splashDamageRadius, other -> other.team != b.team, other -> Fires.create(other.tile));
            }
        }
    }

    public void createFrags(Bullet b, float x, float y){
        if(fragBullet != null && (fragOnAbsorb || !b.absorbed)){
            for(int i = 0; i < fragBullets; i++){
                float len = Mathf.random(1f, 7f);
                float a = b.rotation() + Mathf.range(fragRandomSpread / 2) + fragAngle + ((i - fragBullets/2) * fragSpread);
                fragBullet.create(b, x + Angles.trnsx(a, len), y + Angles.trnsy(a, len), a, Mathf.random(fragVelocityMin, fragVelocityMax), Mathf.random(fragLifeMin, fragLifeMax));
            }
        }
    }

    public void createUnits(Bullet b, float x, float y){
        if(despawnUnit != null){
            for(int i = 0; i < despawnUnitCount; i++){
                despawnUnit.spawn(b.team, x + Mathf.range(despawnUnitRadius), y + Mathf.range(despawnUnitRadius));
            }
        }
    }

    /** Called when the bullet reaches the end of its lifetime or is destroyed by something external. */
    public void despawned(Bullet b){
        if(despawnHit){
            hit(b);
        }else{
            createUnits(b, b.x, b.y);
        }

        if(!fragOnHit){
            createFrags(b, b.x, b.y);
        }
        
        despawnEffect.at(b.x, b.y, b.rotation(), hitColor);
        despawnSound.at(b);

        Effect.shake(despawnShake, despawnShake, b);
    }

    /** Called when the bullet is removed for any reason. */
    public void removed(Bullet b){
        if(trailLength > 0 && b.trail != null && b.trail.size() > 0){
            Fx.trailFade.at(b.x, b.y, trailWidth, trailColor, b.trail.copy());
        }
    }

    public void draw(Bullet b){
        drawTrail(b);
        drawParts(b);
    }

    public void drawTrail(Bullet b){
        if(trailLength > 0 && b.trail != null){
            //draw below bullets? TODO
            float z = Draw.z();
            Draw.z(z - 0.0001f);
            b.trail.draw(trailColor, trailWidth);
            Draw.z(z);
        }
    }

    public void drawParts(Bullet b){
        if(parts.size > 0){
            DrawPart.params.set(b.fin(), 0f, 0f, 0f, 0f, 0f, b.x, b.y, b.rotation());
            DrawPart.params.life = b.fin();

            for(int i = 0; i < parts.size; i++){
                parts.get(i).draw(DrawPart.params);
            }
        }
    }

    public void drawLight(Bullet b){
        if(lightOpacity <= 0f || lightRadius <= 0f) return;
        Drawf.light(b, lightRadius, lightColor, lightOpacity);
    }

    public void init(Bullet b){

        if(killShooter && b.owner() instanceof Healthc h && !h.dead()){
            h.kill();
        }

        if(instantDisappear){
            b.time = lifetime + 1f;
        }

        if(spawnBullets.size > 0){
            for(var bullet : spawnBullets){
                bullet.create(b, b.x, b.y, b.rotation());
            }
        }
    }

    public void update(Bullet b){
        updateTrail(b);
        updateHoming(b);
        updateWeaving(b);
        updateTrailEffects(b);
        updateBulletInterval(b);
    }

    public void updateBulletInterval(Bullet b){
        if(intervalBullet != null && b.time >= intervalDelay && b.timer.get(2, bulletInterval)){
            float ang = b.rotation();
            for(int i = 0; i < intervalBullets; i++){
                intervalBullet.create(b, b.x, b.y, ang + Mathf.range(intervalRandomSpread) + intervalAngle + ((i - (intervalBullets - 1f)/2f) * intervalSpread));
            }
        }
    }

    public void updateHoming(Bullet b){
        if(homingPower > 0.0001f && b.time >= homingDelay){
            float realAimX = b.aimX < 0 ? b.x : b.aimX;
            float realAimY = b.aimY < 0 ? b.y : b.aimY;

            Teamc target;
            //home in on allies if possible
            if(heals()){
                target = Units.closestTarget(null, realAimX, realAimY, homingRange,
                e -> e.checkTarget(collidesAir, collidesGround) && e.team != b.team && !b.hasCollided(e.id),
                t -> collidesGround && (t.team != b.team || t.damaged()) && !b.hasCollided(t.id)
                );
            }else{
                if(b.aimTile != null && b.aimTile.build != null && b.aimTile.build.team != b.team && collidesGround && !b.hasCollided(b.aimTile.build.id)){
                    target = b.aimTile.build;
                }else{
                    target = Units.closestTarget(b.team, realAimX, realAimY, homingRange,
                        e -> e != null && e.checkTarget(collidesAir, collidesGround) && !b.hasCollided(e.id),
                        t -> t != null && collidesGround && !b.hasCollided(t.id));
                }
            }

            if(target != null){
                b.vel.setAngle(Angles.moveToward(b.rotation(), b.angleTo(target), homingPower * Time.delta * 50f));
            }
        }
    }

    public void updateWeaving(Bullet b){
        if(weaveMag != 0){
            b.vel.rotateRadExact((float)Math.sin((b.time + Math.PI * weaveScale/2f) / weaveScale) * weaveMag * (weaveRandom ? (Mathf.randomSeed(b.id, 0, 1) == 1 ? -1 : 1) : 1f) * Time.delta * Mathf.degRad);
        }
    }

    public void updateTrailEffects(Bullet b){
        if(trailChance > 0){
            if(Mathf.chanceDelta(trailChance)){
                trailEffect.at(b.x, b.y, trailRotation ? b.rotation() : trailParam, trailColor);
            }
        }

        if(trailInterval > 0f){
            if(b.timer(0, trailInterval)){
                trailEffect.at(b.x, b.y, trailRotation ? b.rotation() : trailParam, trailColor);
            }
        }
    }
    
    public void updateTrail(Bullet b){
        if(!headless && trailLength > 0){
            if(b.trail == null){
                b.trail = new Trail(trailLength);
            }
            b.trail.length = trailLength;
            b.trail.update(b.x, b.y, trailInterp.apply(b.fin()) * (1f + (trailSinMag > 0 ? Mathf.absin(Time.time, trailSinScl, trailSinMag) : 0f)));
        }
    }

    @Override
    public void init(){
        if(pierceCap >= 1){
            pierce = true;
            //pierceBuilding is not enabled by default, because a bullet may want to *not* pierce buildings
        }

        if(setDefaults){
            if(lightning > 0){
                if(status == StatusEffects.none){
                    status = StatusEffects.shocked;
                }
            }

            if(fragBullet != null || splashDamageRadius > 0 || lightning > 0){
                despawnHit = true;
            }
        }

        if(fragBullet != null){
            fragBullet.keepVelocity = false;
        }

        if(lightningType == null){
            lightningType = !collidesAir ? Bullets.damageLightningGround : Bullets.damageLightning;
        }

        if(lightRadius <= -1){
            lightRadius = Math.max(18, hitSize * 5f);
        }
        
        drawSize = Math.max(drawSize, trailLength * speed * 2f);
        range = calculateRange();
    }

    @Override
    public ContentType getContentType(){
        return ContentType.bullet;
    }

    public @Nullable Bullet create(Teamc owner, float x, float y, float angle){
        return create(owner, owner.team(), x, y, angle);
    }

    public @Nullable Bullet create(Entityc owner, Team team, float x, float y, float angle){
        return create(owner, team, x, y, angle, 1f);
    }

    public @Nullable Bullet create(Entityc owner, Team team, float x, float y, float angle, float velocityScl){
        return create(owner, team, x, y, angle, -1, velocityScl, 1f, null);
    }

    public @Nullable Bullet create(Entityc owner, Team team, float x, float y, float angle, float velocityScl, float lifetimeScl){
        return create(owner, team, x, y, angle, -1, velocityScl, lifetimeScl, null);
    }

    public @Nullable Bullet create(Entityc owner, Team team, float x, float y, float angle, float velocityScl, float lifetimeScl, Mover mover){
        return create(owner, team, x, y, angle, -1, velocityScl, lifetimeScl, null, mover);
    }

    public @Nullable Bullet create(Bullet parent, float x, float y, float angle){
        return create(parent.owner, parent.team, x, y, angle);
    }

    public @Nullable Bullet create(Bullet parent, float x, float y, float angle, float velocityScl, float lifeScale){
        return create(parent.owner, parent.team, x, y, angle, velocityScl, lifeScale);
    }

    public @Nullable Bullet create(Bullet parent, float x, float y, float angle, float velocityScl){
        return create(parent.owner(), parent.team, x, y, angle, velocityScl);
    }

    public @Nullable Bullet create(@Nullable Entityc owner, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl, Object data){
        return create(owner, team, x, y, angle, damage, velocityScl, lifetimeScl, data, null);
    }

    public @Nullable Bullet create(@Nullable Entityc owner, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl, Object data, @Nullable Mover mover){
        return create(owner, team, x, y, angle, damage, velocityScl, lifetimeScl, data, mover, -1f, -1f);
    }

    public @Nullable Bullet create(@Nullable Entityc owner, Team team, float x, float y, float angle, float damage, float velocityScl, float lifetimeScl, Object data, @Nullable Mover mover, float aimX, float aimY){
        if(spawnUnit != null){
            //don't spawn units clientside!
            if(!net.client()){
                Unit spawned = spawnUnit.create(team);
                spawned.set(x, y);
                spawned.rotation = angle;

                //immediately spawn at top speed, since it was launched
                if(spawnUnit.missileAccelTime <= 0f){
                    spawned.vel.trns(angle, spawnUnit.speed);
                }
                
                //assign unit owner
                if(spawned.controller() instanceof MissileAI ai){
                    if(owner instanceof Unit unit){
                        ai.shooter = unit;
                    }
                    if(owner instanceof ControlBlock control){
                        ai.shooter = control.unit();
                    }
                }
                spawned.add();
            }

            //Since bullet init is never called, handle killing shooter here
            if(killShooter && owner instanceof Healthc h && !h.dead()) h.kill();

            //no bullet returned
            return null;
        }

        Bullet bullet = Bullet.create();
        bullet.type = this;
        bullet.owner = owner;
        bullet.team = team;
        bullet.time = 0f;
        bullet.originX = x;
        bullet.originY = y;
        bullet.aimTile = world.tileWorld(aimX, aimY);
        bullet.aimX = aimX;
        bullet.aimY = aimY;
        bullet.initVel(angle, speed * velocityScl);
        if(backMove){
            bullet.set(x - bullet.vel.x * Time.delta, y - bullet.vel.y * Time.delta);
        }else{
            bullet.set(x, y);
        }
        bullet.lifetime = lifetime * lifetimeScl;
        bullet.data = data;
        bullet.drag = drag;
        bullet.hitSize = hitSize;
        bullet.mover = mover;
        bullet.damage = (damage < 0 ? this.damage : damage) * bullet.damageMultiplier();
        //reset trail
        if(bullet.trail != null){
            bullet.trail.clear();
        }
        bullet.add();

        if(keepVelocity && owner instanceof Velc v) bullet.vel.add(v.vel());
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
