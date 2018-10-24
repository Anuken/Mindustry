package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.util.EnumSet;

public class PowerGenerator extends PowerDistributor{

    public PowerGenerator(String name){
        super(name);
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.producer);
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
    }
}
