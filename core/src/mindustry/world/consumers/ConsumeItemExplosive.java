package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeItemExplosive extends ConsumeItemFilter{
    public float minExplosiveness;

    public ConsumeItemExplosive(float minCharge){
        this.minExplosiveness = minCharge;
        filter = item -> item.explosiveness >= this.minExplosiveness;
    }

    public ConsumeItemExplosive(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(i -> i.explosiveness, stats.timePeriod, filter));
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.explosiveness;
    }
}
