package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;

public class PlanetRenderer{
    private ImmediateRenderer3D rend = new ImmediateRenderer3D(500000, true, true, 0);
    private Camera3D cam = new Camera3D();

    private ShortArray tmpIndices = new ShortArray();
    private ShortArray indices = new ShortArray();
    private Array<Vertex> vertices = new Array<>();

    private Color[] colors = {Color.royal, Color.tan, Color.forest, Color.olive, Color.lightGray, Color.white};
    private Simplex sim = new Simplex();

    {
        int div = 100;
        generate(15, 15, 15, div, div);
    }

    public void draw(){
        Draw.flush();
        Gl.clearColor(0, 0, 0, 1);
        Gl.clear(Gl.depthBufferBit | Gl.colorBufferBit);
        Gl.enable(Gl.depthTest);

        Tmp.v1.trns(Time.time(), 20f);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        rend.begin(cam.combined(), Gl.triangleStrip);
        drawSphere();
        rend.end();
        Gl.disable(Gl.depthTest);
    }

    void generate(float width, float height, float depth, int divisionsU, int divisionsV){
        float angleUFrom = 0, angleUTo = 360f, angleVFrom = 0, angleVTo = 180f;
        final float hw = width * 0.5f;
        final float hh = height * 0.5f;
        final float hd = depth * 0.5f;
        final float auo = Mathf.degRad * angleUFrom;
        final float stepU = (Mathf.degRad * (angleUTo - angleUFrom)) / divisionsU;
        final float avo = Mathf.degRad * angleVFrom;
        final float stepV = (Mathf.degRad * (angleVTo - angleVFrom)) / divisionsV;
        final float us = 1f / divisionsU;
        final float vs = 1f / divisionsV;
        float u, v, angleU, angleV;

        final int s = divisionsU + 3;
        int tempOffset = 0;

        tmpIndices.clear();
        tmpIndices.ensureCapacity(divisionsU * 2);
        tmpIndices.size = s;

        vertices.clear();
        indices.clear();

        for(int iv = 0; iv <= divisionsV; iv++){
            angleV = avo + stepV * iv;
            v = vs * iv;
            final float t = Mathf.sin(angleV);
            final float h = Mathf.cos(angleV) * hh;
            for(int iu = 0; iu <= divisionsU; iu++){
                angleU = auo + stepU * iu;
                u = 1f - us * iu;
                Tmp.v31.set(Mathf.cos(angleU) * hw * t, h, Mathf.sin(angleU) * hd * t);

                Vertex vert = new Vertex();
                vert.normal.set(Tmp.v32.set(Tmp.v31).nor());
                vert.color.set(color(Tmp.v31));//set(vert.normal.x, vert.normal.y, vert.normal.z, 1f);
                vert.uv.set(u, v);
                vert.pos.set(Tmp.v31);

                int index = vertices.size;
                vertices.add(vert);

                tmpIndices.set(tempOffset, (short)index);
                final int o = tempOffset + s;
                if(iv > 0 && iu > 0){
                    indices.add(tmpIndices.get(tempOffset), tmpIndices.get((o - 1) % s), tmpIndices.get((o - (divisionsU + 2)) % s), tmpIndices.get((o - (divisionsU + 1)) % s));
                    //builder.rect(tmpIndices.get(tempOffset), tmpIndices.get((o - 1) % s), tmpIndices.get((o - (divisionsU + 2)) % s), tmpIndices.get((o - (divisionsU + 1)) % s));
                }
                tempOffset = (tempOffset + 1) % tmpIndices.size;
            }
        }
    }

    Color color(Vec3 v){
        double value = sim.octaveNoise3D(6, 0.6, 1.0 / 10.0, v.x, v.y, v.z);
        return colors[Mathf.clamp((int)(value * colors.length), 0, colors.length - 1)];
    }

    void drawSphere(){
        for(int i = 0; i < indices.size; i += 4){
            Vertex v1 = vertices.get(indices.get(i));
            Vertex v2 = vertices.get(indices.get(i + 1));
            Vertex v3 = vertices.get(indices.get(i + 2));
            Vertex v4 = vertices.get(indices.get(i + 3));

            v1.d();
            v2.d();
            v3.d();

            v3.d();
            v4.d();
            v1.d();
        }
    }

    class Vertex{
        Color color = new Color();
        Vec3 normal = new Vec3();
        Vec2 uv = new Vec2();
        Vec3 pos = new Vec3();

        void d(){
            rend.color(color);
            rend.normal(normal);
            rend.texCoord(uv.x, uv.y);
            rend.vertex(pos);
        }
    }
}
