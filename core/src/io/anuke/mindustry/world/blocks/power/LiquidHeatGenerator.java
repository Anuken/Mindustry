package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.type.Liquid;

public class LiquidHeatGenerator extends LiquidGenerator {

    public LiquidHeatGenerator(String name) {
        super(name);
    }

    @Override
    protected float getEfficiency(Liquid liquid){
        return liquid.temperature-0.5f;
    }
}
