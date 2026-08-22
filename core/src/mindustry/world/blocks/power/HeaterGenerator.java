package mindustry.world.blocks.power;

import arc.math.*;
import arc.util.io.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

public class HeaterGenerator extends ConsumeGenerator{
    public float heatOutput = 10f;
    public float warmupRate = 0.15f;
    /** Whether to scale heat output with timescale. */
    public boolean scaleHeat = true;

    public HeaterGenerator(String name){
        super(name);

        drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        rotateDraw = false;
        rotate = true;
        canOverdrive = true;
        drawArrow = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.output, heatOutput, StatUnit.heatUnits);
    }

    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("heat", (HeaterGeneratorBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> Mathf.clamp(entity.heat / entity.heatOutScaled)));
    }

    public class HeaterGeneratorBuild extends ConsumeGeneratorBuild implements HeatBlock{
        public float heat;
        public float heatOutScaled = heatOutput;

        @Override
        public void updateTile(){
            super.updateTile();

            float approachHeat = HeatBlock.approachHeatOutput(heatOutput, scaleHeat, timeScale);
            heat = HeatBlock.approachHeat(heat, approachHeat * efficiency, warmupRate * delta());
            heatOutScaled = HeatBlock.approachHeat(heatOutScaled, approachHeat, warmupRate * delta());
        }

        @Override
        public float heatFrac(){
            return heat / heatOutScaled;
        }

        @Override
        public float heat(){
            return heat;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return heat;
            return super.sense(sensor);
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(heatOutScaled);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            if(revision >= 2) heatOutScaled = read.f();
        }
    }
}
