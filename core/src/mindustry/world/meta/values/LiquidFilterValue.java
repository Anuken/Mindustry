package mindustry.world.meta.values;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

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
        Seq<Liquid> list = new Seq<>();

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
