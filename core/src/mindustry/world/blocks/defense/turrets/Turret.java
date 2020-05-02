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
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

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
    public int shots = 1;
    public float spread = 4f;
    public float recoilAmount = 1f;
    public float restitution = 0.02f;
    public float cooldown = 0.02f;
    public float rotatespeed = 5f; //in degrees per tick
    public float shootCone = 8f;
    public float shootShake = 0f;
    public float xRand = 0f;
    public boolean alternate = false;
    public boolean targetAir = true;
    public boolean targetGround = true;

    protected Vec2 tr = new Vec2();
    protected Vec2 tr2 = new Vec2();

    public @Load("block-$size") TextureRegion baseRegion;
    public @Load("@-heat") TextureRegion heatRegion;

    public Cons<TurretEntity> drawer = tile -> Draw.rect(region, tile.x() + tr2.x, tile.y() + tr2.y, tile.rotation - 90);
    public Cons<TurretEntity> heatDrawer = tile -> {
        if(tile.heat <= 0.00001f) return;
        Draw.color(heatColor, tile.heat);
        Draw.blend(Blending.additive);
        Draw.rect(heatRegion, tile.x() + tr2.x, tile.y() + tr2.y, tile.rotation - 90);
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
        stats.add(BlockStat.reload, 60f / reloadTime, StatUnit.none);
        stats.add(BlockStat.shots, shots, StatUnit.none);
        stats.add(BlockStat.targetsAir, targetAir);
        stats.add(BlockStat.targetsGround, targetGround);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("block-" + size), Core.atlas.find(name)};
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range, Pal.placing);
    }

    public static abstract class AmmoEntry{
        public int amount;

        public abstract BulletType type();
    }

    public class TurretEntity extends TileEntity{
        public Array<AmmoEntry> ammo = new Array<>();
        public int totalAmmo;
        public float reload, rotation = 90, recoil, heat;
        public int shotCounter;
        public Posc target;

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

            if(hasAmmo()){

                if(timer(timerTarget, targetInterval)){
                    findTarget();
                }

                if(validateTarget()){

                    BulletType type = peekAmmo();
                    float speed = type.speed;
                    if(speed < 0.1f) speed = 9999999f;

                    Vec2 result = Predict.intercept(this, target, speed);
                    if(result.isZero()){
                        result.set(target.getX(), target.getY());
                    }

                    float targetRot = result.sub(x, y).angle();

                    if(Float.isNaN(rotation)){
                        rotation = 0;
                    }

                    if(shouldTurn()){
                        turnToTarget(targetRot);
                    }

                    if(Angles.angleDist(rotation, targetRot) < shootCone){
                        updateShooting();
                    }
                }
            }
        }


        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range, team.color);
        }

        protected boolean validateTarget(){
            return !Units.invalidateTarget(target, team, x, y);
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
            if(tile.isEnemyCheat()) return peekAmmo();

            AmmoEntry entry = ammo.peek();
            entry.amount -= ammoPerShot;
            if(entry.amount == 0) ammo.pop();
            totalAmmo -= ammoPerShot;
            Time.run(reloadTime / 2f, () -> ejectEffects());
            return entry.type();
        }

        /**
         * Get the ammo type that will be returned if useAmmo is called.
         */
        public BulletType peekAmmo(){
            return ammo.peek().type();
        }

        /**
         * Returns whether the turret has ammo.
         */
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

            if(alternate){
                float i = (shotCounter % shots) - shots/2f + (((shots+1)%2) / 2f);

                tr.trns(rotation - 90, spread * i + Mathf.range(xRand), size * tilesize / 2);
                bullet(type, rotation + Mathf.range(inaccuracy));
            }else{
                tr.trns(rotation, size * tilesize / 2f, Mathf.range(xRand));

                for(int i = 0; i < shots; i++){
                    bullet(type, rotation + Mathf.range(inaccuracy + type.inaccuracy) + (i - shots / 2f) * spread);
                }
            }

            shotCounter++;

            effects();
            useAmmo();
        }

        protected void bullet(BulletType type, float angle){
            type.create(this, team, x + tr.x, y + tr.y, angle);
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
