package io.anuke.mindustry.ui;

import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.modules.ItemModule;

public class ItemImage extends Stack{

    private ItemModule module;
    private Item item;

    public ItemImage(TextureRegion region, int amount){
        Table t = new Table().left().bottom();
        t.add(amount + "").name("item-label");

        add(new Image(region));
        add(t);
    }

    public ItemImage(ItemModule module, Item item){
        Table t = new Table().left().bottom();
        t.add(module.get(item) + "").name("item-label");

        add(new Image(item.icon(Cicon.medium)));
        add(t);

        this.module = module;
        this.item = item;
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

    @Override
    public void draw() {

        if(module != null && item != null){
            Table t = new Table().left().bottom();
            t.add(module.get(item) + "").name("item-label");

            addChildAt(0, new Image(item.icon(Cicon.medium)));
            addChildAt(1, t);
        }

        super.draw();
    }
}
