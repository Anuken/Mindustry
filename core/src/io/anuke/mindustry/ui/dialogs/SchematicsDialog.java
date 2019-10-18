package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Schematics.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.mindustry.Vars.*;

public class SchematicsDialog extends FloatingDialog{

    public SchematicsDialog(){
        super("$schematics");

        addCloseButton();
        shown(this::setup);
    }

    void setup(){
        cont.clear();

        cont.pane(t -> {
            t.top();
            t.margin(20f);
            int i = 0;
            for(Schematic s : schematics.all()){
                Button sel = t.addButton(b -> {
                    Texture tex = schematics.getPreview(s, PreviewRes.high);
                    b.add(s.name()).center().color(Color.lightGray).fillY().get().setEllipsis(true);
                    b.row();
                    b.add(new BorderImage(tex){
                        @Override
                        public void draw(){
                            //Draw.blend(Blending.disabled);
                            super.draw();
                            //Draw.blend();
                        }
                    }.setScaling(Scaling.fit).setName("border"));
                }, () -> {
                    control.input.useSchematic(s);
                    hide();
                }).size(200f, 230f).pad(2).style(Styles.clearPartiali).get();

                BorderImage image = sel.find("border");
                image.update(() -> image.borderColor = (sel.isOver() ? Pal.accent : Pal.gray));

                if(++i % 4 == 0){
                    t.row();
                }
            }
        }).get().setScrollingDisabled(true, false);
    }
}
