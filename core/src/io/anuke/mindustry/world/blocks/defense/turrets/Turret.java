package io.anuke.mindustry.world.blocks.defense.turrets;

import io.anuke.arc.Core;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.EnumSet;
import io.anuke.arc.function.BiConsumer;
import io.anuke.arc.graphics.Blending;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.Effects.Effect;
import io.anuke.mindustry.entities.type.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.*;

import static io.anuke.mindustry.Vars.tilesize;

public abstract class Turret extends Block{
    protected static final int targetInterval = 20;

    protected final int timerTarget = timers++;

    protected Color heatColor = Pal.turretHeat;
    protected Effect shootEffect = Fx.none;
    protected Effect smokeEffect = Fx.none;
    protected Effect ammoUseEffect = Fx.none;
    protected Sound shootSound = Sounds.shoot;

    protected int ammoPerShot = 1;
    protected float ammoEjectBack = 1f;
    protected float range = 50f;
    protected float reload = 10f;
    protected float inaccuracy = 0f;
    protected int shots = 1;
    protected float spread = 4f;
    protected float recoil = 1f;
    protected float restitution = 0.02f;
    protected float cooldown = 0.02f;
    protected float rotatespeed = 5f; //in degrees per tick
    protected float shootCone = 8f;
    protected float shootShake = 0f;
    protected float xRand = 0f;
    protected boolean targetAir = true;
    protected boolean targetGround = true;

    protected Vector2 tr = new Vector2();
    protected Vector2 tr2 = new Vector2();

    protected TextureRegion baseRegion, heatRegion;

    protected BiConsumer<Tile, TurretEntity> drawer = (tile, entity) -> Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
    protected BiConsumer<Tile, TurretEntity> heatDrawer = (tile, entity) -> {
        if(entity.heat <= 0.00001f) return;
        Draw.color(heatColor, entity.heat);
        Draw.blend(Blending.additive);
        Draw.rect(heatRegion, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
        Draw.blend();
        Draw.color();
    };

    public Turret(String name){
        super(name);
        update = true;
        solid = true;
        layer = Layer.turret;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
        outlineIcon = true;
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
        TurretEntity entity = tile.entity();

        tr2.trns(entity.rotation, -entity.recoil);

        drawer.accept(tile, entity);

        if(heatRegion != Core.atlas.find("error")){
            heatDrawer.accept(tile, entity);
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
        TurretEntity entity = tile.entity();

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

                Vector2 result = Predict.intercept(entity, entity.target, speed);
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
        TurretEntity entity = tile.entity();
        return !Units.invalidateTarget(entity.target, tile.getTeam(), tile.drawx(), tile.drawy());
    }

    protected void findTarget(Tile tile){
        TurretEntity entity = tile.entity();

        if(targetAir && !targetGround){
            entity.target = Units.closestEnemy(tile.getTeam(), tile.drawx(), tile.drawy(), range, e -> !e.isDead() && e.isFlying());
        }else{
            entity.target = Units.closestTarget(tile.getTeam(), tile.drawx(), tile.drawy(), range, e -> !e.isDead() && (!e.isFlying() || targetAir) && (e.isFlying() || targetGround));
        }
    }

    protected void turnToTarget(Tile tile, float targetRot){
        TurretEntity entity = tile.entity();

        entity.rotation = Angles.moveToward(entity.rotation, targetRot, rotatespeed * entity.delta() * baseReloadSpeed(tile));
    }

    public boolean shouldTurn(Tile tile){
        return true;
    }

    /** Consume ammo and return a type. */
    public BulletType useAmmo(Tile tile){
        if(tile.isEnemyCheat()) return peekAmmo(tile);

        TurretEntity entity = tile.entity();
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
        TurretEntity entity = tile.entity();
        return entity.ammo.peek().type();
    }

    /**
     * Returns whether the turret has ammo.
     */
    public boolean hasAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return entity.ammo.size > 0 && entity.ammo.peek().amount >= ammoPerShot;
    }

    protected void updateShooting(Tile tile){
        TurretEntity entity = tile.entity();

        if(entity.reload >= reload){
            BulletType type = peekAmmo(tile);

            shoot(tile, type);

            entity.reload = 0f;
        }else{
            entity.reload += tile.entity.delta() * peekAmmo(tile).reloadMultiplier * baseReloadSpeed(tile);
        }
    }

    protected void shoot(Tile tile, BulletType type){
        TurretEntity entity = tile.entity();

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

        TurretEntity entity = tile.entity();

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
        TurretEntity entity = tile.entity();

        Effects.effect(ammoUseEffect, tile.drawx() - Angles.trnsx(entity.rotation, ammoEjectBack),
        tile.drawy() - Angles.trnsy(entity.rotation, ammoEjectBack), entity.rotation);
    }

    protected float baseReloadSpeed(Tile tile){
        return 1f;
    }

    protected boolean isTurret(Tile tile){
        return (tile.entity instanceof TurretEntity);
    }

    @Override
    public TileEntity newEntity(){
        return new TurretEntity();
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
