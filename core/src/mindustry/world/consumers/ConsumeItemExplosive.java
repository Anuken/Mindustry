package mindustry.world.consumers;

import mindustry.gen.*;

public class ConsumeItemExplosive extends ConsumeItemFilter{

    public ConsumeItemExplosive(float minExplosiveness){
        super(item -> item.explosiveness >= minExplosiveness);
    }

    public ConsumeItemExplosive(){
        this(0.2f);
    }

    @Override
    public float efficiencyMultiplier(Building build){
        var item = getConsumed(build);
        return item == null ? 0f : item.explosiveness;
    }
}
