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
import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.Units.*;
import mindustry.entities.bullet.*;
import mindustry.entities.pattern.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Turret extends ReloadTurret{
    //after being logic-controlled and this amount of time passes, the turret will resume normal AI
    public final static float logicControlCooldown = 60 * 2;

    public final int timerTarget = timers++;
    public int targetInterval = 20;

    public Color heatColor = Pal.turretHeat;
    public Effect shootEffect = Fx.none;
    public Effect smokeEffect = Fx.none;
    public Effect ammoUseEffect = Fx.none;
    public Sound shootSound = Sounds.shoot;
    public Sound chargeSound = Sounds.none;
    public float soundPitchMin = 0.9f, soundPitchMax = 1.1f;

    //visuals TODO document
    public float ammoEjectBack = 1f;
    public float shootWarmupSpeed = 0.1f;
    public boolean linearWarmup = false;
    public float recoilAmount = 1f;
    public float restitution = 0.02f;
    public float cooldown = 0.02f;
    public float elevation = -1f;
    public float shootShake = 0f;

    public int maxAmmo = 30;
    public int ammoPerShot = 1;
    /** If true, ammo is only consumed once per shot regardless of bullet count. */
    public boolean consumeAmmoOnce = false;
    public float heatRequirement = -1f;
    public float maxHeatEfficiency = 3f;

    //TODO clean all of this up + weapon consistency

    /** Bullet angle randomness in degrees. */
    public float inaccuracy = 0f;
    /** Fraction of bullet velocity that is random. */
    public float velocityRnd = 0f;
    /** Maximum angle difference in degrees at which turret will still try to shoot. */
    public float shootCone = 8f;
    /** Turret shoot point. */
    public float shootX = 0f, shootY = Float.NEGATIVE_INFINITY;
    /** Currently used for artillery only. */
    public float minRange = 0f;
    /** Minimum warmup needed to fire. */
    public float minWarmup = 0f;
    /** If true, this turret will accurately target moving targets with respect to charge time. */
    public boolean accurateDelay = true;
    /** If false, this turret can't move while charging. */
    public boolean moveWhileCharging = true;
    /** pattern used for bullets */
    public ShootPattern shoot = new ShootPattern();

    public boolean targetAir = true;
    public boolean targetGround = true;
    public boolean targetHealing = false;
    public boolean playerControllable = true;
    public boolean displayAmmoMultiplier = true;
    public Sortf unitSort = UnitSorts.closest;
    public Boolf<Unit> unitFilter = u -> true;
    public Boolf<Building> buildingFilter = b -> !b.block.underBullets;

    public DrawBlock drawer = new DrawTurret();

    public Turret(String name){
        super(name);
        liquidCapacity = 20f;
        quickRotate = false;
        outlinedIcon = 1;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.inaccuracy, (int)inaccuracy, StatUnit.degrees);
        stats.add(Stat.reload, 60f / (reload) * shoot.shots, StatUnit.perSecond);
        stats.add(Stat.targetsAir, targetAir);
        stats.add(Stat.targetsGround, targetGround);
        if(ammoPerShot != 1) stats.add(Stat.ammoUse, ammoPerShot, StatUnit.perShot);
        if(heatRequirement > 0) stats.add(Stat.input, heatRequirement, StatUnit.heatUnits);
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

        super.init();
    }

    @Override
    public void load(){
        super.load();

        drawer.load(this);
    }

    @Override
    public TextureRegion[] icons(){
        return drawer.finalIcons(this);
    }

    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out){
        drawer.getRegionsToOutline(this, out);
    }

    public static abstract class AmmoEntry{
        public int amount;

        public abstract BulletType type();
    }

    public class TurretBuild extends ReloadTurretBuild implements ControlBlock{
        //TODO storing these as instance variables is horrible design
        /** Turret sprite offset, based on recoil. Updated every frame. */
        public Vec2 recoilOffset = new Vec2();

        public Seq<AmmoEntry> ammo = new Seq<>();
        public int totalAmmo;
        public float recoil, heat, logicControlTime = -1;
        public float shootWarmup;
        public int totalShots;
        public boolean logicShooting = false;
        public @Nullable Posc target;
        public Vec2 targetPos = new Vec2();
        public BlockUnitc unit = (BlockUnitc)UnitTypes.block.create(team);
        public boolean wasShooting;
        public int queuedBullets = 0;

        public float heatReq;
        public float[] sideHeat = new float[4];

        public float estimateDps(){
            if(!hasAmmo()) return 0f;
            return shoot.shots / reload * 60f * peekAmmo().estimateDPS() * potentialEfficiency * timeScale;
        }

        @Override
        public float range(){
            if(hasAmmo()){
                return range + peekAmmo().rangeChange;
            }
            return range;
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
            return isShooting() || reloadCounter < reload;
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
        public float progress(){
            return Mathf.clamp(reloadCounter / reload);
        }

        public boolean isShooting(){
            return (isControlled() ? unit.isShooting() : logicControlled() ? logicShooting : target != null);
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
            return (target != null || wasShooting) && enabled;
        }

        public void targetPosition(Posc pos){
            if(!hasAmmo() || pos == null) return;
            BulletType bullet = peekAmmo();

            var offset = Tmp.v1.setZero();

            //when delay is accurate, assume unit has moved by chargeTime already
            if(accurateDelay && pos instanceof Hitboxc h){
                offset.set(h.deltaX(), h.deltaY()).scl(shoot.firstShotDelay / Time.delta);
            }

            targetPos.set(Predict.intercept(this, pos, offset.x, offset.y, bullet.speed <= 0.01f ? 99999999f : bullet.speed));

            if(targetPos.isZero()){
                targetPos.set(pos);
            }
        }

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void updateTile(){
            if(!validateTarget()) target = null;

            float warmupTarget = isShooting() && canConsume() ? 1f : 0f;
            if(linearWarmup){
                shootWarmup = Mathf.approachDelta(shootWarmup, warmupTarget, shootWarmupSpeed);
            }else{
                shootWarmup = Mathf.lerpDelta(shootWarmup, warmupTarget, shootWarmupSpeed);
            }

            wasShooting = false;

            //TODO do not lerp
            recoil = Mathf.lerpDelta(recoil, 0f, restitution);
            heat = Mathf.lerpDelta(heat, 0f, cooldown);

            unit.tile(this);
            unit.rotation(rotation);
            unit.team(team);
            recoilOffset.trns(rotation, -recoil);

            if(logicControlTime > 0){
                logicControlTime -= Time.delta;
            }

            if(heatRequirement > 0){
                heatReq = calculateHeat(sideHeat);
            }

            //turret always reloads regardless of whether it's targeting something
            updateReload();

            if(hasAmmo()){
                if(Float.isNaN(reloadCounter)) reloadCounter = 0;

                if(timer(timerTarget, targetInterval)){
                    findTarget();
                }

                if(validateTarget()){
                    boolean canShoot = true;

                    if(isControlled()){ //player behavior
                        targetPos.set(unit.aimX(), unit.aimY());
                        canShoot = unit.isShooting();
                    }else if(logicControlled()){ //logic behavior
                        canShoot = logicShooting;
                    }else{ //default AI behavior
                        targetPosition(target);

                        if(Float.isNaN(rotation)) rotation = 0;
                    }

                    float targetRot = angleTo(targetPos);

                    if(shouldTurn()){
                        turnToTarget(targetRot);
                    }

                    if(Angles.angleDist(rotation, targetRot) < shootCone && canShoot){
                        wasShooting = true;
                        updateShooting();
                    }
                }
            }

            if(coolant != null){
                updateCooling();
            }
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            if(coolant != null && liquids.currentAmount() <= 0.001f){
                Events.fire(Trigger.turretCool);
            }

            super.handleLiquid(source, liquid, amount);
        }

        protected boolean validateTarget(){
            return !Units.invalidateTarget(target, canHeal() ? Team.derelict : team, x, y) || isControlled() || logicControlled();
        }

        protected boolean canHeal(){
            return targetHealing && hasAmmo() && peekAmmo().collidesTeam && peekAmmo().heals();
        }

        protected void findTarget(){
            float range = range();

            if(targetAir && !targetGround){
                target = Units.bestEnemy(team, x, y, range, e -> !e.dead() && !e.isGrounded() && unitFilter.get(e), unitSort);
            }else{
                target = Units.bestTarget(team, x, y, range, e -> !e.dead() && unitFilter.get(e) && (e.isGrounded() || targetAir) && (!e.isGrounded() || targetGround), b -> targetGround && buildingFilter.get(b), unitSort);

                if(target == null && canHeal()){
                    target = Units.findAllyTile(team, x, y, range, b -> b.damaged() && b != this);
                }
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
                efficiency *= Math.min(heatReq / heatRequirement, maxHeatEfficiency);
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
            //used for "side-ammo" like gas in some turrets
            if(!canConsume()) return false;

            //skip first entry if it has less than the required amount of ammo
            if(ammo.size >= 2 && ammo.peek().amount < ammoPerShot && ammo.get(ammo.size - 2).amount >= ammoPerShot){
                totalAmmo -= ammo.pop().amount;
            }
            return ammo.size > 0 && ammo.peek().amount >= ammoPerShot;
        }

        public boolean charging(){
            return queuedBullets > 0 && shoot.firstShotDelay > 0;
        }

        protected void updateReload(){
            float multiplier = hasAmmo() ? peekAmmo().reloadMultiplier : 1f;
            reloadCounter += delta() * multiplier * baseReloadSpeed();

            //cap reload for visual reasons
            reloadCounter = Math.min(reloadCounter, reload);
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

            shoot.shoot(totalShots, (xOffset, yOffset, angle, delay, mover) -> {
                queuedBullets ++;
                if(delay > 0f){
                    Time.run(delay, () -> bullet(type, xOffset, yOffset, angle, mover));
                }else{
                    bullet(type, xOffset, yOffset, angle, mover);
                }
                totalShots ++;
            });

            if(consumeAmmoOnce){
                useAmmo();
            }
        }

        protected void bullet(BulletType type, float xOffset, float yOffset, float angleOffset, Mover mover){
            queuedBullets --;

            if(dead || (!consumeAmmoOnce && !hasAmmo())) return;

            float
            bulletX = x + Angles.trnsx(rotation - 90, shootX + xOffset, shootY + yOffset),
            bulletY = y + Angles.trnsy(rotation - 90, shootX + xOffset, shootY + yOffset),
            shootAngle = rotation + angleOffset + Mathf.range(inaccuracy);

            float lifeScl = type.scaleLife ? Mathf.clamp(Mathf.dst(bulletX, bulletY, targetPos.x, targetPos.y) / type.range, minRange / type.range, range() / type.range) : 1f;

            //TODO aimX / aimY for multi shot turrets?
            handleBullet(type.create(this, team, bulletX, bulletY, shootAngle, -1f, (1f - velocityRnd) + Mathf.random(velocityRnd), lifeScl, null, mover, targetPos.x, targetPos.y), xOffset, yOffset, angleOffset);

            (shootEffect == Fx.none ? type.shootEffect : shootEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
            (smokeEffect == Fx.none ? type.smokeEffect : smokeEffect).at(bulletX, bulletY, rotation + angleOffset, type.hitColor);
            shootSound.at(bulletX, bulletY, Mathf.random(soundPitchMin, soundPitchMax));

            ammoUseEffect.at(
                x - Angles.trnsx(rotation, ammoEjectBack),
                y - Angles.trnsy(rotation, ammoEjectBack),
                rotation * Mathf.sign(xOffset)
            );

            if(shootShake > 0){
                Effect.shake(shootShake, shootShake, this);
            }

            recoil = recoilAmount;
            heat = 1f;

            if(!consumeAmmoOnce){
                useAmmo();
            }
        }

        protected void handleBullet(@Nullable Bullet bullet, float offsetX, float offsetY, float angleOffset){

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
