package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.tilesize;

public abstract class Turret extends Block{
    public final int timerTarget = timers++;
    public int targetInterval = 20;

    public Color heatColor = Pal.turretHeat;
    public Effect shootEffect = Fx.none;
    public Effect smokeEffect = Fx.none;
    public Effect ammoUseEffect = Fx.none;
    public Sound shootSound = Sounds.shoot;

    public int ammoPerShot = 1;
    public float ammoEjectBack = 1f;
    public float range = 50f;
    public float reloadTime = 10f;
    public float inaccuracy = 0f;
    public float velocityInaccuracy = 0f;
    public int shots = 1;
    public float spread = 4f;
    public float recoilAmount = 1f;
    public float restitution = 0.02f;
    public float cooldown = 0.02f;
    public float rotatespeed = 5f; //in degrees per tick
    public float shootCone = 8f;
    public float shootShake = 0f;
    public float xRand = 0f;
    /** Currently used for artillery only. */
    public float minRange = 0f;
    public float burstSpacing = 0;
    public boolean alternate = false;
    public boolean targetAir = true;
    public boolean targetGround = true;
    public boolean acceptCoolant = true;
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    public float coolantMultiplier = 5f;
    /** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;

    protected Vec2 tr = new Vec2();
    protected Vec2 tr2 = new Vec2();

    public @Load("block-$size") TextureRegion baseRegion;
    public @Load("@-heat") TextureRegion heatRegion;

    public Cons<TurretEntity> drawer = tile -> Draw.rect(region, tile.x + tr2.x, tile.y + tr2.y, tile.rotation - 90);
    public Cons<TurretEntity> heatDrawer = tile -> {
        if(tile.heat <= 0.00001f) return;
        Draw.color(heatColor, tile.heat);
        Draw.blend(Blending.additive);
        Draw.rect(heatRegion, tile.x + tr2.x, tile.y + tr2.y, tile.rotation - 90);
        Draw.blend();
        Draw.color();
    };

    public Turret(String name){
        super(name);
        priority = TargetPriority.turret;
        update = true;
        solid = true;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
        outlineIcon = true;
        liquidCapacity = 20f;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(BlockStat.inaccuracy, (int)inaccuracy, StatUnit.degrees);
        stats.add(BlockStat.reload, 60f / reloadTime * shots, StatUnit.none);
        stats.add(BlockStat.targetsAir, targetAir);
        stats.add(BlockStat.targetsGround, targetGround);

        if(acceptCoolant){
            stats.add(BlockStat.booster, new BoosterListValue(reloadTime, consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount, coolantMultiplier, true, l -> consumes.liquidfilters.get(l.id)));
        }
    }

    @Override
    public void init(){
        if(acceptCoolant && !consumes.has(ConsumeType.liquid)){
            hasLiquids = true;
            consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.2f)).update(false).boost();
        }

        super.init();
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{baseRegion, region};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);
    }

    public static abstract class AmmoEntry{
        public int amount;

        public abstract BulletType type();
    }

    public class TurretEntity extends Building implements ControlBlock{
        public Seq<AmmoEntry> ammo = new Seq<>();
        public int totalAmmo;
        public float reload, rotation = 90, recoil, heat;
        public int shotCounter;
        public @Nullable Posc target;
        public Vec2 targetPos = new Vec2();
        public @NonNull BlockUnitc unit = Nulls.blockUnit;

        @Override
        public void created(){
            unit = (BlockUnitc)UnitTypes.block.create(team);
            unit.tile(this);
        }

        @Override
        public Unit unit(){
            return (Unit)unit;
        }

        @Override
        public void draw(){
            Draw.rect(baseRegion, x, y);
            Draw.color();

            Draw.z(Layer.turret);

            tr2.trns(rotation, -recoil);

            drawer.get(this);

            if(heatRegion != Core.atlas.find("error")){
                heatDrawer.get(this);
            }
        }

        @Override
        public void updateTile(){
            if(!validateTarget()) target = null;

            recoil = Mathf.lerpDelta(recoil, 0f, restitution);
            heat = Mathf.lerpDelta(heat, 0f, cooldown);

            unit.health(health);
            unit.rotation(rotation);
            unit.team(team);

            if(hasAmmo()){

                if(timer(timerTarget, targetInterval)){
                    findTarget();
                }

                if(validateTarget()){
                    boolean canShoot = true;

                    //player behavior
                    if(isControlled()){
                        targetPos.set(unit.aimX(), unit.aimY());
                        canShoot = unit.isShooting();
                    }else{ //default AI behavior
                        BulletType type = peekAmmo();
                        float speed = type.speed;
                        //slow bullets never intersect
                        if(speed < 0.1f) speed = 9999999f;

                        targetPos.set(Predict.intercept(this, target, speed));
                        if(targetPos.isZero()){
                            targetPos.set(target);
                        }

                        if(Float.isNaN(rotation)){
                            rotation = 0;
                        }
                    }

                    float targetRot = angleTo(targetPos);

                    if(shouldTurn()){
                        turnToTarget(targetRot);
                    }

                    if(Angles.angleDist(rotation, targetRot) < shootCone && canShoot){
                        updateShooting();
                    }
                }
            }

            if(acceptCoolant){
                updateCooling();
            }
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, team.color);
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            if(acceptCoolant && liquids.currentAmount() <= 0.001f){
                Events.fire(Trigger.turretCool);
            }

            super.handleLiquid(source, liquid, amount);
        }

        protected void updateCooling(){
            float maxUsed = consumes.<ConsumeLiquidBase>get(ConsumeType.liquid).amount;

            Liquid liquid = liquids.current();

            float used = Math.min(Math.min(liquids.get(liquid), maxUsed * Time.delta), Math.max(0, ((reloadTime - reload) / coolantMultiplier) / liquid.heatCapacity)) * baseReloadSpeed();
            reload += used * liquid.heatCapacity * coolantMultiplier;
            liquids.remove(liquid, used);

            if(Mathf.chance(0.06 * used)){
                coolEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
            }
        }

        protected boolean validateTarget(){
            return !Units.invalidateTarget(target, team, x, y) || isControlled();
        }

        protected void findTarget(){
            if(targetAir && !targetGround){
                target = Units.closestEnemy(team, x, y, range, e -> !e.dead() && !e.isGrounded());
            }else{
                target = Units.closestTarget(team, x, y, range, e -> !e.dead() && (e.isGrounded() || targetAir) && (!e.isGrounded() || targetGround));
            }
        }

        protected void turnToTarget(float targetRot){
            rotation = Angles.moveToward(rotation, targetRot, rotatespeed * delta() * baseReloadSpeed());
        }

        public boolean shouldTurn(){
            return true;
        }

        /** Consume ammo and return a type. */
        public BulletType useAmmo(){
            if(cheating()) return peekAmmo();

            AmmoEntry entry = ammo.peek();
            entry.amount -= ammoPerShot;
            if(entry.amount == 0) ammo.pop();
            totalAmmo -= ammoPerShot;
            Time.run(reloadTime / 2f, this::ejectEffects);
            return entry.type();
        }

