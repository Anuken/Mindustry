package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.LiquidDisplay;
import io.anuke.mindustry.world.meta.StatValue;

public class LiquidValue implements StatValue{
    private final Liquid liquid;
    private final float amount;
    private final boolean perSecond;

    public LiquidValue(Liquid liquid, float amount, boolean perSecond){
        this.liquid = liquid;
        this.amount = amount;
        this.perSecond = perSecond;
    }

    @Override
    public void display(Table table){
        table.add(new LiquidDisplay(liquid, amount, perSecond));
    }
}
