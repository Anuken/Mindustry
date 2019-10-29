package io.anuke.mindustry.editor;

import io.anuke.arc.func.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.maps;

public class MapLoadDialog extends FloatingDialog{
    private Map selected = null;

    public MapLoadDialog(Cons<Map> loader){
        super("$editor.loadmap");

        shown(this::rebuild);

        TextButton button = new TextButton("$load");
        button.setDisabled(() -> selected == null);
        button.clicked(() -> {
            if(selected != null){
                loader.get(selected);
                hide();
            }
        });

        buttons.defaults().size(200f, 50f);
        buttons.addButton("$cancel", this::hide);
        buttons.add(button);
    }

    public void rebuild(){
        cont.clear();
        if(maps.all().size > 0){
            selected = maps.all().first();
        }

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        int maxcol = 3;

        int i = 0;

        Table table = new Table();
        table.defaults().size(200f, 90f).pad(4f);
        table.margin(10f);

        ScrollPane pane = new ScrollPane(table, Styles.horizontalPane);
        pane.setFadeScrollBars(false);

        for(Map map : maps.all()){

            TextButton button = new TextButton(map.name(), Styles.togglet);
            button.add(new BorderImage(map.safeTexture(), 2f).setScaling(Scaling.fit)).size(16 * 4f);
            button.getCells().reverse();
            button.clicked(() -> selected = map);
            button.getLabelCell().grow().left().padLeft(5f);
            group.add(button);
            table.add(button);
            if(++i % maxcol == 0) table.row();
        }

        if(maps.all().size == 0){
            table.add("$maps.none").center();
        }else{
            cont.add("$editor.loadmap");
        }

        cont.row();
        cont.add(pane);
    }

}
