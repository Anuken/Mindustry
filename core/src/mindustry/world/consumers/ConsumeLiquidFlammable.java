package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeLiquidFlammable extends ConsumeLiquidFilter{
    public float minFlammability;

    public ConsumeLiquidFlammable(float minFlammability){
        this.minFlammability = minFlammability;
        filter = liquid -> liquid.flammability >= this.minFlammability;
    }

    public ConsumeLiquidFlammable(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var liq = getConsumed(build);
        return liq == null ? 0f : liq.flammability;
    }
}
