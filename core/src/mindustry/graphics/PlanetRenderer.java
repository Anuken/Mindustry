package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.graphics.PlanetGrid.*;

public class PlanetRenderer{
    private Camera3D cam = new Camera3D();
    private float lastX, lastY;

    private PlanetMesh planet = new PlanetMesh(4, 1f, false, Color.royal);
    private PlanetMesh outline = new PlanetMesh(3, 1.01f, true, Pal.accent);
    private VertexBatch3D batch = new VertexBatch3D(false, true, 0);

    public PlanetRenderer(){
        Tmp.v1.trns(0, 2.5f);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
    }

    public void draw(){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);

        input();

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        planet.render(cam.combined());
        //outline.render(cam.combined());

        Ptile tile = outline.getTile(cam.getPickRay(Core.input.mouseX(), Core.input.mouseY()));
        if(tile != null){
            for(int i = 0; i < tile.corners.length; i++){
                batch.color(1f, 1f, 1f, 0.5f);
                batch.vertex(tile.corners[i].v);
            }
            batch.flush(cam.combined(), Gl.triangleFan);
        }

        Gl.disable(Gl.depthTest);
    }

    void input(){
        Vec3 v = Tmp.v33.set(Core.input.mouseX(), Core.input.mouseY(), 0);

        if(Core.input.keyDown(KeyCode.MOUSE_LEFT)){
            cam.position.rotate(Vec3.Y, (v.x - lastX) / 10);
        }
        lastX = v.x;
        lastY = v.y;
    }
}
