package io.anuke.mindustry.world.consumers;

import io.anuke.mindustry.type.ItemStack;

public class ConsumeBoost extends ConsumeItems{

    public ConsumeBoost(ItemStack[] items){
        this.items = items;
        booster = true;
        optional = true;
    }

    @Override
    public ConsumeType type(){
        return ConsumeType.boost;
    }
}
