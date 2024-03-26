package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeLiquidHeatCapacity extends ConsumeLiquidFilter{
    public float minHeatCapacity;

    public ConsumeLiquidHeatCapacity(float minHeatCapacity, float amount){
        this.amount = amount;
        this.minHeatCapacity = minHeatCapacity;
        this.filter = liquid -> liquid.heatCapacity >= this.minHeatCapacity;
    }

    public ConsumeLiquidHeatCapacity(float amount){
        this(0.2f, amount);
    }

    public ConsumeLiquidHeatCapacity(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquidEffMultiplier(l -> l.heatCapacity, amount * 60f, filter));
    }

    @Override
    public float liquidEfficiencyMultiplier(Liquid liquid){
        return liquid.heatCapacity;
    }
}
