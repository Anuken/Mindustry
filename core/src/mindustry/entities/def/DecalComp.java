package mindustry.entities.def;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;

@Component
abstract class DecalComp implements Drawc, Timedc, Rotc, Posc, DrawLayerFloorc{
    Color color = new Color(1, 1, 1, 1);
    TextureRegion region;

    @Override
    public void drawFloor(){
        Draw.color(color);
        Draw.rect(region, x(), y(), rotation());
        Draw.color();
    }

    @Override
    public float clipSize(){
        return region.getWidth()*2;
    }

}
