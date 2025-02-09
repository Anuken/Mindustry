package mindustry.world.consumers;

import mindustry.type.*;

/** For mods. I don't use this (yet). */
public class ConsumeItemCharged extends ConsumeItemEfficiency{
    public float minCharge;

    public ConsumeItemCharged(float minCharge){
        this.minCharge = minCharge;
        filter = item -> item.charge >= this.minCharge;
    }

    public ConsumeItemCharged(){
        this(0.2f);
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return item.charge;
    }
}
