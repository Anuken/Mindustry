package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.util.*;

public class PlanetRenderer{
    private Camera3D cam = new Camera3D();
    private float lastX, lastY;

    private PlanetMesh planet = new PlanetMesh(3, 1f, false, Color.royal);
    private PlanetMesh outline = new PlanetMesh(3, 1.01f, true, Pal.accent);

    public PlanetRenderer(){
        Tmp.v1.trns(0, 2.5f);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
    }

    public void draw(){
        Draw.flush();
        Gl.clearColor(0, 0, 0, 1);
        Gl.clear(Gl.depthBufferBit | Gl.colorBufferBit);

        input();

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        planet.render(cam.combined());
        outline.render(cam.combined());
    }

    void input(){
        Vec3 v = cam.unproject(Tmp.v33.set(Core.input.mouseX(), Core.input.mouseY(), 0f));

        if(Core.input.keyDown(KeyCode.MOUSE_LEFT)){
            cam.position.rotate(Vec3.Y, (v.x - lastX) * 100);
        }
        lastX = v.x;
        lastY = v.y;
    }
}
