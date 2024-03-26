package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeLiquidExplosive extends ConsumeLiquidFilter{
    public float minExplosiveness;

    public ConsumeLiquidExplosive(float minExplosiveness, float amount){
        this.amount = amount;
        this.minExplosiveness = minExplosiveness;
        this.filter = liquid -> liquid.heatCapacity >= this.minExplosiveness;
    }

    public ConsumeLiquidExplosive(float amount){
        this(0.2f, amount);
    }

    public ConsumeLiquidExplosive(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquidEffMultiplier(l -> l.explosiveness, amount * 60f, filter));
    }

    @Override
    public float liquidEfficiencyMultiplier(Liquid liquid){
        return liquid.explosiveness;
    }
}
