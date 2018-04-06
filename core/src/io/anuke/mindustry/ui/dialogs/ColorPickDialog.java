package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;

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
        content().add(table);

        for(int i = 0; i < playerColors.length; i ++){
            Color color = playerColors[i];

            ImageButton button = table.addImageButton("white", "toggle", 34, () -> {
                cons.accept(color);
                hide();
            }).size(44, 48).pad(0).padBottom(-5.1f).get();
            button.setChecked(player.getColor().equals(color));
            button.getStyle().imageUpColor = color;

            if(i%4 == 3){
                table.row();
            }
        }

        keyDown(key->{
            if(key == Keys.ESCAPE || key == Keys.BACK)
                hide();
        });

    }

    public void show(Consumer<Color> cons){
        this.cons = cons;
        show();
    }
}
