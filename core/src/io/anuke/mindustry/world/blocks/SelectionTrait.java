package io.anuke.mindustry.world.blocks;

import io.anuke.arc.utils.Array;
import io.anuke.mindustry.type.Item;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.ButtonGroup;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.control;

public interface SelectionTrait{

    default void buildItemTable(Table table, Supplier<Item> holder, Consumer<Item> consumer){

        Array<Item> items = content.items();

        ButtonGroup<ImageButton> group = new ButtonGroup<>();
        group.setMinCheckCount(0);
        Table cont = new Table();
        cont.defaults().size(38);

        int i = 0;

        for(Item item : items){
            if(!control.unlocks.isUnlocked(item)) continue;

            ImageButton button = cont.addImageButton("white", "clear-toggle", 24, () -> {}).group(group).get();
            button.changed(() -> consumer.accept(button.isChecked() ? item : null));
            button.getStyle().imageUp = new TextureRegionDrawable(item.region);
            button.setChecked(holder.get() == item);

            if(i++ % 4 == 3){
                cont.row();
            }
        }

        table.add(cont);
    }
}
