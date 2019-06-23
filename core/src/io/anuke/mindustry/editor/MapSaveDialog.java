package io.anuke.mindustry.editor;

import io.anuke.arc.function.Consumer;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.mindustry.core.Platform;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;

import static io.anuke.mindustry.Vars.ui;
import static io.anuke.mindustry.Vars.world;

public class MapSaveDialog extends FloatingDialog{
    private TextField field;
    private Consumer<String> listener;

    public MapSaveDialog(Consumer<String> cons){
        super("$editor.savemap");
        field = new TextField();
        listener = cons;

        Platform.instance.addDialog(field);

        shown(() -> {
            cont.clear();
            cont.label(() -> {
                Map map = world.maps.byName(field.getText());
                if(map != null){
                    if(map.custom){
                        return "$editor.overwrite";
                    }else{
                        return "$editor.failoverwrite";
                    }
                }
                return "";
            }).colspan(2);
            cont.row();
            cont.add("$editor.mapname").padRight(14f);
            cont.add(field).size(220f, 48f);
        });

        buttons.defaults().size(200f, 50f).pad(2f);
        buttons.addButton("$cancel", this::hide);

        TextButton button = new TextButton("$save");
        button.clicked(() -> {
            if(!invalid()){
                cons.accept(field.getText());
                hide();
            }
        });
        button.setDisabled(this::invalid);
        buttons.add(button);
    }

    public void save(){
        if(!invalid()){
            listener.accept(field.getText());
        }else{
            ui.showError("$editor.failoverwrite");
        }
    }

    public void setFieldText(String text){
        field.setText(text);
    }

    private boolean invalid(){
        if(field.getText().isEmpty()){
            return true;
        }
        Map map = world.maps.byName(field.getText());
        return map != null && !map.custom;
    }
}
