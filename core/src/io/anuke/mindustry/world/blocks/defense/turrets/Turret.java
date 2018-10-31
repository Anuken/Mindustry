package io.anuke.mindustry.world.blocks.defense.turrets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.AmmoEntry;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.tilesize;

public abstract class Turret extends Block{
    protected static final int targetInterval = 20;

    protected final int timerTarget = timers++;

    protected Color heatColor = Palette.turretHeat;
    protected Effect shootEffect = Fx.none;
    protected Effect smokeEffect = Fx.none;
    protected Effect ammoUseEffect = Fx.none;

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

    protected Translator tr = new Translator();
    protected Translator tr2 = new Translator();

    protected TextureRegion baseRegion;
    protected TextureRegion heatRegion;
    protected TextureRegion baseTopRegion;

    protected BiConsumer<Tile, TurretEntity> drawer = (tile, entity) -> Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
    protected BiConsumer<Tile, TurretEntity> heatDrawer = (tile, entity) -> {
        if(entity.heat <= 0.00001f) return;
        Graphics.setAdditiveBlending();
        Draw.color(heatColor);
        Draw.alpha(entity.heat);
        Draw.rect(heatRegion, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
        Graphics.setNormalBlending();
    };

    public Turret(String name){
        super(name);
        update = true;
        solid = true;
        layer = Layer.turret;
        group = BlockGroup.turrets;
        turretIcon = true;
        flags = EnumSet.of(BlockFlag.turret);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void init(){
        super.init();
        viewRange = range;
    }

    @Override
    public void load(){
        super.load();

        baseRegion = Draw.region("block-" + size);
        baseTopRegion = Draw.region("block-" + size + "-top");
        heatRegion = Draw.region(name + "-heat");
    }

    @Override
    public void setStats(){
        super.setStats();
		/*
		if(ammo != null) stats.add("ammo", ammo);
		if(ammo != null) stats.add("ammocapacity", maxAmmo);
		if(ammo != null) stats.add("ammoitem", ammoMultiplier);*/

        stats.add(BlockStat.shootRange, range, StatUnit.blocks);
        stats.add(BlockStat.inaccuracy, (int) inaccuracy, StatUnit.degrees);
        stats.add(BlockStat.reload, 60f / reload, StatUnit.seconds);
        stats.add(BlockStat.shots, shots, StatUnit.none);
        stats.add(BlockStat.targetsAir, targetAir);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(baseRegion, tile.drawx(), tile.drawy());
        Draw.color(tile.getTeam().color, Color.WHITE, 0.45f);
        Draw.rect(baseTopRegion, tile.drawx(), tile.drawy());
        Draw.color();
    }

    @Override
    public void drawLayer(Tile tile){
        TurretEntity entity = tile.entity();

        tr2.trns(entity.rotation, -entity.recoil);

        drawer.accept(tile, entity);

        if(heatRegion != Draw.region("error")){
            heatDrawer.accept(tile, entity);
        }

        Draw.color();
    }

    @Override
    public TextureRegion[] getBlockIcon(){
        if(blockIcon == null){
            blockIcon = new TextureRegion[]{Draw.region("block-icon-" + name)};
        }
        return blockIcon;
    }

    @Override
    public TextureRegion[] getCompactIcon(){
        if(compactIcon == null){
            compactIcon = new TextureRegion[]{iconRegion(Draw.region("block-icon-" + name))};
        }
        return compactIcon;
    }

    @Override
    public void drawSelect(Tile tile){
        Draw.color(tile.getTeam().color);
        Lines.dashCircle(tile.drawx(), tile.drawy(), range);
        Draw.reset();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        Draw.color(Palette.placing);
        Lines.stroke(1f);
        Lines.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range);
    }

    @Override
    public void update(Tile tile){
        TurretEntity entity = tile.entity();

        if(entity.target != null && entity.target.isDead())
            entity.target = null;

        entity.recoil = Mathf.lerpDelta(entity.recoil, 0f, restitution);
        entity.heat = Mathf.lerpDelta(entity.heat, 0f, cooldown);

        if(hasAmmo(tile)){

            if(entity.timer.get(timerTarget, targetInterval)){
                findTarget(tile);
            }

            if(validateTarget(tile)){

                AmmoType type = peekAmmo(tile);
                float speed = type.bullet.speed;
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

        entity.target = Units.getClosestTarget(tile.getTeam(),
                tile.drawx(), tile.drawy(), range, e -> !e.isDead() && (!e.isFlying() || targetAir));
    }

    protected void turnToTarget(Tile tile, float targetRot){
        TurretEntity entity = tile.entity();

        entity.rotation = Angles.moveToward(entity.rotation, targetRot, rotatespeed * entity.delta());
    }

    public boolean shouldTurn(Tile tile){
        return true;
    }

    /**Consume ammo and return a type.*/
    public AmmoType useAmmo(Tile tile){
        if(tile.isEnemyCheat()) return peekAmmo(tile);

        TurretEntity entity = tile.entity();
        AmmoEntry entry = entity.ammo.peek();
        entry.amount -= ammoPerShot;
        if(entry.amount == 0) entity.ammo.pop();
        entity.totalAmmo -= ammoPerShot;
        Timers.run(reload / 2f, () -> ejectEffects(tile));
        return entry.type;
    }

    /**
     * Get the ammo type that will be returned if useAmmo is called.
     */
    public AmmoType peekAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return entity.ammo.peek().type;
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
            AmmoType type = peekAmmo(tile);

            shoot(tile, type);

            entity.reload = 0f;
        }else{
            entity.reload += tile.entity.delta() * peekAmmo(tile).reloadMultiplier;
        }
    }

    protected void shoot(Tile tile, AmmoType ammo){
        TurretEntity entity = tile.entity();

        entity.recoil = recoil;
        entity.heat = 1f;

        AmmoType type = peekAmmo(tile);

        tr.trns(entity.rotation, size * tilesize / 2, Mathf.range(xRand));

        for(int i = 0; i < shots; i++){
            bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy + type.inaccuracy) + (i-shots/2) * spread);
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

    protected boolean isTurret(Tile tile){
        return (tile.entity instanceof TurretEntity);
    }

    @Override
    public TileEntity newEntity(){
        return new TurretEntity();
    }

    public static class TurretEntity extends TileEntity{
        public Array<AmmoEntry> ammo = new ThreadArray<>();
        public int totalAmmo;
        public float reload;
        public float rotation = 90;
        public float recoil = 0f;
        public float heat;
        public int shots;
        public TargetTrait target;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeByte(ammo.size);
            for(AmmoEntry entry : ammo){
                stream.writeByte(entry.type.id);
                stream.writeShort(entry.amount);
            }
        }

        @Override
        public void read(DataInput stream) throws IOException{
            byte amount = stream.readByte();
            for(int i = 0; i < amount; i++){
                AmmoType type = content.getByID(ContentType.ammo, stream.readByte());
                short ta = stream.readShort();
                ammo.add(new AmmoEntry(type, ta));
                totalAmmo += ta;
            }
        }
    }
}
