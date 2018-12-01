package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.Scaling;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.BorderImage;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.mindustry.Vars.world;

public class MapLoadDialog extends FloatingDialog{
    private Map selected = null;

    public MapLoadDialog(Consumer<Map> loader){
        super("$text.editor.loadmap");

        shown(this::rebuild);
        rebuild();

        TextButton button = new TextButton("$text.load");
        button.setDisabled(() -> selected == null);
        button.clicked(() -> {
            if(selected != null){
                loader.accept(selected);
                hide();
            }
        });

        buttons().defaults().size(200f, 50f);
        buttons().addButton("$text.cancel", this::hide);
        buttons().add(button);
    }

    public void rebuild(){
        content().clear();
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

            TextButton button = new TextButton(map.getDisplayName(), "toggle");
            button.add(new BorderImage(map.texture, 2f).setScaling(Scaling.fit)).size(16 * 4f);
            button.getCells().reverse();
            button.clicked(() -> selected = map);
            button.getLabelCell().grow().left().padLeft(5f);
            group.add(button);
            table.add(button);
            if(++i % maxcol == 0) table.row();
        }

        if(world.maps.all().size == 0){
            table.add("$text.maps.none").center();
        }else{
            content().add("$text.editor.loadmap");
        }

        content().row();
        content().add(pane);
    }

}
