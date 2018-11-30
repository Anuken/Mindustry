package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;

public class LiquidHeatGenerator extends ItemLiquidGenerator{

    public LiquidHeatGenerator(String name){
        super(InputType.LiquidsOnly, name);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.basePowerGeneration);
        // Right now, Lava is the only thing that can be used.
        stats.add(BlockStat.basePowerGeneration, powerProduction * getLiquidEfficiency(Liquids.lava) / maxLiquidGenerate * 60f, StatUnit.powerSecond);
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return liquid.temperature - 0.5f;
    }
}
