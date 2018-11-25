package io.anuke.mindustry.ui;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.ucore.scene.ui.layout.Table;

/**An item image with text.*/
public class ItemDisplay extends Table{

    public ItemDisplay(Item item){
        this(item, 0);
    }

    public ItemDisplay(Item item, int amount){
        add(new ItemImage(new ItemStack(item, amount))).size(8*3);
        add(item.localizedName()).padLeft(4);
    }
}
