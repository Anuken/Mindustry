package mindustry.world.blocks.power;

import arc.func.Boolf;
import mindustry.gen.*;
import mindustry.world.consumers.ConsumePower;

/** A power consumer that only activates sometimes. */
public class ConditionalConsumePower extends ConsumePower{
    private final Boolf<Tilec> consume;

    public ConditionalConsumePower(float usage, Boolf<Tilec> consume){
        super(usage, 0, false);
        this.consume = consume;
    }

    @Override
    public float requestedPower(Tilec entity){
        return consume.get(entity) ? usage : 0f;
    }
}
