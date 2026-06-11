package mindustry.world.blocks.heat;

import arc.math.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

public class HeatProducer extends GenericCrafter{
    public float heatOutput = 10f;
    public float warmupRate = 0.15f;

    public HeatProducer(String name){
        super(name);

        drawer = new DrawMulti(new DrawDefault(), new DrawHeatOutput());
        rotateDraw = false;
        rotate = true;
        canOverdrive = true;
        drawArrow = true;
        //it doesn't count as a standard crafter
        flags = EnumSet.of();
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.output, heatOutput, StatUnit.heatUnits);
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("heat", (HeatProducerBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat / entity.heatOutScaled));
    }

    public class HeatProducerBuild extends GenericCrafterBuild implements HeatBlock{
        public float heat;
        public float heatOutScaled = heatOutput;

        @Override
        public void updateTile(){
            super.updateTile();

            //heat approaches target at the same speed regardless of efficiency. HeatOutput is scaled smoothly just like heat
            heat = Mathf.approachDelta(heat, heatOutput * timeScale * efficiency, warmupRate * delta());
            heatOutScaled = Mathf.approachDelta(heatOutScaled, heatOutput * timeScale, warmupRate * delta());
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
        public void write(Writes write){
            super.write(write);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
        }
    }
}
