package mindustry.ui;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.graphics.*;

public class ReqImage extends Stack{
    private final Boolp valid;

    public ReqImage(Element image, Boolp valid){
        this.valid = valid;
        add(image);
        add(reqDraw());
    }

    public ReqImage(Element image, Boolp valid, String text){
        this.valid = valid;
        add(new Table(t -> {
            t.center();
            t.add(image).size(32f).scaling(Scaling.fit);
        }));
        add(new Table(t -> {
            t.center();
            t.add(reqDraw()).size(32f);
        }));
        add(new Table(t -> {
            t.center().bottom();
            t.add(new Label(text)).style(Styles.outlineLabel);
            t.pack();
        }));
    }

    public Element reqDraw(){
        return new Element(){
            {
                visible(() -> !valid.get());
            }

            @Override
            public void draw(){
                Lines.stroke(Scl.scl(2f), Pal.removeBack);
                Lines.line(x, y - 2f + height, x + width, y - 2f);
                Draw.color(Pal.remove);
                Lines.line(x, y + height, x + width, y);
                Draw.reset();
            }
        };
    }

    public ReqImage(TextureRegion region, Boolp valid){
        this(new Image(region).setScaling(Scaling.fit), valid);
    }

    public boolean valid(){
        return valid.get();
    }
}
