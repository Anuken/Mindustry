package io.anuke.mindustry.editor;

import io.anuke.arc.func.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.ui.dialogs.*;

public class MapResizeDialog extends FloatingDialog{
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
                table.addButton("<", () -> {
                    if(w)
                        width = move(width, -1);
                    else
                        height = move(height, -1);
                }).size(60f);

                table.table(Tex.button, t -> t.label(() -> (w ? width : height) + "")).width(200);

                table.addButton(">", () -> {
                    if(w)
                        width = move(width, 1);
                    else
                        height = move(height, 1);
                }).size(60f);
                table.row();
            }
            cont.row();
            cont.add(table);

        });

        buttons.defaults().size(200f, 50f);
        buttons.addButton("$cancel", this::hide);
        buttons.addButton("$ok", () -> {
            cons.get(width, height);
            hide();
        });
    }

    static int move(int value, int direction){
        return Mathf.clamp((value / increment + direction) * increment, minSize, maxSize);
    }
}
