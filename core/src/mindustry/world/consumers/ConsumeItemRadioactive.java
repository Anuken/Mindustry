package mindustry.world.consumers;

import mindustry.type.*;

public class ConsumeItemRadioactive extends ConsumeItemEfficiency{
    public float minRadioactivity;

    public ConsumeItemRadioactive(float minRadioactivity){
        this.minRadioactivity = minRadioactivity;
        filter = item -> item.radioactivity >= this.minRadioactivity;
    }

    public ConsumeItemRadioactive(){
        this(0.2f);
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.radioactivity;
    }
}
