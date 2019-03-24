package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.function.Consumer;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.ui.Dialog;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.player;
import static io.anuke.mindustry.Vars.playerColors;

public class ColorPickDialog extends Dialog{
    private Consumer<Color> cons;

    public ColorPickDialog(){
        super("", "dialog");
        build();
    }

    private void build(){
        Table table = new Table();
        cont.add(table);

        for(int i = 0; i < playerColors.length; i++){
            Color color = playerColors[i];

            ImageButton button = table.addImageButton("white", "clear-toggle", 34, () -> {
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
