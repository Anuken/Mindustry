package mindustry.world.consumers;

import arc.func.*;
import mindustry.gen.*;

/** A power consumer that only activates sometimes. */
public class ConsumePowerConditional extends ConsumePower{
    private final Boolf<Building> consume;

    public ConsumePowerConditional(float usage, Boolf<Building> consume){
        super(usage, 0, false);
        this.consume = consume;
    }

    @Override
    public float requestedPower(Building entity){
        return consume.get(entity) ? usage : 0f;
    }
}
