package mindustry.world.blocks.power;

import arc.Core;
import arc.struct.EnumSet;
import arc.util.Strings;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Bar;
import mindustry.world.Tile;
import mindustry.world.meta.*;

import java.io.*;

public class PowerGenerator extends PowerDistributor{
    /** The amount of power produced per tick in case of an efficiency of 1.0, which represents 100%. */
    public float powerProduction;
    public BlockStat generationType = BlockStat.basePowerGeneration;

    public PowerGenerator(String name){
        super(name);
        sync = true;
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.producer);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(hasPower && outputsPower && !consumes.hasPower()){
            bars.add("power", entity -> new Bar(() ->
            Core.bundle.format("bar.poweroutput",
            Strings.fixed(block().getPowerProduction(tile()) * 60 * timeScale(), 1)),
            () -> Pal.powerBar,
            () -> ((GeneratorEntity)entity).productionEfficiency));
        }
    }

    @Override
    public float getPowerProduction(){
        return powerProduction * tile.<GeneratorEntity>ent().productionEfficiency;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class GeneratorEntity extends TileEntity{
        public float generateTime;
        /** The efficiency of the producer. An efficiency of 1.0 means 100% */
        public float productionEfficiency = 0.0f;

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(productionEfficiency);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            productionEfficiency = read.f();
        }
    }
}
