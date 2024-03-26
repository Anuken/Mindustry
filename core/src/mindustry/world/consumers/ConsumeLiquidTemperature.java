package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeLiquidTemperature extends ConsumeLiquidFilter{
    public float minTemperature;

    public ConsumeLiquidTemperature(float minTemperature, float amount){
        this.amount = amount;
        this.minTemperature = minTemperature;
        this.filter = liquid -> liquid.temperature >= this.minTemperature;
    }

    public ConsumeLiquidTemperature(float amount){
        this(0.2f, amount);
    }

    public ConsumeLiquidTemperature(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquidEffMultiplier(l -> l.temperature, amount * 60f, filter));
    }

    @Override
    public float liquidEfficiencyMultiplier(Liquid liquid){
        return liquid.temperature;
    }
}
