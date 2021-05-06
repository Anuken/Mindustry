package mindustry.world.blocks.power;

import arc.func.*;
import mindustry.gen.*;
import mindustry.world.consumers.*;

/** A power consumer that uses a dynamic amount of power, and activates sometimes */
public class DynamicConsumePower extends ConsumePower{
    private final Floatf<Building> usage;
    private final Boolf<Building> consume;

    public DynamicConsumePower(Floatf<Building> usage, Boolf<Building> consume){
        super(0, 0, false);
        this.usage = usage;
        this.consume = consume;
    }

    @Override
    public float requestedPower(Building entity){
        return consume.get(entity) ? usage.get(entity) : 0f;
    }
}
