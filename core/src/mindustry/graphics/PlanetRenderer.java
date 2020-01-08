package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.math.*;
import arc.util.*;

public class PlanetRenderer{
    private ImmediateRenderer3D rend = new ImmediateRenderer3D(true, true, 0);
    private Camera3D cam = new Camera3D();

    public void draw(){
        Draw.flush();
        Gl.clearColor(0, 0, 0, 1);
        //Gl.clear(Gl.depthBufferBit | Gl.colorBufferBit);
        Gl.lineWidth(40f);

        Tmp.v1.trns(Time.time() / 4f, 20f);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        rend.begin(cam.combined(), Gl.points);
        build(15, 15, 15, 60, 60);
        rend.end();
    }

    void build(float width, float height, float depth, int divisionsU, int divisionsV) {
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

        for (int iv = 0; iv <= divisionsV; iv++) {
            angleV = avo + stepV * iv;
            v = vs * iv;
            final float t = Mathf.sin(angleV);
            final float h = Mathf.cos(angleV) * hh;
            for (int iu = 0; iu <= divisionsU; iu++) {
                angleU = auo + stepU * iu;
                u = 1f - us * iu;
                Tmp.v31.set(Mathf.cos(angleU) * hw * t, h, Mathf.sin(angleU) * hd * t);

                rend.color(Color.white);
                rend.normal(Tmp.v32.set(Tmp.v31).nor());
                rend.texCoord(u, v);
                rend.vertex(Tmp.v31);
            }
        }
    }
}
