package mindustry.world.blocks.defense;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MendProjector extends Block{
    public final int timerUse = timers++;
    public Color baseColor = Pal.mend;
    public Color phaseColor = Pal.phaseBoost;
    public @Load("@-top") TextureRegion topRegion;
    public float reload = 250f;
    public float range = 60f;
    public float healPercent = 12f;
    public float phaseBoost = 12f;
    public float phaseRangeBoost = 50f;
    public float useTime = 400f;

    public MendProjector(String name){
        super(name);
        solid = true;
        update = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.repairTime, (int)(100f / healPercent * reload / 60f), StatUnit.seconds);
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);

        stats.add(Stat.boostEffect, phaseRangeBoost / tilesize, StatUnit.blocks);
        stats.add(Stat.boostEffect, (phaseBoost + healPercent) / healPercent, StatUnit.timesSpeed);
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

    public class MendBuild extends Building implements Ranged{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;
        float smoothEfficiency;

        @Override
        public float range(){
            return range;
        }

        @Override
        public void updateTile(){
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency(), 0.08f);
            heat = Mathf.lerpDelta(heat, consValid() || cheating() ? 1f : 0f, 0.08f);
            charge += heat * delta();

            phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(cons.optionalValid()), 0.1f);

            if(cons.optionalValid() && timer(timerUse, useTime) && efficiency() > 0){
                consume();
            }

            if(charge >= reload){
                float realRange = range + phaseHeat * phaseRangeBoost;
                charge = 0f;

                indexer.eachBlock(this, realRange, other -> other.damaged(), other -> {
                    other.heal(other.maxHealth() * (healPercent + phaseHeat * phaseBoost) / 100f * efficiency());
                    Fx.healBlockFull.at(other.x, other.y, other.block.size, Tmp.c1.set(baseColor).lerp(phaseColor, phaseHeat));
                });
            }
        }

        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            if(boosterUnlocked()){
                Drawf.dashCircle(x, y, range + phaseRangeBoost, phaseColor, boostAlpha * (1f - Mathf.curve(phaseHeat, 0.9f, 1f)));
    
                indexer.eachBlock(this, range + phaseRangeBoost, other -> Mathf.dst(x, y, other.x, other.y) > range, other -> Drawf.selected(other, Tmp.c1.set(phaseColor).a(Mathf.absin(4f, boostAlpha * (1f - Mathf.curve(phaseHeat, 0.9f, 1f))))));
            }

            indexer.eachBlock(this, realRange, other -> true, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));

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
            Lines.stroke((2f * f + 0.2f) * heat);
            Lines.square(x, y, Math.min(1f + (1f - f) * size * tilesize / 2f, size * tilesize/2f));

            Draw.reset();
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, 50f * smoothEfficiency, baseColor, 0.7f * smoothEfficiency);
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
