package mindustry.world.consumers;

import mindustry.gen.*;
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
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return item == null ? 0f : item.charge;
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(i -> i.charge, filter));
    }
}
