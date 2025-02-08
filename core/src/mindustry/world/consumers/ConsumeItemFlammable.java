package mindustry.world.consumers;

import mindustry.type.*;

public class ConsumeItemFlammable extends ConsumeItemEfficiency{
    public float minFlammability;

    public ConsumeItemFlammable(float minFlammability){
        this.minFlammability = minFlammability;
        filter = item -> item.flammability >= this.minFlammability;
    }

    public ConsumeItemFlammable(){
        this(0.2f);
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.flammability;
    }
}
