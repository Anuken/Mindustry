package io.anuke.mindustry.editor;

import io.anuke.arc.func.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.ui;

public class MapSaveDialog extends FloatingDialog{
    private TextField field;
    private Cons<String> listener;

    public MapSaveDialog(Cons<String> cons){
        super("$editor.savemap");
        field = new TextField();
        listener = cons;

        Vars.platform.addDialog(field);

        shown(() -> {
            cont.clear();
            cont.label(() -> {
                Map map = Vars.maps.byName(field.getText());
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
                cons.get(field.getText());
                hide();
            }
        });
        button.setDisabled(this::invalid);
        buttons.add(button);
    }

    public void save(){
        if(!invalid()){
            listener.get(field.getText());
        }else{
            ui.showErrorMessage("$editor.failoverwrite");
        }
    }

    public void setFieldText(String text){
        field.setText(text);
    }

    private boolean invalid(){
        if(field.getText().isEmpty()){
            return true;
        }
        Map map = Vars.maps.byName(field.getText());
        return map != null && !map.custom;
    }
}
