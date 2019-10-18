package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Texture.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
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
        Core.assets.load("sprites/schematic-background.png", Texture.class).loaded = t -> {
            ((Texture)t).setWrap(TextureWrap.Repeat);
        };

        shouldPause = true;
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

                    Button[] sel = {null};
                    sel[0] = t.addButton(b -> {
                        b.top();
                        b.margin(4f);
                        b.table(buttons -> {
                            buttons.left();
                            buttons.defaults().size(50f);
                            buttons.addImageButton(Icon.pencilSmall, Styles.clearPartiali, () -> {

                            });

                            buttons.addImageButton(Icon.trash16Small, Styles.clearPartiali, () -> {

                            });

                        }).growX().height(50f);
                        b.row();
                        b.add(s.name()).center().color(Color.lightGray).top().left().get().setEllipsis(true);
                        b.row();
                        b.add(new SchematicImage(s).setScaling(Scaling.fit).setName("border")).size(200f);
                    }, () -> {
                        if(sel[0].childrenPressed()) return;
                        control.input.useSchematic(s);
                        hide();
                    }).pad(4).style(Styles.cleari).get();

                    //BorderImage image = sel[0].find("border");
                    //image.update(() -> image.borderColor = (sel[0].isOver() ? Pal.accent : Pal.gray));

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

    public static class SchematicImage extends Image{
        public float scaling = 16f;
        public float thickness = 4f;
        public Color borderColor = Pal.gray;

        public SchematicImage(Schematic s){
            super(schematics.getPreview(s, PreviewRes.high));
        }

        @Override
        public void draw(){
            Texture background = Core.assets.get("sprites/schematic-background.png", Texture.class);
            TextureRegion region = Draw.wrap(background);
            float xr = width / scaling;
            float yr = height / scaling;
            region.setU2(xr);
            region.setV2(yr);
            Draw.rect(region, x + width/2f, y + height/2f, width, height);

            super.draw();

            Draw.color(borderColor);
            Draw.alpha(parentAlpha);
            Lines.stroke(Scl.scl(thickness));
            Lines.rect(x, y, width, height);
            Draw.reset();
        }
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
