package mindustry.world.meta.values;

import arc.scene.ui.layout.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

public class LiquidValue implements StatValue{
    private final Liquid liquid;
    private final float amount;
    private final boolean perSecond;
    private final float timePeriod;

    public LiquidValue(Liquid liquid, float amount, boolean perSecond){
        this.liquid = liquid;
        this.amount = amount;
        this.perSecond = perSecond;
        this.timePeriod = -1;
    }

    public LiquidValue(Liquid liquid, float amount, float timePeriod){
        this.liquid = liquid;
        this.amount = amount;
        this.perSecond = true;
        this.timePeriod = timePeriod;
    }

    @Override
    public void display(Table table){
        if(timePeriod > 0f){
            table.add(new LiquidDisplay(liquid, amount, timePeriod));
        }else{
            table.add(new LiquidDisplay(liquid, amount, perSecond));
        }
    }
}
