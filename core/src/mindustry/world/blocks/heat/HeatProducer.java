package mindustry.world.blocks.heat;

import arc.math.*;
import arc.util.io.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.blocks.power.NuclearReactor.*;
import mindustry.world.blocks.production.*;
import mindustry.world.draw.*;

public class HeatProducer extends GenericCrafter{
    public float heatOutput = 10f;
    public float warmupRate = 0.15f;

    public HeatProducer(String name){
        super(name);

        drawer = new DrawHeatOutput();
        update = solid = rotate = true;
        canOverdrive = false;
    }

    @Override
    public void setStats(){
        super.setStats();
        //TODO heat prod stats
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("heat", (NuclearReactorBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.heat));
    }

    public class HeatProducerBuild extends GenericCrafterBuild implements HeatBlock{
        public float heat;

        @Override
        public void updateTile(){
            super.updateTile();

            //heat approaches target at the same speed regardless of efficiency
            heat = Mathf.approachDelta(heat, heatOutput * efficiency() * Mathf.num(consValid()), warmupRate * delta());
        }

        @Override
        public float heatFrac(){
            return heat / heatOutput;
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
