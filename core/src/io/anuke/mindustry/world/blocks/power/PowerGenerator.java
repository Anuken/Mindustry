package io.anuke.mindustry.world.blocks.power;

import io.anuke.ucore.util.EnumSet;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.mindustry.world.meta.BlockStat;

public class PowerGenerator extends PowerDistributor{
    public float powerProduction;
    public BlockStat generationType = BlockStat.basePowerGeneration;

    public PowerGenerator(String name){
        super(name);
        baseExplosiveness = 5f;
        flags = EnumSet.of(BlockFlag.producer);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(generationType, baseGeneration * 60f, StatUnit.powerSecond);
    }

    @Override
    public float getPowerProduction(Tile tile){
        return powerProduction * tile.<GeneratorEntity>entity().productionEfficiency * tile.entity.delta();
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
        public float productionEfficiency = 1;
    }
}
