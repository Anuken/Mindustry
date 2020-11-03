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
import mindustry.type.*;
import mindustry.logic.*;
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
    public Color phaseColor = Pal.accent;

    public OverdriveProjector(String name){
        super(name);
        solid = true;
        update = true;
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
        //inner circle
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, baseColor);

        if(hasBoost && boosterUnlocked()){
            float expandProgress = (Time.time() % 90f <= 30f ? Time.time() % 90f : 30f) / 30f;
            float transparency = Time.time() %90f / 90f;

            //outside circle
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + phaseRangeBoost, phaseColor, 0.25f);

            //expanding circle
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + expandProgress * phaseRangeBoost, baseColor.cpy().lerp(phaseColor, transparency), 1f - transparency);

            //arrows
            float sin = Mathf.absin(Time.time(), 6f, 1f);
            for(int i = 0; i < 360; i += 60){
                Tmp.v1.trns(i, 0, range - sin);
                Tmp.v2.trns(i, 0, range + phaseRangeBoost);
                Drawf.arrow(x * tilesize + offset + Tmp.v1.x, y * tilesize + offset + Tmp.v1.y, x * tilesize + offset + Tmp.v2.x, y * tilesize + offset + Tmp.v2.y, phaseRangeBoost/2f + sin, 4f + sin, phaseColor);
            }
        }
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

    public class OverdriveBuild extends Building implements Ranged{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;

        @Override
        public float range(){
            return range;
        }

        @Override
        public void drawLight(){
            Drawf.light(team, x, y, 50f * efficiency(), baseColor, 0.7f * efficiency());
        }

        @Override
        public void updateTile(){
            heat = Mathf.lerpDelta(heat, consValid() ? 1f : 0f, 0.08f);
            charge += heat * Time.delta;

            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, Mathf.num(cons.optionalValid()), 0.1f);
            }

            if(timer(timerUse, useTime) && efficiency() > 0 && consValid()){
                consume();
            }

            if(charge >= reload){
                float realRange = range + phaseHeat * phaseRangeBoost;
                float realBoost = (speedBoost + phaseHeat * speedBoostPhase) * efficiency();

                charge = 0f;
                indexer.eachBlock(this, realRange, other -> true, other -> other.applyBoost(realBoost, reload + 1f));
            }
        }

        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            Drawf.dashCircle(x, y, realRange, baseColor.cpy().lerp(phaseColor, phaseHeat));
            
            indexer.eachBlock(this, realRange, other -> other.block().canOverdrive, other -> Drawf.selected(other, baseColor.cpy().lerp(phaseColor, phaseHeat).a(Mathf.absin(4f, 1f))));
            
            if(!cons().optionalValid() && hasBoost && boosterUnlocked()){
                float expandProgress = (Time.time() % 90f <= 30f ? Time.time() % 90f : 30f) / 30f;
                float transparency = Time.time() % 90f / 90f;
                
                //outside circle
                Drawf.dashCircle(x, y, range + phaseRangeBoost, phaseColor, 0.25f);

                //expanding circle
                Drawf.dashCircle(x, y, range + expandProgress * phaseRangeBoost, baseColor.cpy().lerp(phaseColor, transparency), 1f - transparency);

                //arrows
                float sin = Mathf.absin(Time.time(), 6f, 1f);
                for(int i = 0; i < 360; i += 60){
                    Tmp.v1.trns(i, 0, range - sin);
                    Tmp.v2.trns(i, 0, range + phaseRangeBoost);
                    Drawf.arrow(x + Tmp.v1.x, y + Tmp.v1.y, x + Tmp.v2.x, y + Tmp.v2.y, phaseRangeBoost/2f + sin, 4f + sin, phaseColor);
                }
            }
        }

        @Override
        public void draw(){
            super.draw();

            float f = 1f - (Time.time() / 100f) % 1f;

            Draw.color(baseColor, phaseColor, phaseHeat);
            Draw.alpha(heat * Mathf.absin(Time.time(), 10f, 1f) * 0.5f);
            Draw.rect(topRegion, x, y);
            Draw.alpha(1f);
            Lines.stroke((2f * f + 0.1f) * heat);
            Lines.square(x, y, Math.min(1f + (1f - f) * size * tilesize / 2f, size * tilesize/2f));

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
