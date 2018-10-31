package io.anuke.mindustry.world.meta.values;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Cell;
import io.anuke.ucore.scene.ui.layout.Table;
import static io.anuke.mindustry.Vars.*;

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

            Cell<Image> imageCell = table.addImage(item.region);
            imageCell.size(8 * 3).padRight(2).padLeft(2);

            StatValue.addToolTip(imageCell.getElement(), item);

            if(i != list.size - 1){
                table.add("/");
            }
        }
    }
}
