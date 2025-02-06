package mindustry.world.consumers;

import arc.func.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

/** A power consumer that uses a dynamic amount of power. */
public class ConsumePowerDynamic extends ConsumePower{
    private final Floatf<Building> usage;
    private float displayedPowerUsage;

    public ConsumePowerDynamic(Floatf<Building> usage){
        super(0, 0, false);
        this.usage = usage;
    }

    public ConsumePowerDynamic(float displayed, Floatf<Building> usage){
        super(0, 0, false);
        this.displayedPowerUsage = displayed;
        this.usage = usage;
    }

    @Override
    public float requestedPower(Building entity){
        return usage.get(entity);
    }

    @Override
    public void display(Stats stats){
        if(displayedPowerUsage != 0f){
            stats.add(Stat.powerUse, displayedPowerUsage * 60f, StatUnit.powerSecond);
        }
    }
}
