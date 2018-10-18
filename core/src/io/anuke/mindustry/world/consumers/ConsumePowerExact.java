package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;

public class ConsumePowerExact extends ConsumePower{

    public ConsumePowerExact(float use){
        super(use);
    }

    @Override
    protected float use(Block block, TileEntity entity){
        return this.use;
    }
}
