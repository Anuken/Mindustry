package mindustry.world.meta.values;

import arc.scene.ui.layout.Table;
import mindustry.type.Liquid;
import mindustry.ui.LiquidDisplay;
import mindustry.world.meta.StatValue;

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
