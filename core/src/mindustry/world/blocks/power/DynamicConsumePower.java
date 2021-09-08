package mindustry.world.blocks.power;

import arc.func.*;
import mindustry.gen.*;
import mindustry.world.consumers.*;

/** A power consumer that uses a dynamic amount of power. */
public class DynamicConsumePower extends ConsumePower{
    private final Floatf<Building> usage;

    public DynamicConsumePower(Floatf<Building> usage){
        super(0, 0, false);
        this.usage = usage;
    }

    @Override
    public float requestedPower(Building entity){
        return usage.get(entity);
    }
}
