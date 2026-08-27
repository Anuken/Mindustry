package mindustry.world.blocks.production;

import arc.*;
import arc.math.*;
import arc.util.io.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

/** A crafter that requires contact from heater blocks to craft. */
public class HeatCrafter extends GenericCrafter{
    /** Base heat requirement for 100% efficiency. */
    public float heatRequirement = 10f;
    public float warmupRate = 0.15f;
    /** Whether to scale required heat with timescale. */
    public boolean scaleHeat = true;
    /** After heat meets this requirement, excess heat will be scaled by this number. */
    public float overheatScale = 1f;
    /** Maximum possible efficiency after overheat. */
    public float maxEfficiency = 4f;

    public HeatCrafter(String name){
        super(name);
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("heat", (HeatCrafterBuild entity) ->
            new Bar(() ->
            Core.bundle.format("bar.heatpercent", (int)(entity.heat + 0.01f), (int)(entity.efficiencyScale() * 100 + 0.01f)),
            () -> Pal.lightOrange,
            () -> Mathf.clamp(entity.heat / entity.requiredHeat)));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.input, heatRequirement, StatUnit.heatUnits);
        stats.add(Stat.maxEfficiency, (int)(maxEfficiency * 100f), StatUnit.percent);
    }

    public class HeatCrafterBuild extends GenericCrafterBuild implements HeatConsumer{
        //TODO sideHeat could be smooth
        public float[] sideHeat = new float[4];
        public float heat = 0f, requiredHeat = heatRequirement;

        @Override
        public void updateTile(){
            requiredHeat = heatRequirement * timeScale;
            heat = calculateHeat(sideHeat);

            super.updateTile();
        }

        @Override
        public boolean shouldConsume(){
            return (requiredHeat <= 0f || heat > 0) && super.shouldConsume();
        }

        @Override
        public float heatRequirement(){
            return requiredHeat;
        }

        @Override
        public float[] sideHeat(){
            return sideHeat;
        }

        @Override
        public float warmupTarget(){
            return Mathf.clamp(heat / requiredHeat);
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return heat;
            return super.sense(sensor);
        }

        @Override
        public float efficiencyScale(){
            float over = Math.max(heat - requiredHeat, 0f);
            return Math.min(Mathf.clamp(heat / requiredHeat) + over / requiredHeat * overheatScale, maxEfficiency);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(requiredHeat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                heat = read.f();
                requiredHeat = read.f();
            }
        }
    }
}
