package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquidFilter;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.entities.impl.BaseEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Mathf;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class ForceProjector extends Block {
    protected float radius = 100f;
    protected float breakage = 500f;
    protected float cooldownNormal = 1.5f;
    protected float cooldownLiquid = 1.5f;
    protected float cooldownBrokenBase = 0.3f;
    protected float powerDamage = 0.1f;
    protected TextureRegion topRegion;

    public ForceProjector(String name) {
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        canOverdrive = false;
        hasLiquids = true;
        powerCapacity = 60f;
        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.1f)).optional(true).update(false);
    }

    @Override
    public void load(){
        super.load();
        topRegion = Draw.region(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerDamage, powerDamage, StatUnit.powerUnits);
    }

    @Override
    public void update(Tile tile){
        ForceEntity entity = tile.entity();

        if(entity.shield == null){
            entity.shield = new ShieldEntity(tile);
            entity.shield.add();
        }

        entity.radscl = Mathf.lerpDelta(entity.radscl, entity.broken ? 0f : 1f, 0.05f);

        if(Mathf.chance(Timers.delta() * entity.buildup / breakage * 0.1f)){
            Effects.effect(BlockFx.reactorsmoke, tile.drawx() + Mathf.range(tilesize/2f), tile.drawy() + Mathf.range(tilesize/2f));
        }

        if(!entity.cons.valid()){
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.1f);
            if(entity.warmup <= 0.09f){
                entity.broken = true;
            }
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.1f);
            float powerUse = Math.min(powerDamage * entity.delta() * (1f + entity.buildup / breakage), powerCapacity);
            entity.power.amount -= powerUse;
        }

        if(entity.buildup > 0){
            float scale = !entity.broken ? cooldownNormal : cooldownBrokenBase;
            if(consumes.get(ConsumeLiquidFilter.class).valid(this, entity)){
                consumes.get(ConsumeLiquidFilter.class).update(this, entity);
                scale *= (cooldownLiquid * (1f+(entity.liquids.current().heatCapacity-0.4f)*0.9f));
            }

            entity.buildup -= Timers.delta()*scale;
        }

        if(entity.broken && entity.buildup <= 0 && entity.warmup >= 0.9f){
            entity.broken = false;
        }

        if(entity.buildup >= breakage && !entity.broken){
            entity.broken = true;
            entity.buildup = breakage;
            Effects.effect(BlockFx.shieldBreak, tile.drawy(), tile.drawy(), radius);
        }

        if(entity.hit > 0f){
            entity.hit -= 1f/5f * Timers.delta();
        }

        if(!entity.broken){
            EntityPhysics.getNearby(bulletGroup, tile.drawx(), tile.drawy(), radius * entity.radscl*2f, bullet -> {
                if(bullet instanceof Bullet && ((Bullet) bullet).getTeam() != tile.getTeam() && isInsideHexagon(bullet.getX(), bullet.getY(), radius * 2f * entity.radscl, tile.drawx(), tile.drawy())){
                    ((Bullet) bullet).absorb();
                    Effects.effect(BulletFx.absorb, bullet);
                    float hit = ((Bullet) bullet).getDamage()*powerDamage;
                    entity.hit = 1f;
                    entity.power.amount -= Math.min(hit, entity.power.amount);
                    entity.buildup += ((Bullet) bullet).getDamage() * entity.warmup;
                }
            });
        }
    }

    boolean isInsideHexagon(float x0, float y0, float d, float x, float y) {
        float dx = Math.abs(x - x0)/d;
        float dy = Math.abs(y - y0)/d;
        float a = 0.25f * Mathf.sqrt3;
        return (dy <= a) && (a*dx + 0.25*dy <= 0.5*a);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        ForceEntity entity = tile.entity();

        if(entity.buildup <= 0f) return;
        Draw.alpha(entity.buildup / breakage * 0.75f/* * Mathf.absin(Timers.time(), 10f - (entity.buildup/breakage)*6f, 1f)*/);

        Graphics.setAdditiveBlending();
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Graphics.setNormalBlending();

        Draw.reset();
    }

    @Override
    public TileEntity getEntity(){
        return new ForceEntity();
    }

    class ForceEntity extends TileEntity{
        ShieldEntity shield;
        boolean broken = true;
        float buildup = 0f;
        float radscl = 0f;
        float hit;
        float warmup;

        @Override
        public void write(DataOutputStream stream) throws IOException{
            stream.writeBoolean(broken);
            stream.writeFloat(buildup);
            stream.writeFloat(radscl);
            stream.writeFloat(warmup);
        }

        @Override
        public void read(DataInputStream stream) throws IOException{
            broken = stream.readBoolean();
            buildup = stream.readFloat();
            radscl = stream.readFloat();
            warmup = stream.readFloat();
        }
    }

    public class ShieldEntity extends BaseEntity implements DrawTrait, SyncTrait{
        final ForceEntity entity;

        public ShieldEntity(Tile tile){
            this.entity = tile.entity();
            set(tile.drawx(), tile.drawy());
        }

        @Override
        public void update(){
            if(entity.isDead() || !entity.isAdded()){
                remove();
            }
        }

        @Override
        public float drawSize(){
            return radius*2f+2f*entity.radscl;
        }

        @Override
        public void draw(){
            Draw.color(Palette.accent);
            Fill.polyTri(x, y, 6, radius*entity.radscl);
            Draw.color();
        }

        public void drawOver(){
            if(entity.hit <= 0f) return;

            Draw.color(Color.WHITE);
            Draw.alpha(entity.hit);
            Fill.polyTri(x, y, 6, radius*entity.radscl);
            Draw.color();
        }

        @Override
        public EntityGroup targetGroup(){
            return shieldGroup;
        }

        @Override
        public boolean isSyncing(){
            return false;
        }

        @Override
        public void write(DataOutput data) throws IOException{}

        @Override
        public void read(DataInput data, long time) throws IOException{}
    }
}
