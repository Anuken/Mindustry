package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.collection.Array;
import io.anuke.arc.func.Boolf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.ui.LiquidDisplay;
import io.anuke.mindustry.world.meta.StatValue;

import static io.anuke.mindustry.Vars.content;

public class LiquidFilterValue implements StatValue{
    private final Boolf<Liquid> filter;
    private final float amount;
    private final boolean perSecond;

    public LiquidFilterValue(Boolf<Liquid> filter, float amount, boolean perSecond){
        this.filter = filter;
        this.amount = amount;
        this.perSecond = perSecond;
    }

    @Override
    public void display(Table table){
        Array<Liquid> list = new Array<>();

        for(Liquid item : content.liquids()){
            if(!item.isHidden() && filter.get(item)) list.add(item);
        }

        for(int i = 0; i < list.size; i++){
            table.add(new LiquidDisplay(list.get(i), amount, perSecond)).padRight(5);

            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
