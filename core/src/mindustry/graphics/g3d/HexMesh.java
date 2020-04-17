package mindustry.graphics.g3d;

import arc.math.geom.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class HexMesh extends PlanetMesh{

    public HexMesh(Planet planet, int divisions){
        super(planet, MeshBuilder.buildHex(planet.generator, divisions, false, planet.radius, 0.2f), Shaders.planet);
    }

    @Override
    public void preRender(){
        Shaders.planet.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.planet.ambientColor.set(planet.solarSystem.lightColor);
    }
}
