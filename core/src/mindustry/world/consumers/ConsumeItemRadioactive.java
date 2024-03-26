package mindustry.world.consumers;

import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeItemRadioactive extends ConsumeItemFilter{
    public float minRadioactivity;

    public ConsumeItemRadioactive(float minRadioactivity){
        this.minRadioactivity = minRadioactivity;
        filter = item -> item.radioactivity >= this.minRadioactivity;
    }

    public ConsumeItemRadioactive(){
        this(0.2f);
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(i -> i.radioactivity, stats.timePeriod, filter));
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.radioactivity;
    }
}
