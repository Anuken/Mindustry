package mindustry.editor;

import arc.func.*;
import arc.math.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.ui.dialogs.*;

public class MapResizeDialog extends BaseDialog{
    private static final int minSize = 50, maxSize = 500, increment = 50;
    int width, height;

    public MapResizeDialog(MapEditor editor, Intc2 cons){
        super("$editor.resizemap");
        shown(() -> {
            cont.clear();
            width = editor.width();
            height = editor.height();

            Table table = new Table();

            for(boolean w : Mathf.booleans){
                table.add(w ? "$width" : "$height").padRight(8f);
                table.defaults().height(60f).padTop(8);

                table.field((w ? width : height) + "", TextFieldFilter.digitsOnly, value -> {
                    int val = Integer.parseInt(value);
                    if(w) width = val; else height = val;
                }).valid(value -> Strings.canParsePositiveInt(value) && Integer.parseInt(value) <= maxSize && Integer.parseInt(value) >= minSize).addInputDialog(3);

                table.row();
            }
            cont.row();
            cont.add(table);

        });

        buttons.defaults().size(200f, 50f);
        buttons.button("$cancel", this::hide);
        buttons.button("$ok", () -> {
            cons.get(width, height);
            hide();
        });
    }
}
