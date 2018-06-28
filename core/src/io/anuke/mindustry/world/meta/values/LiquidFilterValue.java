package io.anuke.mindustry.world.meta.values;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.scene.ui.layout.Table;

public class LiquidFilterValue implements StatValue {
    private final Predicate<Liquid> filter;

    public LiquidFilterValue(Predicate<Liquid> filter) {
        this.filter = filter;
    }

    @Override
    public void display(Table table) {
        Array<Liquid> list = new Array<>();

        for(Liquid item : Liquid.all()){
            if(filter.test(item)) list.add(item);
        }

        for (int i = 0; i < list.size; i++) {
            Liquid item = list.get(i);
            table.addImage("liquid-icon").color(item.color).size(8*3).padRight(2).padLeft(2);
            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
