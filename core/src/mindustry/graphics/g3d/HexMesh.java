package mindustry.graphics.g3d;

import arc.graphics.gl.*;
import arc.math.geom.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class HexMesh extends PlanetMesh{

    public HexMesh(Planet planet, int divisions){
        super(planet, MeshBuilder.buildHex(planet.generator, divisions, false, planet.radius, 0.2f), Shaders.planet);
    }

    public HexMesh(Planet planet, HexMesher mesher, int divisions, Shader shader){
        super(planet, MeshBuilder.buildHex(mesher, divisions, false, planet.radius, 0.2f), shader);
    }

    @Override
    public void preRender(){
        Shaders.planet.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.planet.ambientColor.set(planet.solarSystem.lightColor);
    }
}
