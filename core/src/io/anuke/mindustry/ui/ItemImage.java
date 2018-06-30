package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;

public class ItemImage extends Stack {

    public ItemImage(TextureRegion region, Supplier<CharSequence> text) {
        Table t = new Table().left().bottom();

        t.label(text).color(Color.DARK_GRAY).padBottom(-22).get().setFontScale(0.5f);
        t.row();
        t.label(text).get().setFontScale(0.5f);

        Image image = new Image(region);

        add(image);
        add(t);
    }

    public ItemImage(ItemStack stack) {
        Table t = new Table().left().bottom();

        t.add(stack.amount + "").color(Color.DARK_GRAY).padBottom(-22).get().setFontScale(0.5f);
        t.row();
        t.add(stack.amount + "").get().setFontScale(0.5f);

        Image image = new Image(stack.item.region);

        add(image);
        add(t);
    }
}
