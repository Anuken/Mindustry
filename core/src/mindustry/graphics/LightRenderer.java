package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;

import static mindustry.Vars.*;

/** Renders overlay lights. Client only. */
public class LightRenderer{
    private static final int scaling = 4;

    private float[] vertices = new float[28];
    private FrameBuffer buffer = new FrameBuffer();
    private Seq<Runnable> lights = new Seq<>();
    private Seq<CircleLight> circles = new Seq<>(CircleLight.class);
    private int circleIndex = 0;
    private TextureRegion circleRegion;

    public void add(Runnable run){
        if(!enabled()) return;

        lights.add(run);
    }

    public void add(float x, float y, float radius, Color color, float opacity){
        if(!enabled() || radius <= 0f) return;

        //TODO: clipping.

        float res = Color.toFloatBits(color.r, color.g, color.b, opacity);

        if(circles.size <= circleIndex) circles.add(new CircleLight());

        //pool circles to prevent runaway GC usage from lambda capturing
        var light = circles.items[circleIndex];
        light.set(x, y, res, radius);

        circleIndex ++;
    }

    public void add(float x, float y, TextureRegion region, Color color, float opacity){
        add(x, y, region, 0f, color, opacity);
    }

    public void add(float x, float y, TextureRegion region, float rotation, Color color, float opacity){
        if(!enabled()) return;

        float res = color.toFloatBits();
        float xscl = Draw.xscl, yscl = Draw.yscl;
        add(() -> {
            Draw.color(res);
            Draw.alpha(opacity);
            Draw.scl(xscl, yscl);
            Draw.rect(region, x, y, rotation);
            Draw.scl();
        });
    }

    public void line(float x, float y, float x2, float y2, float stroke, Color tint, float alpha){
        if(!enabled()) return;

        add(() -> {
            Draw.color(tint, alpha);

            float rot = Mathf.angleExact(x2 - x, y2 - y);
            TextureRegion ledge = Core.atlas.find("circle-end"), lmid = Core.atlas.find("circle-mid");
            float depth = lmid.getDepth();

            float color = Draw.getColorPacked();
            float u = lmid.u;
            float v = lmid.v2;
            float u2 = lmid.u2;
            float v2 = lmid.v;

            Vec2 v1 = Tmp.v1.trnsExact(rot + 90f, stroke);
            float lx1 = x - v1.x, ly1 = y - v1.y,
            lx2 = x + v1.x, ly2 = y + v1.y,
            lx3 = x2 + v1.x, ly3 = y2 + v1.y,
            lx4 = x2 - v1.x, ly4 = y2 - v1.y;

            vertices[0] = lx1;
            vertices[1] = ly1;
            vertices[2] = u;
            vertices[3] = v;
            vertices[4] = depth;
            vertices[5] = color;
            vertices[6] = 0;

            vertices[7] = lx2;
            vertices[8] = ly2;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = depth;
            vertices[12] = color;
            vertices[13] = 0;

            vertices[14] = lx3;
            vertices[15] = ly3;
            vertices[16] = u2;
            vertices[17] = v2;
            vertices[18] = depth;
            vertices[19] = color;
            vertices[20] = 0;

            vertices[21] = lx4;
            vertices[22] = ly4;
            vertices[23] = u2;
            vertices[24] = v;
            vertices[25] = depth;
            vertices[26] = color;
            vertices[27] = 0;

            Draw.vert(ledge.texture, vertices, 0, vertices.length);

            Vec2 v3 = Tmp.v2.trnsExact(rot, stroke);

            depth = ledge.getDepth();
            u = ledge.u;
            v = ledge.v2;
            u2 = ledge.u2;
            v2 = ledge.v;

            vertices[0] = lx4;
            vertices[1] = ly4;
            vertices[2] = u;
            vertices[3] = v;
            vertices[4] = depth;
            vertices[5] = color;
            vertices[6] = 0;

            vertices[7] = lx3;
            vertices[8] = ly3;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = depth;
            vertices[12] = color;
            vertices[13] = 0;

            vertices[14] = lx3 + v3.x;
            vertices[15] = ly3 + v3.y;
            vertices[16] = u2;
            vertices[17] = v2;
            vertices[18] = depth;
            vertices[19] = color;
            vertices[20] = 0;

            vertices[21] = lx4 + v3.x;
            vertices[22] = ly4 + v3.y;
            vertices[23] = u2;
            vertices[24] = v;
            vertices[25] = depth;
            vertices[26] = color;
            vertices[27] = 0;

            Draw.vert(ledge.texture, vertices, 0, vertices.length);

            vertices[0] = lx2;
            vertices[1] = ly2;
            vertices[2] = u;
            vertices[3] = v;
            vertices[4] = depth;
            vertices[5] = color;
            vertices[6] = 0;

            vertices[7] = lx1;
            vertices[8] = ly1;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = depth;
            vertices[12] = color;
            vertices[13] = 0;

            vertices[14] = lx1 - v3.x;
            vertices[15] = ly1 - v3.y;
            vertices[16] = u2;
            vertices[17] = v2;
            vertices[18] = depth;
            vertices[19] = color;
            vertices[20] = 0;

            vertices[21] = lx2 - v3.x;
            vertices[22] = ly2 - v3.y;
            vertices[23] = u2;
            vertices[24] = v;
            vertices[25] = depth;
            vertices[26] = color;
            vertices[27] = 0;

            Draw.vert(ledge.texture, vertices, 0, vertices.length);
        });
    }

    public boolean enabled(){
        return state.rules.lighting && state.rules.ambientLight.a > 0.0001f && renderer.drawLight;
    }

    public void draw(){
        if(!Vars.enableLight){
            lights.clear();
            circleIndex = 0;
            return;
        }

        if(circleRegion == null) circleRegion = Core.atlas.find("circle-shadow");

        buffer.resize(Core.graphics.getWidth()/scaling, Core.graphics.getHeight()/scaling);

        Draw.color();
        buffer.begin(Color.clear);
        Draw.sort(false);
        Gl.blendEquationSeparate(Gl.funcAdd, Gl.max);
        //apparently necessary
        Blending.normal.apply();

        for(Runnable run : lights){
            run.run();
        }
        for(int i = 0; i < circleIndex; i++){
            var cir = circles.items[i];
            Draw.color(cir.color);
            Draw.rect(circleRegion, cir.x, cir.y, cir.radius * 2, cir.radius * 2);
        }
        Draw.reset();
        Draw.sort(true);
        buffer.end();
        Gl.blendEquationSeparate(Gl.funcAdd, Gl.funcAdd);

        Draw.color();
        Shaders.light.ambient.set(state.rules.ambientLight);
        buffer.blit(Shaders.light);

        lights.clear();
        circleIndex = 0;
    }

    static class CircleLight{
        float x, y, color, radius;

        public void set(float x, float y, float color, float radius){
            this.x = x;
            this.y = y;
            this.color = color;
            this.radius = radius;
        }
    }
}
