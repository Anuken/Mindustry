package mindustry.world.blocks.defense;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

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
    public @Load("@-top") TextureRegion topRegion;

    private static ForceProjectorEntity paramEntity;
    private static final Cons<Shielderc> shieldConsumer = trait -> {
        if(trait.team() != paramEntity.team && Intersector.isInsideHexagon(paramEntity.x, paramEntity.y, paramEntity.realRadius() * 2f, trait.x(), trait.y())){
            trait.absorb();
            Fx.absorb.at(trait);
            paramEntity.hit = 1f;
            paramEntity.buildup += trait.damage() * paramEntity.warmup;
        }
    };

    public ForceProjector(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        hasLiquids = true;
        hasItems = true;
        expanded = true;
        consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.1f)).boost().update(false);
    }

    @Override
    public boolean outputsItems(){
        return false;
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

        Draw.color(Pal.gray);
        Lines.stroke(3f);
        Lines.poly(x * tilesize, y * tilesize, 6, radius);
        Draw.color(player.team().color);
        Lines.stroke(1f);
        Lines.poly(x * tilesize, y * tilesize, 6, radius);
        Draw.color();
    }

    public class ForceProjectorEntity extends Building{
        boolean broken = true;
        float buildup = 0f;
        float radscl = 0f;
        float hit;
        float warmup;
        float phaseHeat;

        @Override
        public void updateTile(){
            boolean phaseValid = consumes.get(ConsumeType.item).valid(this);

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(phaseValid), 0.1f);

            if(phaseValid && !broken && timer(timerUse, phaseUseTime) && efficiency() > 0){
                consume();
            }

            radscl = Mathf.lerpDelta(radscl, broken ? 0f : warmup, 0.05f);

            if(Mathf.chanceDelta(buildup / breakage * 0.1f)){
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

                buildup -= delta() * scale;
            }

            if(broken && buildup <= 0){
                broken = false;
            }

            if(buildup >= breakage && !broken){
                broken = true;
                buildup = breakage;
                Fx.shieldBreak.at(x, y, radius, team.color);
            }

            if(hit > 0f){
                hit -= 1f / 5f * Time.delta;
            }

            float realRadius = realRadius();

            if(realRadius > 0 && !broken){
                paramEntity = this;
                Groups.bullet.intersect(x - realRadius, y - realRadius, realRadius * 2f, realRadius * 2f, shieldConsumer);
            }
        }

        float realRadius(){
            return (radius + phaseHeat * phaseRadiusBoost) * radscl;
        }

        @Override
        public void draw(){
            super.draw();

            if(buildup > 0f){
                Draw.alpha(buildup / breakage * 0.75f);
                Draw.blend(Blending.additive);
                Draw.rect(topRegion, x, y);
                Draw.blend();
                Draw.reset();
            }

            if(!broken){
                float radius = realRadius();

                Draw.z(Layer.shields);

                Draw.color(team.color, Color.white, Mathf.clamp(hit));

                if(Core.settings.getBool("animatedshields")){
                    Fill.poly(x, y, 6, radius);
                }else{
                    Lines.stroke(1.5f);
                    Draw.alpha(0.09f + Mathf.clamp(0.08f * hit));
                    Fill.poly(x, y, 6, radius);
                    Draw.alpha(1f);
                    Lines.poly(x, y, 6, radius);
                    Draw.reset();
                }
            }

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
}
