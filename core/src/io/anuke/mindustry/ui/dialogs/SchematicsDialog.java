package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Schematics.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.mindustry.Vars.schematics;

public class SchematicsDialog extends FloatingDialog{

    public SchematicsDialog(){
        super("$schematics");

        shown(this::setup);
    }

    void setup(){
        cont.clear();

        cont.pane(t -> {
            int i = 0;
            for(Schematic s : schematics.all()){
                addButton(b -> {
                    Texture tex = schematics.getPreview(s, PreviewRes.low);
                    b.stack(new Image(tex), new BorderImage(tex)).size(100f);
                }, () -> {

                }).size(110f).pad(4);

                if(++i % 3 == 0){
                    t.row();
                }
            }
        });
    }
}
