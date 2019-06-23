package io.anuke.mindustry.editor;

import io.anuke.arc.function.Consumer;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Scaling;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

import static io.anuke.mindustry.Vars.world;

public class MapLoadDialog extends FloatingDialog{
    private Map selected = null;

    public MapLoadDialog(Consumer<Map> loader){
        super("$editor.loadmap");

        shown(this::rebuild);
        rebuild();

        TextButton button = new TextButton("$load");
        button.setDisabled(() -> selected == null);
        button.clicked(() -> {
            if(selected != null){
                loader.accept(selected);
                hide();
            }
        });

        buttons.defaults().size(200f, 50f);
        buttons.addButton("$cancel", this::hide);
        buttons.add(button);
    }

    public void rebuild(){
        cont.clear();
        if(world.maps.all().size > 0){
            selected = world.maps.all().first();
        }

        ButtonGroup<TextButton> group = new ButtonGroup<>();

        int maxcol = 3;

        int i = 0;

        Table table = new Table();
        table.defaults().size(200f, 90f).pad(4f);
        table.margin(10f);

        ScrollPane pane = new ScrollPane(table, "horizontal");
        pane.setFadeScrollBars(false);

        for(Map map : world.maps.all()){

            TextButton button = new TextButton(map.name(), "toggle");
            button.add(new BorderImage(map.texture, 2f).setScaling(Scaling.fit)).size(16 * 4f);
            button.getCells().reverse();
            button.clicked(() -> selected = map);
            button.getLabelCell().grow().left().padLeft(5f);
            group.add(button);
            table.add(button);
            if(++i % maxcol == 0) table.row();
        }

        if(world.maps.all().size == 0){
            table.add("$maps.none").center();
        }else{
            cont.add("$editor.loadmap");
        }

        cont.row();
        cont.add(pane);
    }

}
