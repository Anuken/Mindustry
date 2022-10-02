package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeItemFlammable extends ConsumeItemFilter{
    public float minFlammability;

    public ConsumeItemFlammable(float minFlammability){
        this.minFlammability = minFlammability;
        filter = item -> item.flammability >= this.minFlammability;
    }

    public ConsumeItemFlammable(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return item == null ? 0f : item.flammability;
    }
}
