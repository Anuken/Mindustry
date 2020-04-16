package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.renderer;

public class Drawf{

    public static void shadow(float x, float y, float rad){
        Draw.color(0, 0, 0, 0.4f);
        Draw.rect("circle-shadow", x, y, rad, rad);
        Draw.color();
    }

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

    public static void arrow(float x, float y, float x2, float y2, float length, float radius){
        arrow(x, y, x2, y2, length, radius, Pal.accent);
    }

    public static void arrow(float x, float y, float x2, float y2, float length, float radius, Color color){
        float angle = Angles.angle(x, y, x2, y2);
        float space = 2f;
        Tmp.v1.set(x2, y2).sub(x, y).limit(length);
        float vx = Tmp.v1.x + x, vy = Tmp.v1.y + y;

        Draw.color(Pal.gray);
        Fill.poly(vx, vy, 3, radius + space, angle);
        Draw.color(color);
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

        renderer.lights.line(x, y, x2, y2);
    }

    public static void tri(float x, float y, float width, float length, float rotation){
        float oy = 17f / 63f * length;
        Draw.rect(Core.atlas.find("shape-3"), x, y - oy + length / 2f, width, length, width / 2f, oy, rotation - 90);
    }

    public static void drawRespawn(Tilec tile, float heat, float progress, float time, UnitType to, @Nullable Playerc player){
        float x = tile.x(), y = tile.y();
        progress = Mathf.clamp(progress);

        Draw.color(Pal.darkMetal);
        Lines.stroke(2f * heat);
        Fill.poly(x, y, 4, 10f * heat);

        Draw.reset();
        if(player != null){
            TextureRegion region = to.icon(Cicon.full);

            Draw.color(0f, 0f, 0f, 0.4f * progress);
            Draw.rect("circle-shadow", x, y, region.getWidth() / 3f, region.getWidth() / 3f);
            Draw.color();

            Shaders.build.region = region;
            Shaders.build.progress = progress;
            Shaders.build.color.set(Pal.accent);
            Shaders.build.time = -time / 10f;

            Draw.shader(Shaders.build, true);
            Draw.rect(region, x, y);
            Draw.shader();

            Draw.color(Pal.accentBack);

            float pos = Mathf.sin(time, 6f, 8f);

            Lines.lineAngleCenter(x + pos, y, 90, 16f - Math.abs(pos) * 2f);

            Draw.reset();
        }

        Lines.stroke(2f * heat);

        Draw.color(Pal.accentBack);
        Lines.poly(x, y, 4, 8f * heat);

        float oy = -7f, len = 6f * heat;
        Lines.stroke(5f);
        Draw.color(Pal.darkMetal);
        Lines.line(x - len, y + oy, x + len, y + oy, CapStyle.none);
        for(int i : Mathf.signs){
            Fill.tri(x + len * i, y + oy - Lines.getStroke()/2f, x + len * i, y + oy + Lines.getStroke()/2f, x + (len + Lines.getStroke() * heat) * i, y + oy);
        }

        Lines.stroke(3f);
        Draw.color(Pal.accent);
        Lines.line(x - len, y + oy, x - len + len*2 * progress, y + oy, CapStyle.none);
        for(int i : Mathf.signs){
            Fill.tri(x + len * i, y + oy - Lines.getStroke()/2f, x + len * i, y + oy + Lines.getStroke()/2f, x + (len + Lines.getStroke() * heat) * i, y + oy);
        }
        Draw.reset();

        if(Vars.net.active() && player != null){
            tile.block().drawPlaceText(player.name(), tile.tileX(), tile.tileY() - (Math.max((tile.block().size-1)/2, 0)), true);
        }
    }
}
