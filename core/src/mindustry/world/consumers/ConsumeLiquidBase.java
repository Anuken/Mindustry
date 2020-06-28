package mindustry.world.consumers;

import mindustry.gen.*;

public abstract class ConsumeLiquidBase extends Consume{
    /** amount used per frame */
    public final float amount;
    /**
     * How much time is taken to use this liquid, in ticks. Used only for visual purposes.
     * Example: a normal ConsumeLiquid with 10/s and a 10 second timePeriod would display as "100 seconds".
     * Without a time override, it would display as "10 liquid/second".
     * This is used for generic crafters.
     */
    public float timePeriod = 60;

    public ConsumeLiquidBase(float amount){
        this.amount = amount;
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.liquid;
    }

    protected float use(Building entity){
        return Math.min(amount * entity.delta(), entity.block().liquidCapacity);
    }
}
