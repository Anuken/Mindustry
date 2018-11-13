package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.consumers.ConsumePower;

public class ConsumePowerBuffered extends ConsumePower{
    @Override
    public float getUse(Block block, TileEntity entity){
        return use * (1 - entity.power.satisfaction);
    }

    @Override
    public void addPower(float amount){
        entity.power.satisfaction = Math.min(entity.power.satisfaction + (amount / use), 1);
    }

    public void usePower(float amount){
        entity.power.satisfaction = Math.max(entity.power.satisfaction - (amount / use), 0);
    }
}
