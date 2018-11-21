package io.anuke.mindustry.world.blocks.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.traits.AbsorbTrait;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.ConsumeLiquidFilter;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityQuery;
import io.anuke.ucore.entities.impl.BaseEntity;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class ForceProjector extends Block {
    protected int timerUse = timers ++;
    protected float phaseUseTime = 350f;

    protected float phaseRadiusBoost = 80f;
    protected float radius = 100f;
    protected float breakage = 550f;
    protected float cooldownNormal = 1.75f;
    protected float cooldownLiquid = 1.5f;
    protected float cooldownBrokenBase = 0.35f;
    protected float powerDamage = 0.1f;
    protected TextureRegion topRegion;

    public ForceProjector(String name) {
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        canOverdrive = false;
        hasLiquids = true;
        hasItems = true;
        itemCapacity = 10;
        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.1f)).optional(true).update(false);
        consumes.powerBuffered(60f);
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
    public void setBars(){
        super.setBars();

        bars.add(new BlockBar(BarType.heat, true, tile -> tile.<ForceEntity>entity().buildup / breakage));
    }

    @Override
    public void update(Tile tile){
        ForceEntity entity = tile.entity();
        boolean cheat = tile.isEnemyCheat();

        if(entity.shield == null){
            entity.shield = new ShieldEntity(tile);
            entity.shield.add();
        }

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, (float)entity.items.get(consumes.item()) / itemCapacity, 0.1f);

        if(entity.cons.valid() && !entity.broken && entity.timer.get(timerUse, phaseUseTime) && entity.items.total() > 0){
            entity.items.remove(consumes.item(), 1);
        }

        entity.radscl = Mathf.lerpDelta(entity.radscl, entity.broken ? 0f : 1f, 0.05f);

        if(Mathf.chance(Timers.delta() * entity.buildup / breakage * 0.1f)){
            Effects.effect(BlockFx.reactorsmoke, tile.drawx() + Mathf.range(tilesize/2f), tile.drawy() + Mathf.range(tilesize/2f));
        }

        if(!entity.cons.valid() && !cheat){
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.15f);
            if(entity.warmup <= 0.09f){
                entity.broken = true;
            }
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.1f);
            // TODO Adapt power calculations to new power system
//            float powerUse = Math.min(powerDamage * entity.delta() * (1f + entity.buildup / breakage), powerCapacity);
//            entity.power.amount -= powerUse;
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
            Effects.effect(BlockFx.shieldBreak, tile.drawx(), tile.drawy(), radius);
        }

        if(entity.hit > 0f){
            entity.hit -= 1f/5f * Timers.delta();
        }

        float realRadius = realRadius(entity);

        if(!entity.broken){
            EntityQuery.getNearby(bulletGroup, tile.drawx(), tile.drawy(), realRadius*2f, bullet -> {
                AbsorbTrait trait = (AbsorbTrait)bullet;
                if(trait.canBeAbsorbed() && trait.getTeam() != tile.getTeam() && isInsideHexagon(trait.getX(), trait.getY(), realRadius * 2f, tile.drawx(), tile.drawy())){
                    trait.absorb();
                    Effects.effect(BulletFx.absorb, trait);
                    float hit = trait.getShieldDamage()*powerDamage;
                    entity.hit = 1f;
                    // TODO Adapt power calculations to new power system
//                    entity.power.amount -= Math.min(hit, entity.power.amount);
//
//                    if(entity.power.amount <= 0.0001f){
//                        entity.buildup += trait.getShieldDamage() * entity.warmup*2f;
//                    }
                    entity.buildup += trait.getShieldDamage() * entity.warmup;
                }
            });
        }
    }

    float realRadius(ForceEntity entity){
        return (radius+entity.phaseHeat*phaseRadiusBoost) * entity.radscl;
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
        Draw.alpha(entity.buildup / breakage * 0.75f);

        Graphics.setAdditiveBlending();
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Graphics.setNormalBlending();

        Draw.reset();
    }

    @Override
    public TileEntity newEntity(){
        return new ForceEntity();
    }

    class ForceEntity extends TileEntity{
        ShieldEntity shield;
        boolean broken = true;
        float buildup = 0f;
        float radscl = 0f;
        float hit;
        float warmup;
        float phaseHeat;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeBoolean(broken);
            stream.writeFloat(buildup);
            stream.writeFloat(radscl);
            stream.writeFloat(warmup);
            stream.writeFloat(phaseHeat);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            broken = stream.readBoolean();
            buildup = stream.readFloat();
            radscl = stream.readFloat();
            warmup = stream.readFloat();
            phaseHeat = stream.readFloat();
        }
    }

    public class ShieldEntity extends BaseEntity implements DrawTrait{
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
            return realRadius(entity)*2f+2f;
        }

        @Override
        public void draw(){
            Draw.color(Palette.accent);
            Fill.poly(x, y, 6, realRadius(entity));
            Draw.color();
        }

        public void drawOver(){
            if(entity.hit <= 0f) return;

            Draw.color(Color.WHITE);
            Draw.alpha(entity.hit);
            Fill.poly(x, y, 6, realRadius(entity));
            Draw.color();
        }

        @Override
        public EntityGroup targetGroup(){
            return shieldGroup;
        }
    }
}
