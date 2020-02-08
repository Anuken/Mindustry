package mindustry.graphics;

import arc.*;
import arc.struct.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;

import static mindustry.Vars.state;

/** Renders overlay lights. Client only. */
public class LightRenderer{
    private static final int scaling = 4;
    private float[] vertices = new float[24];
    private FrameBuffer buffer = new FrameBuffer(2, 2);
    private Array<Runnable> lights = new Array<>();

    public void add(Runnable run){
        if(!enabled()) return;

        lights.add(run);
    }

    public void add(float x, float y, float radius, Color color, float opacity){
        if(!enabled()) return;

        float res = color.toFloatBits();
        add(() -> {
            Draw.color(res);
            Draw.alpha(opacity);
            Draw.rect("circle-shadow", x, y, radius * 2, radius * 2);
        });
    }

    public void add(float x, float y, TextureRegion region, Color color, float opacity){
        if(!enabled()) return;

        float res = color.toFloatBits();
        add(() -> {
            Draw.color(res);
            Draw.alpha(opacity);
            Draw.rect(region, x, y);
        });
    }

    public void line(float x, float y, float x2, float y2){
        if(!enabled()) return;

        add(() -> {
            Draw.color(Color.orange, 0.3f);

            float stroke = 30f;
            float rot = Mathf.angleExact(x2 - x, y2 - y);
            TextureRegion ledge = Core.atlas.find("circle-end"), lmid = Core.atlas.find("circle-mid");

            float color = Draw.getColor().toFloatBits();
            float u = lmid.getU();
            float v = lmid.getV2();
            float u2 = lmid.getU2();
            float v2 = lmid.getV();


            Vec2 v1 = Tmp.v1.trnsExact(rot + 90f, stroke);
            float lx1 = x - v1.x, ly1 = y - v1.y,
            lx2 = x + v1.x, ly2 = y + v1.y,
            lx3 = x2 + v1.x, ly3 = y2 + v1.y,
            lx4 = x2 - v1.x, ly4 = y2 - v1.y;

            vertices[0] = lx1;
            vertices[1] = ly1;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = 0;

            vertices[6] = lx2;
            vertices[7] = ly2;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = 0;

            vertices[12] = lx3;
            vertices[13] = ly3;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = 0;

            vertices[18] = lx4;
            vertices[19] = ly4;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = 0;

            Draw.vert(ledge.getTexture(), vertices, 0, vertices.length);


            Vec2 v3 = Tmp.v2.trnsExact(rot, stroke);

            u = ledge.getU();
            v = ledge.getV2();
            u2 = ledge.getU2();
            v2 = ledge.getV();

            vertices[0] = lx4;
            vertices[1] = ly4;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = 0;

            vertices[6] = lx3;
            vertices[7] = ly3;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = 0;

            vertices[12] = lx3 + v3.x;
            vertices[13] = ly3 + v3.y;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = 0;

            vertices[18] = lx4 + v3.x;
            vertices[19] = ly4 + v3.y;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = 0;

            Draw.vert(ledge.getTexture(), vertices, 0, vertices.length);

            vertices[0] = lx2;
            vertices[1] = ly2;
            vertices[2] = color;
            vertices[3] = u;
            vertices[4] = v;
            vertices[5] = 0;

            vertices[6] = lx1;
            vertices[7] = ly1;
            vertices[8] = color;
            vertices[9] = u;
            vertices[10] = v2;
            vertices[11] = 0;

            vertices[12] = lx1 - v3.x;
            vertices[13] = ly1 - v3.y;
            vertices[14] = color;
            vertices[15] = u2;
            vertices[16] = v2;
            vertices[17] = 0;

            vertices[18] = lx2 - v3.x;
            vertices[19] = ly2 - v3.y;
            vertices[20] = color;
            vertices[21] = u2;
            vertices[22] = v;
            vertices[23] = 0;

            Draw.vert(ledge.getTexture(), vertices, 0, vertices.length);
        });
    }

    public boolean enabled(){
        return state.rules.lighting;
    }

    public void draw(){
        if(buffer.getWidth() != Core.graphics.getWidth()/scaling || buffer.getHeight() != Core.graphics.getHeight()/scaling){
            buffer.resize(Core.graphics.getWidth()/scaling, Core.graphics.getHeight()/scaling);
        }

        Draw.color();
        buffer.beginDraw(Color.clear);
        Draw.blend(Blending.normal);
        for(Runnable run : lights){
            run.run();
        }
        Draw.reset();
        Draw.blend();
        buffer.endDraw();

        Draw.color();
        Shaders.light.ambient.set(state.rules.ambientLight);
        Draw.shader(Shaders.light);
        Draw.rect(Draw.wrap(buffer.getTexture()), Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);
        Draw.shader();

        lights.clear();
    }
}
