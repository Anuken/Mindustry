package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeItemFlammable extends ConsumeItemFilter{
    public float minFlammability;

    public ConsumeItemFlammable(float minFlammability){
        this.minFlammability = minFlammability;
        filter = item -> item.flammability >= this.minFlammability;
    }

    public ConsumeItemFlammable(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(i -> i.flammability, stats.timePeriod, filter));
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.flammability;
    }
}
