package mindustry.world.blocks.defense;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.traits.*;
import mindustry.entities.type.*;
import mindustry.entities.type.BaseEntity;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class ForceProjector extends Block{
    public final int timerUse = timers++;
    public float phaseUseTime = 350f;

    public float phaseRadiusBoost = 80f;
    public float radius = 101.7f;
    public float breakage = 550f;
    public float cooldownNormal = 1.75f;
    public float cooldownLiquid = 1.5f;
    public float cooldownBrokenBase = 0.35f;
    public float basePowerDraw = 0.2f;
    public TextureRegion topRegion;

    private static Tile paramTile;
    private static ForceProjector paramBlock;
    private static ForceEntity paramEntity;
    private static Cons<AbsorbTrait> shieldConsumer = trait -> {
        if(trait.canBeAbsorbed() && trait.getTeam() != paramTile.getTeam() && Intersector.isInsideHexagon(trait.getX(), trait.getY(), paramBlock.realRadius(paramEntity) * 2f, paramTile.drawx(), paramTile.drawy())){
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
        entityType = ForceEntity::new;
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
        ForceEntity entity = tile.ent();

        if(entity.shield == null){
            entity.shield = new ShieldEntity(tile);
            entity.shield.add();
        }

        boolean phaseValid = consumes.get(ConsumeType.item).valid(tile.entity);

        entity.phaseHeat = Mathf.lerpDelta(entity.phaseHeat, Mathf.num(phaseValid), 0.1f);

        if(phaseValid && !entity.broken && entity.timer.get(timerUse, phaseUseTime) && entity.efficiency() > 0){
            entity.cons.trigger();
        }

        entity.radscl = Mathf.lerpDelta(entity.radscl, entity.broken ? 0f : entity.warmup, 0.05f);

        if(Mathf.chance(Time.delta() * entity.buildup / breakage * 0.1f)){
            Effects.effect(Fx.reactorsmoke, tile.drawx() + Mathf.range(tilesize / 2f), tile.drawy() + Mathf.range(tilesize / 2f));
        }

        entity.warmup = Mathf.lerpDelta(entity.warmup, entity.efficiency(), 0.1f);

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

    @Override
    public void draw(Tile tile){
        super.draw(tile);

        ForceEntity entity = tile.ent();

        if(entity.buildup <= 0f) return;
        Draw.alpha(entity.buildup / breakage * 0.75f);
        Draw.blend(Blending.additive);
        Draw.rect(topRegion, tile.drawx(), tile.drawy());
        Draw.blend();
        Draw.reset();
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
            this.entity = tile.ent();
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
