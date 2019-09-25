package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.input.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.mindustry.Vars.*;

public class ColorPickDialog extends Dialog{
    private Consumer<Color> cons;

    public ColorPickDialog(){
        super("");
        build();
    }

    private void build(){
        Table table = new Table();
        cont.add(table);

        for(int i = 0; i < playerColors.length; i++){
            Color color = playerColors[i];

            ImageButton button = table.addImageButton(Tex.whiteui, Styles.clearTogglei, 34, () -> {
                cons.accept(color);
                hide();
            }).size(48).get();
            button.setChecked(player.color.equals(color));
            button.getStyle().imageUpColor = color;

            if(i % 4 == 3){
                table.row();
            }
        }

        keyDown(key -> {
            if(key == KeyCode.ESCAPE || key == KeyCode.BACK)
                hide();
        });

    }

    public void show(Consumer<Color> cons){
        this.cons = cons;
        show();
    }
}
