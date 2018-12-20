package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.utils.Array;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.ItemDisplay;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.content;

public class ItemFilterValue implements StatValue{
    private final Predicate<Item> filter;

    public ItemFilterValue(Predicate<Item> filter){
        this.filter = filter;
    }

    @Override
    public void display(Table table){
        Array<Item> list = new Array<>();

        for(Item item : content.items()){
            if(filter.test(item)) list.add(item);
        }

        for(int i = 0; i < list.size; i++){
            Item item = list.get(i);

            table.add(new ItemDisplay(item)).padRight(5);

            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
