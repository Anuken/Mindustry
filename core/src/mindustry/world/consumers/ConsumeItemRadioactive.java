package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeItemRadioactive extends ConsumeItemFilter{

    public ConsumeItemRadioactive(float minRadioactivity){
        super(item -> item.radioactivity >= minRadioactivity);
    }

    public ConsumeItemRadioactive(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return item == null ? 0f : item.radioactivity;
    }
}
