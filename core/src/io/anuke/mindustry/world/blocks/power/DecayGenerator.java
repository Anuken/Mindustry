package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;

public class DecayGenerator extends ItemLiquidGenerator{

    public DecayGenerator(String name){
        super(InputType.ItemsOnly, name);
        hasItems = true;
        hasLiquids = false;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.radioactivity;
    }
}
