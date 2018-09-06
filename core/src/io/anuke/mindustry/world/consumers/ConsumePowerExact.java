package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.world.Block;

public class ConsumePowerExact extends ConsumePower{

    public ConsumePowerExact(float use){
        super(use);
    }

    protected float use(Block block){
        return this.use;
    }
}
