package mindustry.world.blocks.defense.turrets;

import arc.Core;
import arc.audio.*;
import arc.struct.Array;
import arc.struct.EnumSet;
import arc.func.Cons2;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.*;
import mindustry.entities.Effects.Effect;
import mindustry.entities.type.Bullet;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.traits.TargetTrait;
import mindustry.entities.type.TileEntity;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.Block;
import mindustry.world.Tile;
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
    public float reload = 10f;
    public float inaccuracy = 0f;
    public int shots = 1;
    public float spread = 4f;
    public float recoil = 1f;
    public float restitution = 0.02f;
    public float cooldown = 0.02f;
    public float rotatespeed = 5f; //in degrees per tick
    public float shootCone = 8f;
    public float shootShake = 0f;
    public float xRand = 0f;
    public boolean targetAir = true;
    public boolean targetGround = true;

    protected Vec2 tr = new Vec2();
    protected Vec2 tr2 = new Vec2();

    public TextureRegion baseRegion, heatRegion;

    public Cons2<Tile, TurretEntity> drawer = (tile, entity) -> Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
    public Cons2<Tile, TurretEntity> heatDrawer = (tile, entity) -> {
        if(entity.heat <= 0.00001f) return;
        Draw.color(heatColor, entity.heat);
        Draw.blend(Blending.additive);
        Draw.rect(heatRegion, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
        Draw.blend();
        Draw.color();
    };

    public Turret(String name){
        super(name);
        priority = TargetPriority.turret;
        update = true;
        solid = true;
        layer = Layer.turret;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
        outlineIcon = true;
        entityType = TurretEntity::new;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void load(){
        super.load();

        region = Core.atlas.find(name);
        baseRegion = Core.atlas.find("block-" + size);
        heatRegion = Core.atlas.find(name + "-heat");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.shootRange, range / tilesize, StatUnit.blocks);
        stats.add(BlockStat.inaccuracy, (int)inaccuracy, StatUnit.degrees);
        stats.add(BlockStat.reload, 60f / reload, StatUnit.none);
        stats.add(BlockStat.shots, shots, StatUnit.none);
        stats.add(BlockStat.targetsAir, targetAir);
        stats.add(BlockStat.targetsGround, targetGround);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(baseRegion, tile.drawx(), tile.drawy());
        Draw.color();
    }

    @Override
    public void drawLayer(Tile tile){
        TurretEntity entity = tile.ent();

        tr2.trns(entity.rotation, -entity.recoil);

        drawer.get(tile, entity);

        if(heatRegion != Core.atlas.find("error")){
            heatDrawer.get(tile, entity);
        }
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find("block-" + size), Core.atlas.find(name)};
    }

    @Override
    public void drawSelect(Tile tile){
        Drawf.dashCircle(tile.drawx(), tile.drawy(), range, tile.getTeam().color);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Drawf.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range, Pal.placing);
    }

    @Override
    public void update(Tile tile){
        TurretEntity entity = tile.ent();

        if(!validateTarget(tile)) entity.target = null;

        entity.recoil = Mathf.lerpDelta(entity.recoil, 0f, restitution);
        entity.heat = Mathf.lerpDelta(entity.heat, 0f, cooldown);

        if(hasAmmo(tile)){

            if(entity.timer.get(timerTarget, targetInterval)){
                findTarget(tile);
            }

            if(validateTarget(tile)){

                BulletType type = peekAmmo(tile);
                float speed = type.speed;
                if(speed < 0.1f) speed = 9999999f;

                Vec2 result = Predict.intercept(entity, entity.target, speed);
                if(result.isZero()){
                    result.set(entity.target.getX(), entity.target.getY());
                }

                float targetRot = result.sub(tile.drawx(), tile.drawy()).angle();

                if(Float.isNaN(entity.rotation)){
                    entity.rotation = 0;
                }

                if(shouldTurn(tile)){
                    turnToTarget(tile, targetRot);
                }

                if(Angles.angleDist(entity.rotation, targetRot) < shootCone){
                    updateShooting(tile);
                }
            }
        }
    }

    protected boolean validateTarget(Tile tile){
        TurretEntity entity = tile.ent();
        return !Units.invalidateTarget(entity.target, tile.getTeam(), tile.drawx(), tile.drawy());
    }

    protected void findTarget(Tile tile){
        TurretEntity entity = tile.ent();

        if(targetAir && !targetGround){
            entity.target = Units.closestEnemy(tile.getTeam(), tile.drawx(), tile.drawy(), range, e -> !e.isDead() && e.isFlying());
        }else{
            entity.target = Units.closestTarget(tile.getTeam(), tile.drawx(), tile.drawy(), range, e -> !e.isDead() && (!e.isFlying() || targetAir) && (e.isFlying() || targetGround));
        }
    }

    protected void turnToTarget(Tile tile, float targetRot){
        TurretEntity entity = tile.ent();

        entity.rotation = Angles.moveToward(entity.rotation, targetRot, rotatespeed * entity.delta() * baseReloadSpeed(tile));
    }

    public boolean shouldTurn(Tile tile){
        return true;
    }

    /** Consume ammo and return a type. */
    public BulletType useAmmo(Tile tile){
        if(tile.isEnemyCheat()) return peekAmmo(tile);

        TurretEntity entity = tile.ent();
        AmmoEntry entry = entity.ammo.peek();
        entry.amount -= ammoPerShot;
        if(entry.amount == 0) entity.ammo.pop();
        entity.totalAmmo -= ammoPerShot;
        Time.run(reload / 2f, () -> ejectEffects(tile));
        return entry.type();
    }

    /**
     * Get the ammo type that will be returned if useAmmo is called.
     */
    public BulletType peekAmmo(Tile tile){
        TurretEntity entity = tile.ent();
        return entity.ammo.peek().type();
    }

    /**
     * Returns whether the turret has ammo.
     */
    public boolean hasAmmo(Tile tile){
        TurretEntity entity = tile.ent();
        return entity.ammo.size > 0 && entity.ammo.peek().amount >= ammoPerShot;
    }

    protected void updateShooting(Tile tile){
        TurretEntity entity = tile.ent();

        if(entity.reload >= reload){
            BulletType type = peekAmmo(tile);

            shoot(tile, type);

            entity.reload = 0f;
        }else{
            entity.reload += tile.entity.delta() * peekAmmo(tile).reloadMultiplier * baseReloadSpeed(tile);
        }
    }

    protected void shoot(Tile tile, BulletType type){
        TurretEntity entity = tile.ent();

        entity.recoil = recoil;
        entity.heat = 1f;

        tr.trns(entity.rotation, size * tilesize / 2f, Mathf.range(xRand));

        for(int i = 0; i < shots; i++){
            bullet(tile, type, entity.rotation + Mathf.range(inaccuracy + type.inaccuracy) + (i - shots / 2) * spread);
        }

        effects(tile);
        useAmmo(tile);
    }

    protected void bullet(Tile tile, BulletType type, float angle){
        Bullet.create(type, tile.entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y, angle);
    }

    protected void effects(Tile tile){
        Effect shootEffect = this.shootEffect == Fx.none ? peekAmmo(tile).shootEffect : this.shootEffect;
        Effect smokeEffect = this.smokeEffect == Fx.none ? peekAmmo(tile).smokeEffect : this.smokeEffect;

        TurretEntity entity = tile.ent();

        Effects.effect(shootEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
        Effects.effect(smokeEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
        shootSound.at(tile, Mathf.random(0.9f, 1.1f));

        if(shootShake > 0){
            Effects.shake(shootShake, shootShake, tile.entity);
        }

        entity.recoil = recoil;
    }

    protected void ejectEffects(Tile tile){
        if(!isTurret(tile)) return;
        TurretEntity entity = tile.ent();

        Effects.effect(ammoUseEffect, tile.drawx() - Angles.trnsx(entity.rotation, ammoEjectBack),
        tile.drawy() - Angles.trnsy(entity.rotation, ammoEjectBack), entity.rotation);
    }

    protected float baseReloadSpeed(Tile tile){
        return 1f;
    }

    protected boolean isTurret(Tile tile){
        return (tile.entity instanceof TurretEntity);
    }

    public static abstract class AmmoEntry{
        public int amount;

        public abstract BulletType type();
    }

    public static class TurretEntity extends TileEntity{
        public Array<AmmoEntry> ammo = new Array<>();
        public int totalAmmo;
        public float reload;
        public float rotation = 90;
        public float recoil = 0f;
        public float heat;
        public int shots;
        public TargetTrait target;
    }
}
