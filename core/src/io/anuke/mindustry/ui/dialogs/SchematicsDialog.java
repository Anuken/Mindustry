package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Schematics.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;

import static io.anuke.mindustry.Vars.*;

public class SchematicsDialog extends FloatingDialog{
    private SchematicInfoDialog info = new SchematicInfoDialog();
    private String search = "";

    public SchematicsDialog(){
        super("$schematics");

        addCloseButton();
        shown(this::setup);
    }

    void setup(){
        search = "";
        Runnable[] rebuildPane = {null};

        cont.top();
        cont.clear();

        cont.table(s -> {
            s.left();
            s.addImage(Icon.zoom).color(Pal.gray);
            s.addField(search, res -> {
                search = res;
                rebuildPane[0].run();
            }).growX();
        }).fillX().padBottom(4);

        cont.row();

        cont.pane(t -> {
            t.top();
            t.margin(20f);
            rebuildPane[0] = () -> {
                t.clear();
                int i = 0;
                for(Schematic s : schematics.all()){
                    if(!search.isEmpty() && !s.name().contains(search)) continue;

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
            };

            rebuildPane[0].run();
        }).get().setScrollingDisabled(true, false);
    }

    public void showInfo(Schematic schematic){
        info.show(schematic);
    }

    public static class SchematicInfoDialog extends FloatingDialog{

        SchematicInfoDialog(){
            super("");
            setFillParent(false);
            addCloseButton();
        }

        public void show(Schematic schem){
            cont.clear();
            title.setText(schem.name());

            cont.add(new BorderImage(schematics.getPreview(schem, PreviewRes.high))).maxSize(400f);
            cont.row();

            show();
        }
    }
}
