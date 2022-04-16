package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class Drawf{

    public static void dashLine(Color color, float x, float y, float x2, float y2){
        int segments = (int)(Math.max(Math.abs(x - x2), Math.abs(y - y2)) / tilesize * 2);
        Lines.stroke(3f, Pal.gray);
        Lines.dashLine(x, y, x2, y2, segments);
        Lines.stroke(1f, color);
        Lines.dashLine(x, y, x2, y2, segments);
        Draw.reset();
    }

    public static void target(float x, float y, float rad, Color color){
        target(x, y, rad, 1, color);
    }

    public static void target(float x, float y, float rad, float alpha, Color color){
        Lines.stroke(3f);
        Draw.color(Pal.gray, alpha);
        Lines.poly(x, y, 4, rad, Time.time * 1.5f);
        Lines.spikes(x, y, 3f/7f * rad, 6f/7f * rad, 4, Time.time * 1.5f);
        Lines.stroke(1f);
        Draw.color(color, alpha);
        Lines.poly(x, y, 4, rad, Time.time * 1.5f);
        Lines.spikes(x, y, 3f/7f * rad, 6f/7f * rad, 4, Time.time * 1.5f);
        Draw.reset();
    }

    /** Sets Draw.z to the text layer, and returns the previous layer. */
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

    public static void light(Team team, float x, float y, float radius, Color color, float opacity){
        if(allowLight(team)) renderer.lights.add(x, y, radius, color, opacity);
    }

    public static void light(Team team, Position pos, float radius, Color color, float opacity){
       light(team, pos.getX(), pos.getY(), radius, color, opacity);
    }

    public static void light(Team team, float x, float y, TextureRegion region, Color color, float opacity){
        if(allowLight(team)) renderer.lights.add(x, y, region, color, opacity);
    }

    public static void light(Team team, float x, float y, float x2, float y2){
        if(allowLight(team)) renderer.lights.line(x, y, x2, y2, 30, Color.orange, 0.3f);
    }

    public static void light(Team team, float x, float y, float x2, float y2, float stroke, Color tint, float alpha){
        if(allowLight(team)) renderer.lights.line(x, y, x2, y2, stroke, tint, alpha);
    }

    private static boolean allowLight(Team team){
        return renderer != null && (team == Team.derelict || team == Vars.player.team() || state.rules.enemyLights);
    }

    public static void selected(Building tile, Color color){
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
            x*tilesize + block.offset + offset * p.x,
            y*tilesize + block.offset + offset * p.y, i * 90);
        }
        Draw.reset();
    }

    public static void shadow(float x, float y, float rad){
        shadow(x, y, rad, 1f);
    }

    public static void shadow(float x, float y, float rad, float alpha){
        Draw.color(0, 0, 0, 0.4f * alpha);
        Draw.rect("circle-shadow", x, y, rad * Draw.xscl, rad * Draw.yscl);
        Draw.color();
    }

    public static void shadow(TextureRegion region, float x, float y, float rotation){
        Draw.color(Pal.shadow);
        Draw.rect(region, x, y, rotation);
        Draw.color();
    }

    public static void shadow(TextureRegion region, float x, float y){
        Draw.color(Pal.shadow);
        Draw.rect(region, x, y);
        Draw.color();
    }
    
    public static void shadow(TextureRegion region, float x, float y, float width, float height, float rotation){
        Draw.color(Pal.shadow);
        Draw.rect(region, x, y, width, height, rotation);
        Draw.color();
    }

    public static void liquid(TextureRegion region, float x, float y, float alpha, Color color, float rotation){
        Draw.color(color, alpha);
        Draw.rect(region, x, y, rotation);
        Draw.color();
    }

    public static void liquid(TextureRegion region, float x, float y, float alpha, Color color){
        Draw.color(color, alpha);
        Draw.rect(region, x, y);
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

    public static void select(float x, float y, float radius, Color color){
        Lines.stroke(3f, Pal.gray);
        Lines.square(x, y, radius + 1f);
        Lines.stroke(1f, color);
        Lines.square(x, y, radius);
        Draw.reset();
    }

    public static void square(float x, float y, float radius, float rotation, Color color){
        Lines.stroke(3f, Pal.gray);
        Lines.square(x, y, radius + 1f, rotation);
        Lines.stroke(1f, color);
        Lines.square(x, y, radius + 1f, rotation);
        Draw.reset();
    }

    public static void square(float x, float y, float radius, float rotation){
        square(x, y, radius, rotation, Pal.accent);
    }

    public static void square(float x, float y, float radius, Color color){
        square(x, y, radius, 45, color);
    }

    public static void square(float x, float y, float radius){
        square(x, y, radius, 45);
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

    public static void laser(Team team, TextureRegion line, TextureRegion edge, float x, float y, float x2, float y2){
        laser(team, line, edge, edge, x, y, x2, y2, 1f);
    }

    public static void laser(Team team, TextureRegion line, TextureRegion start, TextureRegion end, float x, float y, float x2, float y2){
        laser(team, line, start, end, x, y, x2, y2, 1f);
    }

    public static void laser(Team team, TextureRegion line, TextureRegion edge, float x, float y, float x2, float y2, float scale){
        laser(team, line, edge, edge, x, y, x2, y2, scale);
    }

    public static void laser(Team team, TextureRegion line, TextureRegion start, TextureRegion end, float x, float y, float x2, float y2, float scale){
        float scl = 8f * scale * Draw.scl, rot = Mathf.angle(x2 - x, y2 - y);
        float vx = Mathf.cosDeg(rot) * scl, vy = Mathf.sinDeg(rot) * scl;

        Draw.rect(start, x, y, start.width * scale * Draw.scl, start.height * scale * Draw.scl, rot + 180);
        Draw.rect(end, x2, y2, end.width * scale * Draw.scl, end.height * scale * Draw.scl, rot);

        Lines.stroke(12f * scale);
        Lines.line(line, x + vx, y + vy, x2 - vx, y2 - vy, false);
        Lines.stroke(1f);

        light(team, x, y, x2, y2);
    }

    public static void tri(float x, float y, float width, float length, float rotation){
        float oy = 17f / 63f * length;
        Draw.rect(Core.atlas.find("shape-3"), x, y - oy + length / 2f, width, length, width / 2f, oy, rotation - 90);
    }

    public static void construct(Building t, UnlockableContent content, float rotation, float progress, float speed, float time){
        construct(t, content.fullIcon, rotation, progress, speed, time);
    }

    public static void construct(float x, float y, TextureRegion region, float rotation, float progress, float speed, float time){
        construct(x, y, region, Pal.accent, rotation, progress, speed, time);
    }
    
    public static void construct(float x, float y, TextureRegion region, Color color, float rotation, float progress, float speed, float time){
        Shaders.build.region = region;
        Shaders.build.progress = progress;
        Shaders.build.color.set(color);
        Shaders.build.color.a = speed;
        Shaders.build.time = -time / 20f;

        Draw.shader(Shaders.build);
        Draw.rect(region, x, y, rotation);
        Draw.shader();

        Draw.reset();
    }

    public static void construct(Building t, TextureRegion region, float rotation, float progress, float speed, float time){
        construct(t, region, Pal.accent, rotation, progress, speed, time);
    }
        
    public static void construct(Building t, TextureRegion region, Color color, float rotation, float progress, float speed, float time){
        Shaders.build.region = region;
        Shaders.build.progress = progress;
        Shaders.build.color.set(color);
        Shaders.build.color.a = speed;
        Shaders.build.time = -time / 20f;

        Draw.shader(Shaders.build);
        Draw.rect(region, t.x, t.y, rotation);
        Draw.shader();

        Draw.color(Pal.accent);
        Draw.alpha(speed);

        Lines.lineAngleCenter(t.x + Mathf.sin(time, 20f, Vars.tilesize / 2f * t.block.size - 2f), t.y, 90, t.block.size * Vars.tilesize - 4f);

        Draw.reset();
    }
    
    /** Draws a sprite that should be light-wise correct, when rotated. Provided sprite must be symmetrical in shape. */
    public static void spinSprite(TextureRegion region, float x, float y, float r){
        r = Mathf.mod(r, 90f);
        Draw.rect(region, x, y, r);
        Draw.alpha(r / 90f);
        Draw.rect(region, x, y, r - 90f);
        Draw.alpha(1f);
    }
}
