package mindustry.world.consumers;

import mindustry.gen.*;

public abstract class ConsumeLiquidBase extends Consume{
    /** amount used per frame */
    public final float amount;

    public ConsumeLiquidBase(float amount){
        this.amount = amount;
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.liquid;
    }

    protected float use(Building entity){
        return Math.min(amount * entity.edelta(), entity.block.liquidCapacity);
    }
}
