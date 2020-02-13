package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@EntityDef(value = {Decalc.class}, pooled = true)
@Component
abstract class DecalComp implements Drawc, Timedc, Rotc, Posc, DrawLayerFloorc{
    @Import float x, y, rotation;

    Color color = new Color(1, 1, 1, 1);
    TextureRegion region;

    @Override
    public void drawFloor(){
        Draw.color(color);
        Draw.alpha(1f - Mathf.curve(fin(), 0.98f));
        Draw.rect(region, x, y, rotation);
        Draw.color();
    }

    @Override
    public float clipSize(){
        return region.getWidth()*2;
    }

}
