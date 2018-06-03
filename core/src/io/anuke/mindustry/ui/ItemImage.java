package io.anuke.mindustry.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.Image;
import io.anuke.ucore.scene.ui.layout.Stack;
import io.anuke.ucore.scene.ui.layout.Table;

public class ItemImage extends Stack {
    private Image image;

    public ItemImage(TextureRegion region, Supplier<String> text, Color color) {
        Table t = new Table().left().bottom();

        t.label(text).get().setFontScale(0.5f);

        image = new Image(region);
        image.setColor(color);

        add(image);
        add(t);
    }

    public ItemImage updateColor(Supplier<Color> c){
        image.update(() -> image.setColor(c.get()));
        return this;
    }

    public ItemImage updateRegion(Supplier<TextureRegion> c){
        image.update(() -> image.setDrawable(new TextureRegionDrawable(c.get())));
        return this;
    }
}
