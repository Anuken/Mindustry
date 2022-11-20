package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class OverdriveProjector extends Block{
    public final int timerUse = timers++;

    public float reload = 60f;
    public float range = 80f;
    public float speedBoost = 1.5f;
    public float speedBoostPhase = 0.75f;
    public float useTime = 400f;
    public float phaseRangeBoost = 20f;
    public boolean hasBoost = true;
    public Color baseColor = Color.valueOf("feb380");

    public DrawBlock drawer = new DrawMulti(
        new DrawDefault(),
        new DrawProjectorPulse(false, baseColor)
    );

    public OverdriveProjector(String name){
        super(name);
        solid = true;
        update = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
        canOverdrive = false;
        emitLight = true;
        lightRadius = 50f;
        envEnabled |= Env.space;
    }

    public void load() {
        super.load();

        drawer.load(this);
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, baseColor);

        indexer.eachBlock(player.team(), x * tilesize + offset, y * tilesize + offset, range, other -> other.block.canOverdrive, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));
    }

    @Override
    public void setStats(){
        stats.timePeriod = useTime;
        super.setStats();

        stats.add(Stat.speedIncrease, "+" + (int)(speedBoost * 100f - 100) + "%");
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.productionTime, useTime / 60f, StatUnit.seconds);

        if(hasBoost){
            stats.add(Stat.boostEffect, (range + phaseRangeBoost) / tilesize, StatUnit.blocks);
            stats.add(Stat.boostEffect, "+" + (int)((speedBoost + speedBoostPhase) * 100f - 100) + "%");
        }
    }
    
    @Override
    public void setBars(){
        super.setBars();
        addBar("boost", (OverdriveBuild entity) -> new Bar(() -> Core.bundle.format("bar.boost", Mathf.round(Math.max((entity.realBoost() * 100 - 100), 0))), () -> Pal.accent, () -> entity.realBoost() / (hasBoost ? speedBoost + speedBoostPhase : speedBoost)));
    }

    public class OverdriveBuild extends Building implements Ranged{
        public float warmup, progress = Mathf.random(reload), totalProgress, phaseHeat, smoothEfficiency;

        @Override
        public float range(){
            return range;
        }

        @Override
        public void updateTile(){
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.08f);
            warmup = Mathf.lerpDelta(warmup, efficiency > 0 ? 1f : 0f, 0.08f);
            progress += warmup * Time.delta;
            totalProgress += warmup * Time.delta;

            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, optionalEfficiency, 0.1f);
            }

            if(progress >= reload){
                float realRange = range + phaseHeat * phaseRangeBoost;

                progress = 0f;
                indexer.eachBlock(this, realRange, other -> other.block.canOverdrive, other -> other.applyBoost(realBoost(), reload + 1f));
            }

            if(timer(timerUse, useTime) && efficiency > 0){
                consume();
            }
        }

        public float realBoost(){
            return (speedBoost + phaseHeat * speedBoostPhase) * efficiency;
        }

        @Override
        public void drawSelect(){
            float realRange = range + phaseHeat * phaseRangeBoost;

            indexer.eachBlock(this, realRange, other -> other.block.canOverdrive, other -> Drawf.selected(other, Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f))));

            Drawf.dashCircle(x, y, realRange, baseColor);
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(warmup);
            write.f(phaseHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            warmup = read.f();
            phaseHeat = read.f();
        }
    }
}
