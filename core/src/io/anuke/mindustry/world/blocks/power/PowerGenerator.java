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
    /** The amount of power produced per tick. */
    protected float powerProduction;
    /** The maximum possible efficiency for this generator. Supply values larger than 1.0f if more than 100% is possible.
     *  This could be the case when e.g. an item with 100% flammability is the reference point, but a more effective liquid
     *  can be supplied as an alternative.
     */
    protected float maxEfficiency = 1.0f;
    public BlockStat generationType = BlockStat.basePowerGeneration;

    public PowerGenerator(String name){
        super(name);
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.producer);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(generationType, powerProduction * 60.0f, StatUnit.powerSecond);
    }

    @Override
    public float getPowerProduction(Tile tile){
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

    @Override
    public void setBars(){
        super.setBars();
        if(hasPower){
            bars.add(new BlockBar(BarType.power, true, tile -> tile.<GeneratorEntity>entity().productionEfficiency / maxEfficiency));
        }
    }

    public static class GeneratorEntity extends TileEntity{
        public float generateTime;
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
