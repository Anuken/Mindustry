package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;

public class SingleTypeGenerator extends ItemLiquidGenerator{

    public SingleTypeGenerator(boolean hasItems, boolean hasLiquids, String name){
        super(hasItems, hasLiquids, name);
    }

    @Override
    protected float getItemEfficiency(Item item){
        return 1f;
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return 0f;
    }
}
