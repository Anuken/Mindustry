package io.anuke.mindustry.world.meta.values;

import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.scene.ui.layout.Table;

public class LiquidValue implements StatValue {
    private final Liquid liquid;

    public LiquidValue(Liquid liquid) {
        this.liquid = liquid;
    }

    @Override
    public void display(Table table) {
        table.addImage("liquid-icon").color(liquid.color).size(8*3);
    }
}
