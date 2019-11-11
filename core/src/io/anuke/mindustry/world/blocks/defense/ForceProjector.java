package io.anuke.mindustry.world.blocks.defense;

import io.anuke.arc.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.type.BaseEntity;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.consumers.*;
import io.anuke.mindustry.world.meta.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class ForceProjector extends Block{
    protected int timerUse = timers++;
    protected float phaseUseTime = 350f;

    protected float phaseRadiusBoost = 80f;
    protected float radius = 101.7f;
    protected float breakage = 550f;
    protected float cooldownNormal = 1.75f;
    protected float cooldownLiquid = 1.5f;
    protected float cooldownBrokenBase = 0.35f;
    protected float basePowerDraw = 0.2f;
    protected TextureRegion topRegion;

    private static Tile paramTile;
    private static ForceProjector paramBlock;
    private static ForceEntity paramEntity;
    private static Cons<AbsorbTrait> shieldConsumer = trait -> {
        if(trait.canBeAbsorbed() && trait.getTeam() != paramTile.getTeam() && paramBlock.isInsideHexagon(trait.getX(), trait.getY(), paramBlock.realRadius(paramEntity) * 2f, paramTile.drawx(), paramTile.drawy())){
            trait.absorb();
            Effects.effect(Fx.absorb, trait);
            paramEntity.hit = 1f;
            paramEntity.buildup += trait.getShieldDamage() * paramEntity.warmup;
        }
    };

    public ForceProjector(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        canOverdrive = false;
        hasLiquids = true;
        hasItems = true;
        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.1f)).boost().update(false);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void load(){
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.powerUse, basePowerDraw * 60f, StatUnit.powerSecond);
        stats.add(BlockStat.boostEffect, phaseRadiusBoost / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Draw.color(Pal.accent);
        Lines.stroke(1f);
        Lines.poly(x * tilesize, y * tilesize, 6, radius);
        Draw.color();
    }

    @Override
    public void update(Tile tile){
        ForceEntity entity = tile.entity();

        if(entity.shield == null){
            entity.shield = new ShieldEntity(tile);
            entity.shield.add();
        }

        boolean phaseValid = consumes.get(ConsumeType.item).valid(tile.entity);

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, Mathf.num(phaseValid), 0.1f);

        if(phaseValid && !entity.broken && entity.timer.get(timerUse, phaseUseTime)){
            entity.cons.trigger();
        }

        entity.radscl = Mathf.lerpDelta(entity.radscl, entity.broken ? 0f : entity.warmup, 0.05f);

        if(Mathf.chance(Time.delta() * entity.buildup / breakage * 0.1f)){
            Effects.effect(Fx.reactorsmoke, tile.drawx() + Mathf.range(tilesize / 2f), tile.drawy() + Mathf.range(tilesize / 2f));
        }

        entity.warmup = Mathf.lerpDelta(entity.warmup, entity.power.satisfaction, 0.1f);

/*
        if(entity.power.satisfaction < relativePowerDraw){
            entity.warmup = Mathf.lerpDelta(entity.warmup, 0f, 0.15f);
            entity.power.satisfaction = 0f;
            if(entity.warmup <= 0.09f){
                entity.broken = true;
            }
        }else{
            entity.warmup = Mathf.lerpDelta(entity.warmup, 1f, 0.1f);
        }*/

        if(entity.buildup > 0){
            float scale = !entity.broken ? cooldownNormal : cooldownBrokenBase;
            ConsumeLiquidFilter cons = consumes.get(ConsumeType.liquid);
            if(cons.valid(entity)){
                cons.update(entity);
                scale *= (cooldownLiquid * (1f + (entity.liquids.current().heatCapacity - 0.4f) * 0.9f));
            }

            entity.buildup -= Time.delta() * scale;
        }

        if(entity.broken && entity.buildup <= 0){
            entity.broken = false;
        }

        if(entity.buildup >= breakage && !entity.broken){
            entity.broken = true;
            entity.buildup = breakage;
            Effects.effect(Fx.shieldBreak, tile.drawx(), tile.drawy(), radius);
        }

        if(entity.hit > 0f){
            entity.hit -= 1f / 5f * Time.delta();
        }

        float realRadius = realRadius(entity);

        paramTile = tile;
        paramEntity = entity;
        paramBlock = this;
        bulletGroup.intersect(tile.drawx() - realRadius, tile.drawy() - realRadius, realRadius*2f, realRadius * 2f, shieldConsumer);
    }

    float realRadius(ForceEntity entity){
        return (radius + entity.phaseHeat * phaseRadiusBoost) * entity.radscl;
    }

    boolean isInsideHexagon(float x0, float y0, float d, float x, float y){
        float dx = Math.abs(x - x0) / d;
        float dy = Math.abs(y - y0) / d;
        float a = 0.25f * Mathf.sqrt3;
        return (dy <= a) && (a * dx + 0.25 * dy <= 0.5 * a);
    }

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        ForceEntity entity = tile.entity();

        if(entity.buildup <= 0f) return;
        Draw.alpha(entity.buildup / breakage * 0.75f);
        Draw.blend(Blending.additive);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.blend();
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
            super.write(stream);
            stream.writeBoolean(broken);
            stream.writeFloat(buildup);
            stream.writeFloat(radscl);
            stream.writeFloat(warmup);
            stream.writeFloat(phaseHeat);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
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
            return realRadius(entity) * 2f + 2f;
        }

        @Override
        public void draw(){
            Draw.color(Pal.accent);
            Fill.poly(x, y, 6, realRadius(entity));
            Draw.color();
        }

        public void drawOver(){
            if(entity.hit <= 0f) return;

            Draw.color(Color.white);
            Draw.alpha(entity.hit);
            Fill.poly(x, y, 6, realRadius(entity));
            Draw.color();
        }

        public void drawSimple(){
            if(realRadius(entity) < 0.5f) return;

            float rad = realRadius(entity);

            Draw.color(Pal.accent);
            Lines.stroke(1.5f);
            Draw.alpha(0.09f + 0.08f * entity.hit);
            Fill.poly(x, y, 6, rad);
            Draw.alpha(1f);
            Lines.poly(x, y, 6, rad);
            Draw.reset();
        }

        @Override
        public EntityGroup targetGroup(){
            return shieldGroup;
        }
    }
}
