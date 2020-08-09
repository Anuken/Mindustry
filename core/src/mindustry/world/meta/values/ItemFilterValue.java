package mindustry.world.meta.values;

import arc.struct.Seq;
import arc.func.Boolf;
import arc.scene.ui.layout.Table;
import mindustry.type.Item;
import mindustry.ui.ItemDisplay;
import mindustry.world.meta.StatValue;

import static mindustry.Vars.content;

public class ItemFilterValue implements StatValue{
    private final Boolf<Item> filter;

    public ItemFilterValue(Boolf<Item> filter){
        this.filter = filter;
    }

    @Override
    public void display(Table table){
        Seq<Item> list = content.items().select(filter);

        for(int i = 0; i < list.size; i++){
            Item item = list.get(i);

            table.add(new ItemDisplay(item)).padRight(5);

            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
