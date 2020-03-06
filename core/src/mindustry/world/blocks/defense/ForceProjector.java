package mindustry.world.blocks.defense;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.tilesize;

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
    private static ForceProjectorEntity paramEntity;
    private static Cons<Shielderc> shieldConsumer = trait -> {
        //TODO implement
        /*
        if(trait.team() != paramteam && Intersector.isInsideHexagon(trait.x(), trait.y(), paramEntity.realRadius() * 2f, paramx, paramy)){
            trait.absorb();
            Fx.absorb.at(trait);
            paramhit = 1f;
            parambuildup += trait.damage() * paramwarmup;
        }*/
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

    public class ForceProjectorEntity extends TileEntity{
        ShieldEntity shield;
        boolean broken = true;
        float buildup = 0f;
        float radscl = 0f;
        float hit;
        float warmup;
        float phaseHeat;

        @Override
        public void updateTile(){
            if(shield == null){
                //TODO implement
                //shield = new ShieldEntity(tile);
                //shield.add();
            }

            boolean phaseValid = consumes.get(ConsumeType.item).valid(tile.entity);

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid), 0.1f);

            if(phaseValid && !broken && timer(timerUse, phaseUseTime) && efficiency() > 0){
                consume();
            }

            radscl = Mathf.lerpDelta(radscl, broken ? 0f : warmup, 0.05f);

            if(Mathf.chance(Time.delta() * buildup / breakage * 0.1f)){
                Fx.reactorsmoke.at(x + Mathf.range(tilesize / 2f), y + Mathf.range(tilesize / 2f));
            }

            warmup = Mathf.lerpDelta(warmup, efficiency(), 0.1f);

            if(buildup > 0){
                float scale = !broken ? cooldownNormal : cooldownBrokenBase;
                ConsumeLiquidFilter cons = consumes.get(ConsumeType.liquid);
                if(cons.valid(this)){
                    cons.update(this);
                    scale *= (cooldownLiquid * (1f + (liquids.current().heatCapacity - 0.4f) * 0.9f));
                }

                buildup -= Time.delta() * scale;
            }

            if(broken && buildup <= 0){
                broken = false;
            }

            if(buildup >= breakage && !broken){
                broken = true;
                buildup = breakage;
                Fx.shieldBreak.at(x, y, radius);
            }

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta();
            }

            float realRadius = realRadius();

            paramTile = tile;
            paramEntity = this;
            paramBlock = ForceProjector.this;
            Groups.bullet.intersect(x - realRadius, y - realRadius, realRadius*2f, realRadius * 2f, shieldConsumer);
        }

        float realRadius(){
            return (radius + phaseHeat * phaseRadiusBoost) * radscl;
        }

        @Override
        public void draw(){
            super.draw();

            if(buildup <= 0f) return;
            Draw.alpha(buildup / breakage * 0.75f);
            Draw.blend(Blending.additive);
            Draw.rect(topRegion, x, y);
            Draw.blend();
            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(broken);
            write.f(buildup);
            write.f(radscl);
            write.f(warmup);
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            broken = read.bool();
            buildup = read.f();
            radscl = read.f();
            warmup = read.f();
            phaseHeat = read.f();
        }
    }

    //TODO fix
    class ShieldEntity{

    }
    /*
    //@EntityDef({Drawc.class})
    //class ShieldDef{}

    public class ShieldEntity extends BaseEntity implements DrawTrait{
        final ForceEntity entity;

        public ShieldEntity(){
            this.entity = tile.ent();
            set(x, y);
        }

        @Override
        public void update(){
            if(isDead() || !isAdded()){
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
            if(hit <= 0f) return;

            Draw.color(Color.white);
            Draw.alpha(hit);
            Fill.poly(x, y, 6, realRadius(entity));
            Draw.color();
        }

        public void drawSimple(){
            if(realRadius(entity) < 0.5f) return;

            float rad = realRadius(entity);

            Draw.color(Pal.accent);
            Lines.stroke(1.5f);
            Draw.alpha(0.09f + 0.08f * hit);
            Fill.poly(x, y, 6, rad);
            Draw.alpha(1f);
            Lines.poly(x, y, 6, rad);
            Draw.reset();
        }

        @Override
        public EntityGroup targetGroup(){
            return shieldGroup;
        }
    }*/
}
