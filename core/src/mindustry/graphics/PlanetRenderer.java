package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;

public class PlanetRenderer{
    private ImmediateRenderer3D rend = new ImmediateRenderer3D(500000, true, true, 0, new Shader(Core.files.internal("shaders/planet.vertex.glsl").readString(), Core.files.internal("shaders/planet.fragment.glsl").readString()));
    private Camera3D cam = new Camera3D();

    private IntArray tmpIndices = new IntArray();
    private IntArray indices = new IntArray();
    private Array<Vertex> vertices = new Array<>();

    private Color[] colors = {Color.royal, Color.tan, Color.forest, Color.olive, Color.lightGray, Color.white};
    private Simplex sim = new Simplex();

    {
        int div = 100;
        ico();
        //generate(15, 15, 15, div, div);
    }

    public void draw(){
        Draw.flush();
        Gl.clearColor(1, 1, 1, 1);
        Gl.clear(Gl.depthBufferBit | Gl.colorBufferBit);
        Gl.enable(Gl.depthTest);

        Tmp.v1.trns(Time.time() * 2f, 20f);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        rend.begin(cam.combined(), Gl.triangleStrip);
        drawTri();
        rend.end();
        Gl.disable(Gl.depthTest);
    }

    void ico(){
        float s = 2f/Mathf.sqrt(5), c = 1f/Mathf.sqrt(5);

        Array<Vec3> topPoints = new Array<>();
        topPoints.add(new Vec3(0, 0, 1));
        for(int i = 0; i < 5; i++){
            topPoints.add(new Vec3(s*Mathf.cos(i*2*Mathf.pi/5f), s*Mathf.sin(i*2*Mathf.pi/5f), c));
        }
        topPoints.addAll(topPoints.map(v -> v.cpy().scl(-1, 1, -1)));
        Array<Vec3> points = topPoints;

        vertices.addAll(points.map(p -> {
            Vertex v = new Vertex();
            v.pos.set(p).scl(4f);
            v.normal.set(p).nor();
            v.color.set(Color.royal);
            return v;
        }));

        for(int i = 0; i < 5; i++){
            indices.add(0,i+1,(i+1)%5+1);
            indices.add(6,i+7,(i+1)%5+7);
            indices.add(i+1,(i+1)%5+1,(7-i)%5+7);
            indices.add(i+1,(7-i)%5+7,(8-i)%5+7);
        }

        Array<Vertex> newVertices = new Array<>();
        IntArray newIndices = new IntArray();
        for(int i = 0; i < indices.size; i += 3){
            Vertex v1 = vertices.get(indices.get(i));
            Vertex v2 = vertices.get(indices.get(i + 1));
            Vertex v3 = vertices.get(indices.get(i + 2));

            Vec3 nor = v1.normal.cpy().add(v2.normal).add(v3.normal).nor();
            v1 = v1.copy();
            v2 = v2.copy();
            v3 = v3.copy();
            v1.normal = v2.normal = v3.normal = nor;
            newIndices.add(newVertices.size, newVertices.size + 1, newVertices.size + 2);
            newVertices.add(v1, v2, v3);
        }
        vertices = newVertices;
        indices = newIndices;
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

    void drawTri(){
        for(int i = 0; i < indices.size; i += 3){
            Vertex v1 = vertices.get(indices.get(i));
            Vertex v2 = vertices.get(indices.get(i + 1));
            Vertex v3 = vertices.get(indices.get(i + 2));

            v1.d();
            v2.d();
            v3.d();
        }
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

        Vertex copy(){
            Vertex v = new Vertex();
            v.color.set(color);
            v.normal.set(normal);
            v.uv.set(uv);
            v.pos.set(pos);
            return v;
        }

        public String toString(){
            return pos.toString();
        }
    }
}
