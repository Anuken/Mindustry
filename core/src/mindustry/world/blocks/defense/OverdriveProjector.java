package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class OverdriveProjector extends Block{
    public final int timerUse = timers++;

    public @Load("@-top") TextureRegion topRegion;
    public float reload = 60f;
    public float range = 80f;
    public float speedBoost = 1.5f;
    public float speedBoostPhase = 0.75f;
    public float useTime = 400f;
    public float phaseRangeBoost = 20f;
    public boolean hasBoost = true;
    public Color baseColor = Pal.overdrive;
    public Color phaseColor = Pal.phaseBoost;

    public OverdriveProjector(String name){
        super(name);
        solid = true;
        update = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
        canOverdrive = false;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        float realX = x * tilesize + offset;
        float realY = y * tilesize + offset;

        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(realX, realY, range, baseColor);

        if(boosterUnlocked()){
            Drawf.dashCircle(realX, realY, range + phaseRangeBoost, phaseColor, boostAlpha);

            indexer.eachBlock(player.team(), realX, realY, range + phaseRangeBoost, other -> Mathf.dst(realX, realY, other.x, other.y) > range, other -> Drawf.selected(other, Tmp.c1.set(phaseColor).a(Mathf.absin(4f, boostAlpha))));
        }

        indexer.eachBlock(player.team(), realX, realY, range, other -> true, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.speedIncrease, (int)(100f * speedBoost), StatUnit.percent);
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.productionTime, useTime / 60f, StatUnit.seconds);

        if(hasBoost){
            stats.add(Stat.boostEffect, phaseRangeBoost / tilesize, StatUnit.blocks);
            stats.add(Stat.boostEffect, (int)((speedBoost + speedBoostPhase) * 100f), StatUnit.percent);
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("boost", (OverdriveBuild entity) -> new Bar(() -> Core.bundle.format("bar.boost", (int)(entity.realBoost() * 100)), () -> Pal.accent, () -> entity.realBoost() / (hasBoost ? speedBoost + speedBoostPhase : speedBoost)));
    }

    public class OverdriveBuild extends Building implements Ranged{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;
        float smoothEfficiency;

        @Override
        public float range(){
            return range;
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, 50f * smoothEfficiency, baseColor, 0.7f * smoothEfficiency);
        }

        @Override
        public void updateTile(){
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency(), 0.08f);
            heat = Mathf.lerpDelta(heat, consValid() ? 1f : 0f, 0.08f);
            charge += heat * Time.delta;

            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(cons.optionalValid()), 0.1f);
            }

            if(charge >= reload){
                float realRange = range + phaseHeat * phaseRangeBoost;

                charge = 0f;
                indexer.eachBlock(this, realRange, other -> true, other -> other.applyBoost(realBoost(), reload + 1f));
            }

            if(timer(timerUse, useTime) && efficiency() > 0 && consValid()){
                consume();
            }
        }

        public float realBoost(){
            return consValid() ? (speedBoost + phaseHeat * speedBoostPhase) * efficiency() : 0f;
        }

        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            if(boosterUnlocked()){
                Drawf.dashCircle(x, y, range + phaseRangeBoost, phaseColor, boostAlpha * (1f - Mathf.curve(phaseHeat, 0.9f, 1f)));
    
                indexer.eachBlock(this, range + phaseRangeBoost, other -> Mathf.dst(x, y, other.x, other.y) > range, other -> Drawf.selected(other, Tmp.c1.set(phaseColor).a(Mathf.absin(4f, boostAlpha * (1f - Mathf.curve(phaseHeat, 0.9f, 1f))))));
            }

            indexer.eachBlock(this, realRange, other -> other.block.canOverdrive, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));

            Drawf.dashCircle(x, y, realRange, baseColor);
        }

        @Override
        public void draw(){
            super.draw();

            float f = 1f - (Time.time / 100f) % 1f;

            Draw.color(baseColor, phaseColor, phaseHeat);
            Draw.alpha(heat * Mathf.absin(Time.time, 10f, 1f) * 0.5f);
            Draw.rect(topRegion, x, y);
            Draw.alpha(1f);
            Lines.stroke((2f * f + 0.1f) * heat);

            float r = Math.max(0f, Mathf.clamp(2f - f * 2f) * size * tilesize / 2f - f - 0.2f), w = Mathf.clamp(0.5f - f) * size * tilesize;
            Lines.beginLine();
            for(int i = 0; i < 4; i++){
                Lines.linePoint(x + Geometry.d4(i).x * r + Geometry.d4(i).y * w, y + Geometry.d4(i).y * r - Geometry.d4(i).x * w);
                if(f < 0.5f) Lines.linePoint(x + Geometry.d4(i).x * r - Geometry.d4(i).y * w, y + Geometry.d4(i).y * r + Geometry.d4(i).x * w);
            }
            Lines.endLine(true);

            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            phaseHeat = read.f();
        }
    }
}
