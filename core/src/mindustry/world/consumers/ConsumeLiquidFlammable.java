package mindustry.world.consumers;

import mindustry.gen.*;
import mindustry.world.meta.*;

public class ConsumeLiquidFlammable extends ConsumeLiquidFilter{
    public float minFlammability;

    public ConsumeLiquidFlammable(float minFlammability, float amount){
        this.amount = amount;
        this.minFlammability = minFlammability;
        this.filter = liquid -> liquid.flammability >= this.minFlammability;
    }

    public ConsumeLiquidFlammable(float amount){
        this(0.2f, amount);
    }

    public ConsumeLiquidFlammable(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var liq = getConsumed(build);
        return liq == null ? 0f : liq.flammability;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquidEffMultiplier(l -> l.flammability, filter));
    }
}
