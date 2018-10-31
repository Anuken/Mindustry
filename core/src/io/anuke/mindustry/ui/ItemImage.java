package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.meta.StatValue;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;

public class ItemImage extends Stack{

    public ItemImage(TextureRegion region, Supplier<CharSequence> text){
        Table t = new Table().left().bottom();

        t.label(text).color(Color.DARK_GRAY).padBottom(-Core.skin.font().getData().capHeight * 2).get().setFontScale(Unit.dp.scl(0.5f));
        t.row();
        t.label(text).get().setFontScale(Unit.dp.scl(0.5f));

        add(new Image(region));
        add(t);
    }

    public ItemImage(ItemStack stack){
        Table t = new Table().left().bottom();

        t.add(stack.amount + "").color(Color.DARK_GRAY).padBottom(-Core.skin.font().getData().capHeight * 2).get().setFontScale(Unit.dp.scl(0.5f));
        t.row();
        t.add(stack.amount + "").get().setFontScale(Unit.dp.scl(0.5f));

        add(new Image(stack.item.region));
        add(t);
    }
}
