package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.style.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Schematics.*;

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
                addImageButton(new TextureRegionDrawable(new TextureRegion(schematics.getPreview(s, PreviewRes.low))), 100f, () -> {

                }).size(110f).pad(4);

                if(++i % 3 == 0){
                    t.row();
                }
            }
        });
    }
}
