package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;

public class ItemImage extends Stack{

    public ItemImage(TextureRegion region, Supplier<CharSequence> text){
        Table t = new Table().left().bottom();
        t.label(text);

        add(new Image(region));
        add(t);
    }

    public ItemImage(ItemStack stack){
        add(new Image(stack.item.region));

        if(stack.amount != 0){
            Table t = new Table().left().bottom();
            t.add(stack.amount + "");
            add(t);
        }
    }
}
