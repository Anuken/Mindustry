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
    public float efficiencyMultiplier(Building build){
        var liq = getConsumed(build);
        return liq == null ? 0f : liq.flammability;
    }
}
