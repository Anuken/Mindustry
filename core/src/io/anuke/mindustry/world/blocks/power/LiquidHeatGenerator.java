package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

public class LiquidHeatGenerator extends LiquidGenerator{

    public LiquidHeatGenerator(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.basePowerGeneration, maxLiquidGenerate * powerPerLiquid * 60f * 0.5f, StatUnit.powerSecond);
    }

    @Override
    protected float getEfficiency(Liquid liquid){
        return liquid.temperature - 0.5f;
    }
}
