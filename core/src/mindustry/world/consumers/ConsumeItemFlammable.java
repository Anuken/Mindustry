package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeItemFlammable extends ConsumeItemFilter{

    public ConsumeItemFlammable(float minFlammability){
        super(item -> item.flammability >= minFlammability);
    }

    public ConsumeItemFlammable(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        //TODO ugh
        if(build.consumeTriggerValid()) return 1f;
        var item = getConsumed(build);
        return item == null ? 0f : item.flammability;
    }
}