        /** @return the ammo type that will be returned if useAmmo is called. */
        public BulletType peekAmmo(){
            return ammo.peek().type();
        }

        /** @return  whether the turret has ammo. */
        public boolean hasAmmo(){
            return ammo.size > 0 && ammo.peek().amount >= ammoPerShot;
        }

        protected void updateShooting(){
            if(reload >= reloadTime){
                BulletType type = peekAmmo();

                shoot(type);

                reload = 0f;
            }else{
                reload += delta() * peekAmmo().reloadMultiplier * baseReloadSpeed();
            }
        }

        protected void shoot(BulletType type){
            recoil = recoilAmount;
            heat = 1f;

            //when burst spacing is enabled, use the burst pattern
            if(burstSpacing > 0.0001f){
                for(int i = 0; i < shots; i++){
                    Time.run(burstSpacing * i, () -> {
                        if(!isValid() || !hasAmmo()) return;

                        recoil = recoilAmount;

                        tr.trns(rotation, size * tilesize / 2f, Mathf.range(xRand));
                        bullet(type, rotation + Mathf.range(inaccuracy));
                        effects();
                        useAmmo();
                    });
                }

            }else{
                //otherwise, use the normal shot pattern(s)

                if(alternate){
                    float i = (shotCounter % shots) - shots/2f + (((shots+1)%2) / 2f);

                    tr.trns(rotation - 90, spread * i + Mathf.range(xRand), size * tilesize / 2f);
                    bullet(type, rotation + Mathf.range(inaccuracy));
                }else{
                    tr.trns(rotation, size * tilesize / 2f, Mathf.range(xRand));

                    for(int i = 0; i < shots; i++){
                        bullet(type, rotation + Mathf.range(inaccuracy + type.inaccuracy) + (i - (int)(shots / 2f)) * spread);
                    }
                }

                shotCounter++;

                effects();
                useAmmo();
            }
        }

        protected void bullet(BulletType type, float angle){
            float lifeScl = type.scaleVelocity ? Mathf.clamp(Mathf.dst(x, y, targetPos.x, targetPos.y) / type.range(), minRange / type.range(), range / type.range()) : 1f;

            type.create(this, team, x + tr.x, y + tr.y, angle, 1f + Mathf.range(velocityInaccuracy), lifeScl);
        }

        protected void effects(){
            Effect fshootEffect = shootEffect == Fx.none ? peekAmmo().shootEffect : shootEffect;
            Effect fsmokeEffect = smokeEffect == Fx.none ? peekAmmo().smokeEffect : smokeEffect;

            fshootEffect.at(x + tr.x, y + tr.y, rotation);
            fsmokeEffect.at(x + tr.x, y + tr.y, rotation);
            shootSound.at(tile, Mathf.random(0.9f, 1.1f));

            if(shootShake > 0){
                Effects.shake(shootShake, shootShake, this);
            }

            recoil = recoilAmount;
        }

        protected void ejectEffects(){
            if(!isValid()) return;

            ammoUseEffect.at(x - Angles.trnsx(rotation, ammoEjectBack), y - Angles.trnsy(rotation, ammoEjectBack), rotation);
        }

        protected float baseReloadSpeed(){
            return 1f;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(reload);
            write.f(rotation);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision == 1){
                reload = read.f();
                rotation = read.f();
            }
        }

        @Override
        public byte version(){
            return 1;
        }
    }
}
