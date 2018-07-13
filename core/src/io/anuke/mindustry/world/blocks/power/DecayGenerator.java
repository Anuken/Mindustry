package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Item;

public class DecayGenerator extends ItemGenerator{

    public DecayGenerator(String name){
        super(name);
        hasItems = true;
        hasLiquids = false;
    }

    @Override
    protected float getItemEfficiency(Item item){
        return item.radioactivity;
    }
}
