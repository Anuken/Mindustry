package io.anuke.mindustry.ui;

import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;

import java.text.NumberFormat;
import java.util.Locale;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.data;

public class ItemsDisplay extends Table{
    private static final NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());

    public ItemsDisplay(){
        rebuild();
    }

    public void rebuild(){
        clear();
        top().left();

        table("flat", t -> {
            t.margin(4);
            ObjectIntMap<Item> items = data.items();
            for(Item item : content.items()){
                if(item.type == ItemType.material && data.isUnlocked(item)){
                    t.label(() -> format.format(items.get(item, 0))).left();
                    t.addImage(item.region).size(8*3).padLeft(4).padRight(4);
                    t.add(item.localizedName()).color(Color.LIGHT_GRAY).left();
                    t.row();
                }
            }
        });
    }
}
