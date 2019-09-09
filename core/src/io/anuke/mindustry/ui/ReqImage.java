package io.anuke.mindustry.ui;

import io.anuke.arc.function.BooleanProvider;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.layout.Stack;
import io.anuke.arc.scene.ui.layout.Scl;
import io.anuke.mindustry.graphics.Pal;

public class ReqImage extends Stack{
    private final BooleanProvider valid;

    public ReqImage(Element image, BooleanProvider valid){
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

    public ReqImage(TextureRegion region, BooleanProvider valid){
        this(new Image(region), valid);
    }

    public boolean valid(){
        return valid.get();
    }
}
