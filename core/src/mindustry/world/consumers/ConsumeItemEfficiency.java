package mindustry.world.consumers;

import arc.func.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeItemEfficiency extends ConsumeItemFilter{

    public ConsumeItemEfficiency(Boolf<Item> item){
        super(item);
    }

    public ConsumeItemEfficiency(){
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(this::itemEfficiencyMultiplier, stats.timePeriod, filter));
    }
}
