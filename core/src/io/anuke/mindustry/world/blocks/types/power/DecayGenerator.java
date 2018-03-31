package io.anuke.mindustry.world.blocks.types.power;

import io.anuke.mindustry.resource.Item;

public class DecayGenerator extends BurnerGenerator {

    public DecayGenerator(String name) {
        super(name);
    }

    @Override
    protected float getItemEfficiency(Item item) {
        return item.radioactivity;
    }
}
