package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Colors;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.event.Touchable;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.layout.Table;

public class LoadingFragment implements Fragment {
    private Table table;

    @Override
    public void build() {

       table = new table("loadDim"){{
            touchable(Touchable.enabled);
            get().addImage("white").growX()
                    .height(3f).pad(4f).growX().get().setColor(Colors.get("accent"));
            row();
            new label("$text.loading"){{
                get().setName("namelabel");
            }}.pad(10);
            row();
            get().addImage("white").growX()
                    .height(3f).pad(4f).growX().get().setColor(Colors.get("accent"));
        }}.end().get();

        table.setVisible(false);
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
    }
}
