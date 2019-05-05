package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.function.Predicate;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.consumers.ConsumePower;

/** A power consumer that only activates sometimes. */
public class ConditionalConsumePower extends ConsumePower{
    private final Predicate<TileEntity> consume;

    public ConditionalConsumePower(float usage, Predicate<TileEntity> consume){
        super(usage, 0, false);
        this.consume = consume;
    }

    @Override
    public float requestedPower(TileEntity entity){
        return consume.test(entity) ? usage : 0f;
    }
}
