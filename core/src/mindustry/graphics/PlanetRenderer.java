package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g3d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.type.*;

public class PlanetRenderer{
    private Camera3D cam = new Camera3D();
    private float lastX, lastY, camLength = 4f;

    //private PlanetMesh planet = new PlanetMesh(6, 1f, false, Color.royal);
    //private PlanetMesh outline = new PlanetMesh(3, 1.3f, true, Pal.accent);
    private VertexBatch3D batch = new VertexBatch3D(false, true, 0);

    public PlanetRenderer(){
        Tmp.v1.trns(0, camLength);
        cam.position.set(Tmp.v1.x, 0f, Tmp.v1.y);
    }

    public void render(Planet planet){
        Draw.flush();
        Gl.clear(Gl.depthBufferBit);
        Gl.enable(Gl.depthTest);

        input();

        cam.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
        cam.update();
        cam.lookAt(0, 0, 0);
        cam.update();

        planet.mesh.render(cam.combined());
        //outline.render(cam.combined());

        //TODO
        /*
        Ptile tile = outline.getTile(cam.getPickRay(Core.input.mouseX(), Core.input.mouseY()));
        if(tile != null){
            for(int i = 0; i < tile.corners.length; i++){
                batch.color(1f, 1f, 1f, 0.5f);
                batch.vertex(tile.corners[i].v);
            }
            batch.flush(cam.combined(), Gl.triangleFan);
        }*/

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
