package io.anuke.mindustry.ui.fragments;

import io.anuke.mindustry.graphics.Palette;
import io.anuke.ucore.function.Listenable;
import io.anuke.ucore.scene.Group;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;

public class LoadingFragment extends Fragment {
    private Table table;
    private TextButton button;

    @Override
    public void build(Group parent) {

       table = new table("loadDim"){{
            add().height(70f).row();

            touchable(Touchable.enabled);
            get().addImage("white").growX()
                    .height(3f).pad(4f).growX().get().setColor(Palette.accent);
            row();
            new label("$text.loading"){{
                get().setName("namelabel");
            }}.pad(10);
            row();
            get().addImage("white").growX()
                    .height(3f).pad(4f).growX().get().setColor(Palette.accent);

            row();

            button = get().addButton("$text.cancel", () -> {}).pad(20).size(250f, 70f).get();
            button.setVisible(false);
        }}.end().get();

        table.setVisible(false);
    }

    public void setButton(Listenable listener){
        button.setVisible(true);
        button.getListeners().removeIndex(button.getListeners().size - 1);
        button.clicked(listener);
    }

    public void show(){
        show("$text.loading");
    }

    public void show(String text){
        table.<Label>find("namelabel").setText(text);
        table.setVisible(true);
        table.toFront();
    }

    public void hide(){
        table.setVisible(false);
        button.setVisible(false);
    }
}
