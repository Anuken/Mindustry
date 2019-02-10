package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Item.Icon;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Stack;
import io.anuke.arc.scene.ui.layout.Table;

public class ItemImage extends Stack{

    public ItemImage(TextureRegion region, Supplier<CharSequence> text){
        Table t = new Table().left().bottom();
        t.label(text).name("item-label");

        add(new Image(region));
        add(t);
    }

    public ItemImage(ItemStack stack){
        add(new Image(stack.item.icon(Icon.large)));

        if(stack.amount != 0){
            Table t = new Table().left().bottom();
            t.add(stack.amount + "").name("item-label");
            add(t);
        }
    }
}
