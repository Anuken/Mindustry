package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
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
    public Color baseColor = Color.valueOf("feb380");
    public Color phaseColor = Color.valueOf("ffd59e");
    protected Vec2 close = new Vec2();
    protected Vec2 far = new Vec2();

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
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, baseColor);
        if(hasBoost && Core.settings.getBool("phasedrange")) {
            float sin = Mathf.absin(Time.time(), 6f, 1f);
            /*float close = range + phaseRangeBoost/2 - phaseRangeBoost/10;
            float far = range + phaseRangeBoost/2 + phaseRangeBoost/10;
            
            Drawf.arrow(x * tilesize + close, y * tilesize, x * tilesize + far, y * tilesize, size * tilesize + sin, 4f + sin, phaseColor);
            Drawf.arrow(x * tilesize, y * tilesize - close, x * tilesize, y * tilesize - far, size * tilesize + sin, 4f + sin, phaseColor);
            Drawf.arrow(x * tilesize - close, y * tilesize, x * tilesize - far, y * tilesize, size * tilesize + sin, 4f + sin, phaseColor);
            Drawf.arrow(x * tilesize, y * tilesize + close, x * tilesize, y * tilesize + far, size * tilesize + sin, 4f + sin, phaseColor);*/
            
            for(int i - 0; i < 360; i += 60){
              close.trns(i, 0, range + phaseRadiusBoost/2 - phaseRadiusBoost/10);
              far.trns(i, 0, range + phaseRadiusBoost/2 + phaseRadiusBoost/10);
              Drawf.arrow(x * tilesize + close.x, y * tilesize + close.y, x * tilesize + far.x, y * tilesize + far.y, size * tilesize + sin, 4f + sin, phaseColor);
            }
            
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range + phaseRangeBoost, phaseColor);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.speedIncrease, (int)(100f * speedBoost), StatUnit.percent);
        stats.add(BlockStat.range, range / tilesize, StatUnit.blocks);
        stats.add(BlockStat.productionTime, useTime / 60f, StatUnit.seconds);

        if(hasBoost){
            stats.add(BlockStat.boostEffect, phaseRangeBoost / tilesize, StatUnit.blocks);
            stats.add(BlockStat.boostEffect, (int)((speedBoost + speedBoostPhase) * 100f), StatUnit.percent);
        }
    }

    public class OverdriveBuild extends Building{
        float heat;
        float charge = Mathf.random(reload);
        float phaseHeat;

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

            if(!cons().optionalValid() || !hasBoost) {
                indexer.eachBlock(this, realRange, other -> other.block().canOverdrive, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));
                Drawf.dashCircle(x, y, realRange, baseColor);
            } else {
                indexer.eachBlock(this, realRange, other -> other.block().canOverdrive, other -> Drawf.selected(other, Tmp.c1.set(phaseColor).a(Mathf.absin(4f, 1f))));
                Drawf.dashCircle(x, y, realRange, phaseColor);
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
