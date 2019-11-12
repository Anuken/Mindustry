package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.func.Boolf;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.world.consumers.ConsumePower;

/** A power consumer that only activates sometimes. */
public class ConditionalConsumePower extends ConsumePower{
    private final Boolf<TileEntity> consume;

    public ConditionalConsumePower(float usage, Boolf<TileEntity> consume){
        super(usage, 0, false);
        this.consume = consume;
    }

    @Override
    public float requestedPower(TileEntity entity){
        return consume.get(entity) ? usage : 0f;
    }
}
