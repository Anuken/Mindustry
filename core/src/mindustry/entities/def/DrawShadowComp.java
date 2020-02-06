package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class DrawShadowComp implements Drawc, Rotc, Flyingc{
    static final float shadowTX = -12, shadowTY = -13, shadowColor = Color.toFloatBits(0, 0, 0, 0.22f);

    transient float x, y, rotation;

    abstract TextureRegion getShadowRegion();

    void drawShadow(){
        if(!isGrounded()){
            Draw.color(shadowColor);
            Draw.rect(getShadowRegion(), x + shadowTX * elevation(), y + shadowTY * elevation(), rotation - 90);
            Draw.color();
        }
    }
}
