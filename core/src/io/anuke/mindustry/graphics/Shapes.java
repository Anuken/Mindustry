package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.CapStyle;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;

//TODO remove
public class Shapes{

    public static void laser(String line, String edge, float x, float y, float x2, float y2, float scale){
        laser(line, edge, x, y, x2, y2, Mathf.atan2(x2 - x, y2 - y), scale);
    }

    public static void laser(String line, String edge, float x, float y, float x2, float y2){
        laser(line, edge, x, y, x2, y2, Mathf.atan2(x2 - x, y2 - y), 1f);
    }

    public static void laser(String line, String edge, float x, float y, float x2, float y2, float rotation, float scale){

        Lines.stroke(12f * scale);
        Lines.line(Core.atlas.find(line), x, y, x2, y2, CapStyle.none, 0f);
        Lines.stroke(1f);

        TextureRegion region = Core.atlas.find(edge);

        Draw.rect(edge, x, y, region.getWidth() * Draw.scl, region.getHeight() * scale * Draw.scl).rot(rotation + 180);

        Draw.rect(edge, x2, y2, region.getWidth() * Draw.scl, region.getHeight() * scale * Draw.scl).rot(rotation);
    }

    public static void tri(float x, float y, float width, float length, float rotation){
        float oy = 17f / 63f * length;
        Core.graphics.batch().draw().tex(Core.atlas.find("shape-3")).pos(x - width / 2f, y - oy)
            .origin(width / 2f, oy).size(width, length).rot(rotation - 90);
    }
}
