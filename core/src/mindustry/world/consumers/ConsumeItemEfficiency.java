package mindustry.world.consumers;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class ConsumeItemEfficiency extends ConsumeItemFilter{
    /** This has no effect on the consumer itself, but is used for stat display. */
    public @Nullable ObjectFloatMap<Item> itemDurationMultipliers;

    public ConsumeItemEfficiency(Boolf<Item> item){
        super(item);
    }

    public ConsumeItemEfficiency(){
    }

    @Override
    public void display(Stats stats){
        stats.add(booster ? Stat.booster : Stat.input, StatValues.itemEffMultiplier(this::itemEfficiencyMultiplier, stats.timePeriod, filter, itemDurationMultipliers));
    }
}
