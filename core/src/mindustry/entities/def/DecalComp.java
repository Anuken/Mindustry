package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;

@EntityDef(value = {Decalc.class}, pooled = true, serialize = false)
@Component
abstract class DecalComp implements Drawc, Timedc, Rotc, Posc{
    @Import float x, y, rotation;

    Color color = new Color(1, 1, 1, 1);
    TextureRegion region;

    @Override
    public void draw(){
        Draw.z(Layer.scorch);

        Draw.color(color);
        Draw.alpha(1f - Mathf.curve(fin(), 0.98f));
        Draw.rect(region, x, y, rotation);
        Draw.color();
    }

    @Replace
    public float clipSize(){
        return region.getWidth()*2;
    }

}
