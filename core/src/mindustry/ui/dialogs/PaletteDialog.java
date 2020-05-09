package mindustry.ui.dialogs;

import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class PaletteDialog extends Dialog{
    private Cons<Color> cons;

    public PaletteDialog(){
        super("");
        build();
    }

    private void build(){
        Table table = new Table();
        cont.add(table);

        for(int i = 0; i < playerColors.length; i++){
            Color color = playerColors[i];

            ImageButton button = table.button(Tex.whiteui, Styles.clearTogglei, 34, () -> {
                cons.get(color);
                hide();
            }).size(48).get();
            button.setChecked(player.color().equals(color));
            button.getStyle().imageUpColor = color;

            if(i % 4 == 3){
                table.row();
            }
        }

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back)
                hide();
        });

    }

    public void show(Cons<Color> cons){
        this.cons = cons;
        show();
    }
}
