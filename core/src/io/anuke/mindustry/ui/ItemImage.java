package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.type.*;

public class ItemImage extends Stack{

    public ItemImage(TextureRegion region, int amount){
        Table t = new Table().left().bottom();
        t.add(amount + "").name("item-label");

        add(new Image(region));
        add(t);
    }

    public ItemImage(TextureRegion region){
        Table t = new Table().left().bottom();

        add(new Image(region));
        add(t);
    }

    public ItemImage(ItemStack stack){
        add(new Image(stack.item.icon(Cicon.medium)));

        if(stack.amount != 0){
            Table t = new Table().left().bottom();
            t.add(stack.amount + "").name("item-label");
            add(t);
        }
    }
}
