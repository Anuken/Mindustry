package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.Units.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Turret extends ReloadTurret{
    //after being logic-controlled and this amount of time passes, the turret will resume normal AI
    public final static float logicControlCooldown = 60 * 2;

    public final int timerTarget = timers++;
    /** Ticks between attempt at finding a target. */
    public float targetInterval = 20;
    /** Target interval for when this turret already has a valid target. -1 = targetInterval */
    public float newTargetInterval = -1f;

    /** Maximum ammo units stored. */
    public int maxAmmo = 30;
    /** Ammo units used per shot. */
    public int ammoPerShot = 1;
    /** If true, ammo is only consumed once per shot regardless of bullet count. */
    public boolean consumeAmmoOnce = true;
    /** Minimum input heat required to fire. */
    public float heatRequirement = -1f;
    /** Maximum efficiency possible, if this turret uses heat. */
    public float maxHeatEfficiency = 3f;

    /** Bullet angle randomness in degrees. */
    public float inaccuracy = 0f;
    /** Fraction of bullet velocity that is random. */
    public float velocityRnd = 0f;
    /** Fraction of lifetime that is added to bullets with lifeScale. */
    public float scaleLifetimeOffset = 0f;
    /** Maximum angle difference in degrees at which turret will still try to shoot. */
    public float shootCone = 8f;
    /** Turret shoot point. */
    public float shootX = 0f, shootY = Float.NEGATIVE_INFINITY;
    /** Random spread on the X axis. */
    public float xRand = 0f;
    /** If true, a range ring is also drawn for minRange. */
    public boolean drawMinRange;
    /** Range at which it finds and locks on to the target, but does not shoot. */
    public float trackingRange = 0f;
    /** Minimum bullet range. Used for artillery only. */
    public float minRange = 0f;
    /** Minimum warmup needed to fire. */
    public float minWarmup = 0f;
    /** If true, this turret will accurately target moving targets with respect to shoot.firstShotDelay. */
    public boolean accurateDelay = true;
    /** If false, this turret can't move while charging. */
    public boolean moveWhileCharging = true;
    /** If false, this turret can't reload while charging */
    public boolean reloadWhileCharging = true;
    /** How long warmup is maintained even if this turret isn't shooting. */
    public float warmupMaintainTime = 0f;
    /** pattern used for bullets */
    public ShootPattern shoot = new ShootPattern();

    /** If true, this block targets air units. */
    public boolean targetAir = true;
    /** If true, this block targets ground units and structures. */
    public boolean targetGround = true;
    /** If true, this block targets blocks. */
    public boolean targetBlocks = true;
    /** If true, this block targets friend blocks, to heal them. */
    public boolean targetHealing = false;
    /** If true, this turret can be controlled by players. */
    public boolean playerControllable = true;
    /** If true, this block will display ammo multipliers in its stats (irrelevant for certain types of turrets). */
    public boolean displayAmmoMultiplier = true;
    /** If false, 'under' blocks like conveyors are not targeted. */
    public boolean targetUnderBlocks = true;
    /** If true, the turret will always shoot when it has ammo, regardless of targets in range or any control. */
    public boolean alwaysShooting = false;
    /** Whether this turret predicts unit movement. */
    public boolean predictTarget = true;
    /** Function for choosing which unit to target. */
    public Sortf unitSort = UnitSorts.closest;
    /** Filter for types of units to attack. */
    public Boolf<Unit> unitFilter = u -> true;
    /** Filter for types of buildings to attack. */
    public Boolf<Building> buildingFilter = b -> targetUnderBlocks || !b.block.underBullets;

    /** Color of heat region drawn on top (if found) */
    public Color heatColor = Pal.turretHeat;
    /** Optional override for all shoot effects. */
    public @Nullable Effect shootEffect;
    /** Optional override for all smoke effects. */
    public @Nullable Effect smokeEffect;
    /** Effect created when ammo is used. Not optional. */
    public Effect ammoUseEffect = Fx.none;
    /** Sound emitted when a single bullet is shot. */
    public Sound shootSound = Sounds.shootDuo;
    /** Volume of shooting sound. */
    public float shootSoundVolume = 1f;
    /** Sound emitted when shoot.firstShotDelay is >0 and shooting begins. */
    public Sound chargeSound = Sounds.none;
    /** The sound that this block makes while active. One sound loop. Do not overuse. */
    public Sound loopSound = Sounds.none;
    /** Active sound base volume. */
    public float loopSoundVolume = 0.5f;
    /** Range for pitch of shoot sound. */
    public float soundPitchMin = 0.9f, soundPitchMax = 1.1f;
    /** Backwards Y offset of ammo eject effect. */
    public float ammoEjectBack = 1f;
    /** Lerp speed of turret warmup. */
    public float shootWarmupSpeed = 0.1f;
    /** If true, turret warmup is linear instead of a curve. */
    public boolean linearWarmup = false;
    /** Visual amount by which the turret recoils back per shot. */
    public float recoil = 1f;
    /** Number of additional counters for recoil. */
    public int recoils = -1;
    /** ticks taken for turret to return to starting position in ticks. uses reload time by default  */
    public float recoilTime = -1f;
    /** power curve applied to visual recoil */
    public float recoilPow = 1.8f;
    /** ticks to cool down the heat region */
    public float cooldownTime = 20f;
    /** Visual elevation of turret shadow, -1 to use defaults. */
    public float elevation = -1f;
    /** How much the screen shakes per shot. */
    public float shake = 0f;

    /** Defines drawing behavior for this turret. */
    public DrawBlock drawer = new DrawTurret();

    public Turret(String name){
        super(name);
        liquidCapacity = 20f;
        outlinedIcon = 1;
        drawLiquidLight = false;
        sync = true;
        rotate = true;
        quickRotate = false;
        drawArrow = false;
        ignoreLineRotation = true;
        rotateDrawEditor = false;
        visualRotationOffset = -90f;
        regionRotated1 = 1;
        regionRotated2 = 2;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.inaccuracy, (int)inaccuracy, StatUnit.degrees);
        stats.add(Stat.reload, 60f / (reload + (!reloadWhileCharging ? shoot.firstShotDelay : 0f)) * shoot.shots, StatUnit.perSecond);
        stats.add(Stat.targetsAir, targetAir);
        stats.add(Stat.targetsGround, targetGround);
        if(ammoPerShot != 1) stats.add(Stat.ammoUse, ammoPerShot, StatUnit.perShot);
        if(heatRequirement > 0) stats.add(Stat.input, heatRequirement, StatUnit.heatUnits);
        if(heatRequirement > 0 && maxHeatEfficiency > 0) stats.add(Stat.maxEfficiency, (int)(maxHeatEfficiency * 100f), StatUnit.percent);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(heatRequirement > 0){
            addBar("heat", (TurretBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.heatpercent", (int)entity.heatReq, (int)(Math.min(entity.heatReq / heatRequirement, maxHeatEfficiency) * 100)),
            () -> Pal.lightOrange,
            () -> entity.heatReq / heatRequirement));
        }
    }

    @Override
    public void init(){
        if(shootY == Float.NEGATIVE_INFINITY) shootY = size * tilesize / 2f;
        if(elevation < 0) elevation = size / 2f;
        if(recoilTime < 0f) recoilTime = reload;
        if(cooldownTime < 0f) cooldownTime = reload;
        if(newTargetInterval <= 0f) newTargetInterval = targetInterval;

        if(!targetGround){
            disableOverlapCheck = true;
        }

        super.init();
        trackingRange = Math.max(range, trackingRange);
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out){
        drawer.getRegionsToOutline(this, out);
    }

    public void limitRange(BulletType bullet, float margin){
        float realRange = bullet.rangeChange + range;
        //doesn't handle drag
        bullet.lifetime = (realRange + margin + bullet.extraRangeMargin + 10f) / bullet.speed;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        if(drawMinRange){
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, minRange, Pal.placing);
        }
    }

    public static abstract class AmmoEntry{
        public int amount;

        public abstract BulletType type();
    }


    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        super.placeEnded(tile, builder, rotation, config);
        if(rotate && tile.build instanceof TurretBuild turret){
            turret.rotation = tile.build.rotdeg();
        }
    }

    public class TurretBuild extends ReloadTurretBuild implements ControlBlock{
        //TODO storing these as instance variables is horrible design
        /** Turret sprite offset, based on recoil. Updated every frame. */
        public Vec2 recoilOffset = new Vec2();

        public Seq<AmmoEntry> ammo = new Seq<>();
        public int totalAmmo;
        public float curRecoil, heat, logicControlTime = -1;
        public @Nullable float[] curRecoils;
        public float shootWarmup, charge, warmupHold = 0f;
        public int totalShots, barrelCounter;
        public boolean logicShooting = false;
        public @Nullable Posc target;
        public Vec2 targetPos = new Vec2();
        public BlockUnitc unit = (BlockUnitc)UnitTypes.block.create(team);
        public boolean wasShooting;
        public int queuedBullets = 0;

        public float heatReq;
        public float[] sideHeat = new float[4];

        public @Nullable SoundLoop soundLoop = (loopSound == Sounds.none ? null : new SoundLoop(loopSound, loopSoundVolume));

        float lastRangeChange;

        @Override
        public void remove(){
            super.remove();
            if(soundLoop != null){
                soundLoop.stop();
            }
        }

        @Override
        public void onDestroyed(){
            super.onDestroyed();
            if(soundLoop != null){
                soundLoop.stop();
            }
        }

        @Override
        public float estimateDps(){
            if(!hasAmmo()) return 0f;
            return shoot.shots / reload * 60f * (peekAmmo() == null ? 0f : peekAmmo().estimateDPS()) * potentialEfficiency * timeScale;
        }

        public float minRange(){
            if(peekAmmo() != null){
                return minRange + peekAmmo().minRangeChange;
            }
            return minRange;
        }

        @Override
        public float range(){
            if(peekAmmo() != null){
                return range + peekAmmo().rangeChange;
            }
            return range;
        }

        public float trackingRange(){
            return range() + trackingRange - range;
        }

        @Override
        public float warmup(){
            return shootWarmup;
        }

        @Override
        public float drawrot(){
            return rotation - 90;
        }

        @Override
        public boolean shouldConsume(){
            //when the block is first placed, it shouldn't consume power/liquid just to "cool down" from the initial reload
            //thus, it should only consume once it has actually shot at something
            return isShooting() || (reloadCounter < reload && totalShots > 0);
        }

        @Override
        public BlockStatus status(){
            if(enabled && !hasAmmo()) return BlockStatus.noInput;

            return super.status();
        }

        @Override
        public boolean canControl(){
            return playerControllable;
        }

        @Override
        public void control(LAccess type, double p1, double p2, double p3, double p4){
            if(type == LAccess.shoot && !unit.isPlayer()){
                targetPos.set(World.unconv((float)p1), World.unconv((float)p2));
                logicControlTime = logicControlCooldown;
                logicShooting = !Mathf.zero(p3);
            }

            super.control(type, p1, p2, p3, p4);
        }

        @Override
        public void control(LAccess type, Object p1, double p2, double p3, double p4){
            if(type == LAccess.shootp && (unit == null || !unit.isPlayer())){
                logicControlTime = logicControlCooldown;
                logicShooting = !Mathf.zero(p2);

                if(p1 instanceof Posc pos){
                    targetPosition(pos);
                }
            }

            super.control(type, p1, p2, p3, p4);
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case ammo -> totalAmmo;
                case ammoCapacity -> maxAmmo;
                case rotation -> rotation;
                case shootX -> World.conv(targetPos.x);
                case shootY -> World.conv(targetPos.y);
                case shooting -> isShooting() ? 1 : 0;
                case progress -> progress();
                default -> super.sense(sensor);
            };
        }

        @Override
        public float fogRadius(){
            return (range + (hasAmmo() ? peekAmmo().rangeChange : 0f)) / tilesize * fogRadiusMultiplier;
        }

        @Override
        public float progress(){
            return Mathf.clamp(reloadCounter / reload);
        }

        public boolean isShooting(){
            return alwaysShooting || (isControlled() ? unit.isShooting() : logicControlled() ? logicShooting : target != null);
        }

        @Override
        public Unit unit(){
            //make sure stats are correct
            unit.tile(this);
            unit.team(team);
            return (Unit)unit;
        }

        public boolean logicControlled(){
            return logicControlTime > 0;
        }

        public boolean isActive(){
            return (target != null || wasShooting) && enabled && activationTimer <= 0;
        }

        public void targetPosition(Posc pos){
            if(!hasAmmo() || pos == null) return;
            BulletType bullet = peekAmmo();

            var offset = Tmp.v1.setZero();

            //when delay is accurate, assume unit has moved by chargeTime already
            if(accurateDelay && !moveWhileCharging && pos instanceof Hitboxc h){
                offset.set(h.deltaX(), h.deltaY()).scl(shoot.firstShotDelay / Time.delta);
            }

            if(predictTarget && bullet.speed >= 0.01f){
                targetPos.set(Predict.intercept(this, pos, offset.x, offset.y, bullet.speed));
            }else{
                targetPos.set(pos);
            }

            if(targetPos.isZero()){
                targetPos.set(pos);
            }
        }

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawSelect(){
            super.drawSelect();

            if(drawMinRange){
                Drawf.dashCircle(x, y, minRange(), team.color);
            }
        }

        @Override
        public void updateTile(){
            if(!validateTarget()) target = null;

            if(soundLoop != null){
                soundLoop.update(x, y, shouldActiveSound(), activeSoundVolume());
            }

            float warmupTarget = (isShooting() && canConsume()) || charging() ? 1f : 0f;
            if(warmupTarget > 0 && !isControlled()){
                warmupHold = 1f;
            }
            if(warmupHold > 0f){
                warmupHold -= Time.delta / warmupMaintainTime;
                warmupTarget = 1f;
            }

            if(linearWarmup){
                shootWarmup = Mathf.approachDelta(shootWarmup, warmupTarget, shootWarmupSpeed * (warmupTarget > 0 ? efficiency : 1f));
            }else{
                shootWarmup = Mathf.lerpDelta(shootWarmup, warmupTarget, shootWarmupSpeed * (warmupTarget > 0 ? efficiency : 1f));
            }

            wasShooting = false;

            curRecoil = Mathf.approachDelta(curRecoil, 0, 1 / recoilTime);
            if(recoils > 0){
                if(curRecoils == null) curRecoils = new float[recoils];
                for(int i = 0; i < recoils; i++){
                    curRecoils[i] = Mathf.approachDelta(curRecoils[i], 0, 1 / recoilTime);
                }
            }
            heat = Mathf.approachDelta(heat, 0, 1 / cooldownTime);
            charge = charging() ? Mathf.approachDelta(charge, 1, 1 / shoot.firstShotDelay) : 0;

            unit.tile(this);
            unit.rotation(rotation);
            unit.team(team);
            recoilOffset.trns(rotation, -Mathf.pow(curRecoil, recoilPow) * recoil);

            if(logicControlTime > 0){
                logicControlTime -= Time.delta;
            }

            if(heatRequirement > 0){
                heatReq = calculateHeat(sideHeat);
            }

            if(rotate){
                //sync underlying rotation; 0-3 rotation is a shadowed field
                ((Building)this).rotation = Mathf.mod(Mathf.round(rotation / 90f), 4);
            }

            //turret always reloads regardless of whether it's targeting something
            if(reloadWhileCharging || !charging()){
                updateReload();
                updateCooling();
            }

            if(state.rules.fog){
                float newRange = hasAmmo() ? peekAmmo().rangeChange : 0f;
                if(newRange != lastRangeChange){
                    lastRangeChange = newRange;
                    fogControl.forceUpdate(team, this);
                }
            }

            if(activationTimer > 0){
                activationTimer -= Time.delta;
                return;
            }

            if(hasAmmo()){
                if(Float.isNaN(reloadCounter)) reloadCounter = 0;

                if(timer(timerTarget, target != null ? newTargetInterval : targetInterval)){
                    findTarget();
                }

                if(validateTarget()){
                    boolean canShoot;

                    if(isControlled()){ //player behavior
                        targetPos.set(unit.aimX(), unit.aimY());
                        canShoot = unit.isShooting();
                    }else if(logicControlled()){ //logic behavior
                        canShoot = logicShooting;
                    }else{ //default AI behavior
                        targetPosition(target);

                        if(Float.isNaN(rotation)) rotation = 0;
                        canShoot = within(target, range() + (target instanceof Sized hb ? hb.hitSize()/1.9f : 0f));
                    }

                    if(!isControlled()){
                        unit.aimX(targetPos.x);
                        unit.aimY(targetPos.y);
                    }

                    float targetRot = angleTo(targetPos);

                    if(shouldTurn()){
                        turnToTarget(targetRot);
                    }

                    if(!alwaysShooting && Angles.angleDist(rotation, targetRot) < shootCone && canShoot){
                        wasShooting = true;
                        updateShooting();
                    }
                }else{
                    target = null;
                }

                if(alwaysShooting){
                    wasShooting = true;
                    updateShooting();
                }
            }
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            if(coolant != null && liquids.currentAmount() <= 0.001f){
                Events.fire(Trigger.turretCool);
            }

            super.handleLiquid(source, liquid, amount);
        }

        @Override
        public boolean canConsume(){
            if(heatRequirement > 0 && heatReq <= 0f){
                return false;
            }
            return super.canConsume();
        }

        protected boolean validateTarget(){
            return !Units.invalidateTarget(target, canHeal() ? Team.derelict : team, x, y) || isControlled() || logicControlled();
        }

        protected boolean canHeal(){
            return targetHealing && hasAmmo() && peekAmmo().collidesTeam && peekAmmo().heals();
        }

        protected Posc findEnemy(float range){
            if(targetAir && !targetGround){
                return Units.bestEnemy(team, x, y, range, e -> !e.dead() && !e.isGrounded() && unitFilter.get(e), unitSort);
            }else{
                var ammo = peekAmmo();
                boolean buildings = targetGround && targetBlocks && (ammo == null || ammo.targetBlocks), missiles = ammo == null || ammo.targetMissiles;
                return Units.bestTarget(team, x, y, range,
                    e -> !e.dead() && unitFilter.get(e) && (e.isGrounded() || targetAir) && (!e.isGrounded() || targetGround) && (missiles || !(e instanceof TimedKillc)),
                    b -> buildings && buildingFilter.get(b), unitSort);
            }
        }

        protected void findTarget(){
            float trackRange = trackingRange(), range = range();

            target = findEnemy(range);
            //find another target within the tracking range, but only if there's nothing else (always prioritize standard target)
            if(!Mathf.equal(trackRange, range) && target == null){
                target = findEnemy(trackRange);
            }

            if(target == null && canHeal()){
                target = Units.findAllyTile(team, x, y, range, b -> b.damaged() && b != this);
            }
        }

        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, rotateSpeed * delta() * potentialEfficiency);
        }

        public boolean shouldTurn(){
            return moveWhileCharging || !charging();
        }

        @Override
        public void updateEfficiencyMultiplier(){
            if(heatRequirement > 0){
                efficiency *= Math.min(Math.max(heatReq / heatRequirement, cheating() ? 1f : 0f), maxHeatEfficiency);
            }
        }

        /** Consume ammo and return a type. */
        public BulletType useAmmo(){
            if(cheating()) return peekAmmo();

            AmmoEntry entry = ammo.peek();
            entry.amount -= ammoPerShot;
            if(entry.amount <= 0) ammo.pop();
            totalAmmo -= ammoPerShot;
            totalAmmo = Math.max(totalAmmo, 0);
            return entry.type();
        }

        /** @return the ammo type that will be returned if useAmmo is called. */
        public @Nullable BulletType peekAmmo(){
            return ammo.size == 0 ? null : ammo.peek().type();
        }

        /** @return whether the turret has ammo. */
        public boolean hasAmmo(){
            //skip first entry if it has less than the required amount of ammo
            if(ammo.size >= 2 && ammo.peek().amount < ammoPerShot){
                for(int i = 0; i < ammo.size; i ++){
                    if(ammo.get(i).amount >= ammoPerShot){
                        ammo.swap(ammo.size - 1, i);
                        break;
                    }
                }
            }

            //used for "side-ammo" like gas in some turrets
            if(!canConsume()) return false;

            return ammo.size > 0 && (ammo.peek().amount >= ammoPerShot || cheating());
        }

        public boolean charging(){
            return queuedBullets > 0 && shoot.firstShotDelay > 0;
        }

        protected void updateReload(){
            reloadCounter += delta() * ammoReloadMultiplier() * baseReloadSpeed();

            //cap reload for visual reasons
            reloadCounter = Math.min(reloadCounter, reload);
        }

        @Override
        protected float ammoReloadMultiplier(){
            return hasAmmo() ? peekAmmo().reloadMultiplier : 1f;
        }

        protected void updateShooting(){

            if(reloadCounter >= reload && !charging() && shootWarmup >= minWarmup){
                BulletType type = peekAmmo();

                shoot(type);

                reloadCounter %= reload;
            }
        }

        protected void shoot(BulletType type){
            float
            bulletX = x + Angles.trnsx(rotation - 90, shootX, shootY),
            bulletY = y + Angles.trnsy(rotation - 90, shootX, shootY);

            if(shoot.firstShotDelay > 0){
                chargeSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));
                type.chargeEffect.at(bulletX, bulletY, rotation);
            }

            ShootPattern pattern = type.shootPattern != null ? type.shootPattern : shoot;

            pattern.shoot(barrelCounter, (xOffset, yOffset, angle, delay, mover) -> {
                queuedBullets++;
                int barrel = barrelCounter;

                if(delay > 0f){
                    Time.run(delay, () -> {
                        //hack: make sure the barrel is the same as what it was when the bullet was queued to fire
                        int prev = barrelCounter;
                        barrelCounter = barrel;
                        bullet(type, xOffset, yOffset, angle, mover);
                        barrelCounter = prev;
                    });
                }else{
                    bullet(type, xOffset, yOffset, angle, mover);
                }
            }, () -> barrelCounter++);

            if(consumeAmmoOnce){
                useAmmo();
            }
        }

        protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
            queuedBullets --;

            if(dead || (!consumeAmmoOnce && !hasAmmo())) return;

            float
            xSpread = Mathf.range(xRand),
            bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
            bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset + xSpread, shootY + yOffset),
            shootAngle = rotation + angleOffset + Mathf.range(inaccuracy + type.inaccuracy);

            float lifeScl = type.scaleLife ? Mathf.clamp((1 + scaleLifetimeOffset) * Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange() / type.range, range() / type.range) : 1f;

            //TODO aimX / aimY for multi shot turrets?
            handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, shootAngle - rotation);

            (shootEffect == null ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
            (smokeEffect == null ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
            (type.shootSound != Sounds.none ? type.shootSound : shootSound).at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax), shootSoundVolume);

            ammoUseEffect.at(
                x - Angles.trnsx(rotation, ammoEjectBack),
                y - Angles.trnsy(rotation, ammoEjectBack),
                rotation * Mathf.sign(xOffset)
            );

            if(shake > 0){
                Effect.shake(shake, shake, this);
            }

            curRecoil = 1f;
            if(recoils > 0){
                curRecoils[barrelCounter % recoils] = 1f;
            }
            heat = 1f;
            totalShots++;

            if(!consumeAmmoOnce){
                useAmmo();
            }
        }

        protected void handleBullet(@Nullable Bullet bullet, float offsetX, float offsetY, float angleOffset){

        }

        public float activeSoundVolume(){
            return shootWarmup;
        }

        public boolean shouldActiveSound(){
            return shootWarmup > 0.01f && loopSound != Sounds.none;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(reloadCounter);
            write.f(rotation);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision >= 1){
                reloadCounter = read.f();
                rotation = read.f();
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void readSync(Reads read, byte revision){
            //maintain rotation and reload when syncing so clients don't see turrets snapping around
            float oldRot = rotation, oldReload = reloadCounter;

            readAll(read, revision);

            rotation = oldRot;
            reloadCounter = oldReload;
        }
    }

    public static class BulletEntry{
        public Bullet bullet;
        public float x, y, rotation, life;

        public BulletEntry(Bullet bullet, float x, float y, float rotation, float life){
            this.bullet = bullet;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.life = life;
        }
    }
}
