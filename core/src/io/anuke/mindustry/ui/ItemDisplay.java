package io.anuke.mindustry.ui;

import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;

/** An item image with text. */
public class ItemDisplay extends Table{

    public ItemDisplay(Item item){
        this(item, 0);
    }

    public ItemDisplay(Item item, int amount){
        add(new ItemImage(new ItemStack(item, amount))).size(8 * 4);
        add(item.localizedName()).padLeft(4);
    }
}
