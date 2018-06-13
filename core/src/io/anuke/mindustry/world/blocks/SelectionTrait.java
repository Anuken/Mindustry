package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.function.Supplier;
import io.anuke.ucore.scene.style.TextureRegionDrawable;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

public interface SelectionTrait {

    default void buildItemTable(Table table, Supplier<Item> holder, Consumer<Item> consumer){

        Array<Item> items = Item.all();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        Table cont = new Table();

        for(int i = 0; i < items.size; i ++){

            final int f = i;
            ImageButton button = cont.addImageButton("white", "toggle", 24, () -> consumer.accept(items.get(f)))
                    .size(38, 42).padBottom(-5.1f).group(group).get();
            button.getStyle().imageUp = new TextureRegionDrawable(new TextureRegion(items.get(i).region));
            button.setChecked(holder.get().id == f);

            if(i%4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }
}
