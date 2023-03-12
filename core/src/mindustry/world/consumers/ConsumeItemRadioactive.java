package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeItemRadioactive extends ConsumeItemFilter{
    public float minRadioactivity;

    public ConsumeItemRadioactive(float minRadioactivity){
        this.minRadioactivity = minRadioactivity;
        filter = item -> item.radioactivity >= this.minRadioactivity;
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
