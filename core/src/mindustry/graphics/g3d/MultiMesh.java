package mindustry.graphics.g3d;

import arc.math.geom.*;

public class MultiMesh implements GenericMesh{
    GenericMesh[] meshes;

    public MultiMesh(GenericMesh... meshes){
        this.meshes = meshes;
    }

    @Override
    public void render(PlanetParams params, Mat3D projection, Mat3D transform){
        for(var v : meshes){
            v.render(params, projection, transform);
        }
    }
}
