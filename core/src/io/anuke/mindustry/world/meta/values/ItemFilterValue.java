package io.anuke.mindustry.world.meta.values;

import io.anuke.arc.collection.Array;
import io.anuke.arc.func.Boolf;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.ui.ItemDisplay;
import io.anuke.mindustry.world.meta.StatValue;

import static io.anuke.mindustry.Vars.content;

public class ItemFilterValue implements StatValue{
    private final Boolf<Item> filter;

    public ItemFilterValue(Boolf<Item> filter){
        this.filter = filter;
    }

    @Override
    public void display(Table table){
        Array<Item> list = content.items().select(filter);

        for(int i = 0; i < list.size; i++){
            Item item = list.get(i);

            table.add(new ItemDisplay(item)).padRight(5);

            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
