package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeLiquidViscosity extends ConsumeLiquidFilter{
    public float minViscosity;

    public ConsumeLiquidViscosity(float minViscosity, float amount){
        this.amount = amount;
        this.minViscosity = minViscosity;
        this.filter = liquid -> liquid.viscosity >= this.minViscosity;
    }

    public ConsumeLiquidViscosity(float amount){
        this(0.2f, amount);
    }

    public ConsumeLiquidViscosity(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.liquidEffMultiplier(l -> l.viscosity, amount * 60f, filter));
    }

    @Override
    public float liquidEfficiencyMultiplier(Liquid liquid){
        return liquid.viscosity;
    }
}
