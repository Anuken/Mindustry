package mindustry.graphics.g3d;

import arc.graphics.gl.*;
import mindustry.type.*;

public class ShaderSphereMesh extends PlanetMesh{

    public ShaderSphereMesh(Planet planet, Shader shader, int divisions){
        super(planet, MeshBuilder.buildIcosphere(divisions, planet.radius), shader);
    }

    @Override
    public void preRender(){

    }
}
