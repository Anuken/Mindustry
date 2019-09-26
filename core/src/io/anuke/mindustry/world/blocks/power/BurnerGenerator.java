package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;

public class BurnerGenerator extends ItemLiquidGenerator{

    public BurnerGenerator(String name){
        super(true, false, name);
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return liquid.flammability;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.flammability;
    }
}
