package mindustry.world.consumers;

import mindustry.gen.*;

public abstract class ConsumeLiquidBase extends Consume{
    /** amount used per frame */
    public float amount;

    public ConsumeLiquidBase(float amount){
        this.amount = amount;
    }

    public ConsumeLiquidBase(){}

    @Override
    public ConsumeType type(){
        return ConsumeType.liquid;
    }

    protected float use(Building entity){
        return Math.min(amount * entity.edelta(), entity.block.liquidCapacity);
    }
}
