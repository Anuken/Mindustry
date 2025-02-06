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
        add(new Element(){
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
        });
    }

    public ReqImage(TextureRegion region, Boolp valid){
        this(new Image(region).setScaling(Scaling.fit), valid);
    }

    public boolean valid(){
        return valid.get();
    }
}
