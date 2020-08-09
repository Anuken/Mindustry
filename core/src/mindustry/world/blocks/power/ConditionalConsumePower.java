package mindustry.world.blocks.power;

import arc.func.Boolf;
import mindustry.gen.*;
import mindustry.world.consumers.ConsumePower;

/** A power consumer that only activates sometimes. */
public class ConditionalConsumePower extends ConsumePower{
    private final Boolf<Building> consume;

    public ConditionalConsumePower(float usage, Boolf<Building> consume){
        super(usage, 0, false);
        this.consume = consume;
    }

    @Override
    public float requestedPower(Building entity){
        return consume.get(entity) ? usage : 0f;
    }
}
