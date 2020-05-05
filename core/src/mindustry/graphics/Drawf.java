package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Drawf{

    //an experiment, to be removed
    public static void runes(float x, float y, int[] text){
        int height = 6, width = 5;
        float scale = 3;
        float th = height * scale, tw = width * scale;
        float skewx = width * scale, skewy = 0;

        Draw.color(Pal.accent);

        for(int i = 0; i < text.length; i++){
            float ox = x + i*tw*width;

            for(int j = 0; j < width * height; j++){
                int cx = j % width, cy = j / width;
                float rx = ox + cx * tw + skewx * cy, ry = y + cy * th;

                if((text[i] & (1 << j)) != 0){
                    Fill.quad(rx, ry, rx + tw, ry, rx + tw + skewx, ry + th + skewy, rx + skewx, ry + th + skewy);
                }
            }
        }
    }

    public static float text(){
        float z = Draw.z();
        if(renderer.pixelator.enabled()){
            Draw.z(Layer.endPixeled);
        }

        return z;
    }

    public static void light(float x, float y, float radius, Color color, float opacity){
        renderer.lights.add(x, y, radius, color, opacity);
    }

    public static void light(Position pos, float radius, Color color, float opacity){
       light(pos.getX(), pos.getY(), radius, color, opacity);
    }

    public static void light(float x, float y, TextureRegion region, Color color, float opacity){
        renderer.lights.add(x, y, region, color, opacity);
    }

    public static void light(float x, float y, float x2, float y2){
        renderer.lights.line(x, y, x2, y2, 30, Color.orange, 0.3f);
    }

    public static void light(float x, float y, float x2, float y2, float stroke, Color tint, float alpha){
        renderer.lights.line(x, y, x2, y2, stroke, tint, alpha);
    }

    public static void selected(Tilec tile, Color color){
        selected(tile.tile(), color);
    }

    public static void selected(Tile tile, Color color){
        selected(tile.x, tile.y, tile.block(), color);
    }

    public static void selected(int x, int y, Block block, Color color){
        Draw.color(color);
        for(int i = 0; i < 4; i++){
            Point2 p = Geometry.d8edge[i];
            float offset = -Math.max(block.size - 1, 0) / 2f * tilesize;
            Draw.rect("block-select",
            x*tilesize + block.offset() + offset * p.x,
            y*tilesize + block.offset() + offset * p.y, i * 90);
        }
        Draw.reset();
    }

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

        Drawf.light(x, y, x2, y2);
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
