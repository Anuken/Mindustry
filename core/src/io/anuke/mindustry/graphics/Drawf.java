package io.anuke.mindustry.graphics;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.util.*;

public class Drawf{

    public static void dashCircle(float x, float y, float rad, Color color){
        Lines.stroke(3f, Pal.gray);
        Lines.dashCircle(x, y, rad);
        Lines.stroke(1f, color);
        Lines.dashCircle(x, y, rad);
        Draw.reset();
    }

    public static void circles(float x, float y, float rad){
        circles(x, y, rad, Pal.accent);
    }

    public static void circles(float x, float y, float rad, Color color){
        Lines.stroke(3f, Pal.gray);
        Lines.circle(x, y, rad);
        Lines.stroke(1f, color);
        Lines.circle(x, y, rad);
        Draw.reset();
    }

    public static void square(float x, float y, float radius, Color color){
        Lines.stroke(3f, Pal.gray);
        Lines.square(x, y, radius + 1f, 45);
        Lines.stroke(1f, color);
        Lines.square(x, y, radius + 1f, 45);
        Draw.reset();
    }

    public static void square(float x, float y, float radius){
        square(x, y, radius, Pal.accent);
    }

    /*
    public static void square(float x, float y, float radius){
        Lines.stroke(1f, Pal.gray);
        Lines.square(x, y - 1f, radius + 1f, 45);
        Lines.stroke(1f, Pal.accent);
        Lines.square(x, y, radius + 1f, 45);
        Draw.reset();
    }*/

    public static void arrow(float x, float y, float x2, float y2, float length, float radius){
        float angle = Angles.angle(x, y, x2, y2);
        float space = 2f;
        Tmp.v1.set(x2, y2).sub(x, y).limit(length);
        float vx = Tmp.v1.x + x, vy = Tmp.v1.y + y;

        Draw.color(Pal.gray);
        Fill.poly(vx, vy, 3, radius + space, angle);
        Draw.color(Pal.accent);
        Fill.poly(vx, vy, 3, radius, angle);
        Draw.color();
    }

    public static void laser(TextureRegion line, TextureRegion edge, float x, float y, float x2, float y2, float scale){
        laser(line, edge, x, y, x2, y2, Mathf.angle(x2 - x, y2 - y), scale);
    }

    public static void laser(TextureRegion line, TextureRegion edge, float x, float y, float x2, float y2){
        laser(line, edge, x, y, x2, y2, Mathf.angle(x2 - x, y2 - y), 1f);
    }

    public static void laser(TextureRegion line, TextureRegion edge, float x, float y, float x2, float y2, float rotation, float scale){
        Tmp.v1.trns(rotation, 8f * scale * Draw.scl);

        Draw.rect(edge, x, y, edge.getWidth() * scale * Draw.scl, edge.getHeight() * scale * Draw.scl, rotation + 180);
        Draw.rect(edge, x2, y2, edge.getWidth() * scale * Draw.scl, edge.getHeight() * scale * Draw.scl, rotation);

        Lines.stroke(12f * scale);
        Lines.precise(true);
        Lines.line(line, x + Tmp.v1.x, y + Tmp.v1.y, x2 - Tmp.v1.x, y2 - Tmp.v1.y, CapStyle.none, 0f);
        Lines.precise(false);
        Lines.stroke(1f);
    }

    public static void tri(float x, float y, float width, float length, float rotation){
        float oy = 17f / 63f * length;
        Draw.rect(Core.atlas.find("shape-3"), x, y - oy + length / 2f, width, length, width / 2f, oy, rotation - 90);
    }


}
