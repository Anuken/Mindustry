package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Tmp;

public class Shapes{

    public static void laser(String line, String edge, float x, float y, float x2, float y2, float scale){
        laser(line, edge, x, y, x2, y2, Mathf.angle(x2 - x, y2 - y), scale);
    }

    public static void laser(String line, String edge, float x, float y, float x2, float y2){
        laser(line, edge, x, y, x2, y2, Mathf.angle(x2 - x, y2 - y), 1f);
    }

    public static void laser(String line, String edge, float x, float y, float x2, float y2, float rotation, float scale){
        TextureRegion region = Core.atlas.find(edge);

        Tmp.v1.trns(rotation, 8f * scale * Draw.scl);

        Draw.rect(Core.atlas.find(edge), x, y, region.getWidth() * scale * Draw.scl, region.getHeight() * scale * Draw.scl, rotation + 180);
        Draw.rect(Core.atlas.find(edge), x2, y2, region.getWidth() * scale * Draw.scl, region.getHeight() * scale * Draw.scl, rotation);

        Lines.stroke(12f * scale);
        Lines.line(Core.atlas.find(line), x + Tmp.v1.x, y + Tmp.v1.y, x2 - Tmp.v1.x, y2 - Tmp.v1.y, CapStyle.none, 0f);
        Lines.stroke(1f);
    }

    public static void tri(float x, float y, float width, float length, float rotation){
        float oy = 17f / 63f * length;
        Draw.rect(Core.atlas.find("shape-3"), x, y - oy + length / 2f, width, length, width / 2f, oy, rotation - 90);
    }
}
