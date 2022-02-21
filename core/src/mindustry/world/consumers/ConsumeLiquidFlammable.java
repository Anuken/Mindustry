package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeLiquidFlammable extends ConsumeLiquidFilter{

    public ConsumeLiquidFlammable(float minFlammability, float amount){
        super(item -> item.flammability >= minFlammability, amount);
    }

    public ConsumeLiquidFlammable(float amount){
        this(0.2f, amount);
    }

    @Override
    public float getEfficiency(Building build){
        var item = getConsumed(build);
        return item == null ? 0f : item.flammability;
    }
}
