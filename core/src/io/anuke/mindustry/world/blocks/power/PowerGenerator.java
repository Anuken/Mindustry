package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.util.EnumSet;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.meta.BlockStat;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PowerGenerator extends PowerDistributor{
    /** The amount of power produced per tick in case of an efficiency of 1.0, which currently represents 200%. */
    protected float powerProduction;
    public BlockStat generationType = BlockStat.basePowerGeneration;

    public PowerGenerator(String name){
        super(name);
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.producer);
    }

    @Override
    public void setStats(){
        super.setStats();
        // Divide power production by two since that is what is produced at an efficiency of 0.5, which currently represents 100%
        stats.add(generationType, powerProduction * 60.0f / 2.0f, StatUnit.powerSecond);
    }

    @Override
    public float getPowerProduction(Tile tile){
        // While 0.5 efficiency currently reflects 100%, we do not need to multiply by any factor since powerProduction states the
        // power which would be produced at 1.0 efficiency
        return powerProduction * tile.<GeneratorEntity>entity().productionEfficiency;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public TileEntity newEntity(){
        return new GeneratorEntity();
    }

    public static class GeneratorEntity extends TileEntity{
        public float generateTime;
        /** The efficiency of the producer. Currently, an efficiency of 0.5 means 100% */
        public float productionEfficiency = 0.0f;

        @Override
        public void write(DataOutput stream) throws IOException{
            stream.writeFloat(productionEfficiency);
        }

        @Override
        public void read(DataInput stream) throws IOException{
            productionEfficiency = stream.readFloat();
        }
    }
}
