package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

/** For mods. I don't use this (yet). */
public class ConsumeItemCharged extends ConsumeItemFilter{
    public float minCharge;

    public ConsumeItemCharged(float minCharge){
        this.minCharge = minCharge;
        filter = item -> item.charge >= this.minCharge;
    }

    public ConsumeItemCharged(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(i -> i.charge, stats.timePeriod, filter));
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.charge;
    }
}
