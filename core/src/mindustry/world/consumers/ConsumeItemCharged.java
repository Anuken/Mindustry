package mindustry.world.consumers;

import mindustry.gen.*;

/** For mods. I don't use this (yet). */
public class ConsumeItemCharged extends ConsumeItemFilter{
    public float minCharge;

    public ConsumeItemCharged(float minCharge){
        this.minCharge = minCharge;
        filter = item -> item.charge >= this.minCharge;
    }

    public ConsumeItemCharged(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return item == null ? 0f : item.charge;
    }
}
