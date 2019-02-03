package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;

public class ImpactGenerator extends TurbineGenerator{

    public ImpactGenerator(String name){
        super(name);
        minItemEfficiency = 0.5f;
        minLiquidEfficiency = 0.5f;
        randomlyExplode = false;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.explosiveness;
    }

    @Override
    protected float getLiquidEfficiency(Liquid liquid){
        return liquid.explosiveness;
    }
}
