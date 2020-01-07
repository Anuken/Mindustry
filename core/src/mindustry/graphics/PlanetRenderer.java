package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.util.*;

public class PlanetRenderer{
    private ImmediateRenderer3D rend = new ImmediateRenderer3D(false, true, 0);
    private Camera3D cam = new Camera3D();

    public void draw(){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);

        Tmp.v1.trns(Time.time() *  2f, 30f);
        cam.position.set(Tmp.v1.x, Tmp.v1.y, 5);
        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.lookAt(0, 0, 0);
        cam.update();

        rend.begin(cam.combined(), Gl.triangleStrip);
        rend.color(Color.red);
        rend.vertex(0f, 0f, 0f);
        rend.color(Color.green);
        rend.vertex(0f, 5f, 0f);
        rend.color(Color.blue);
        rend.vertex(0f, 5f, 5f);
        rend.end();
    }
}
