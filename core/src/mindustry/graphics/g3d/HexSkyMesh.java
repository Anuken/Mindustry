package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.geom.*;
import arc.util.noise.*;
import mindustry.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class HexSkyMesh extends PlanetMesh{

    public HexSkyMesh(Planet planet, float radius, int divisions, Color color, int octaves, float persistence, float scl, float thresh){
        super(planet, MeshBuilder.buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return 1f;
            }

            @Override
            public Color getColor(Vec3 position){
                return color;
            }

            @Override
            public boolean skip(Vec3 position){
                return Simplex.noise3d(planet.id, octaves, persistence, scl, position.x, position.y * 3f, position.z) >= thresh;
            }
        }, divisions, false, planet.radius, radius), Shaders.clouds);
    }

    public HexSkyMesh(){
    }

    @Override
    public void preRender(){
        Shaders.clouds.lightDir.set(planet.solarSystem.position).sub(planet.position).rotate(Vec3.Y, planet.getRotation()).nor();
        Shaders.clouds.ambientColor.set(planet.solarSystem.lightColor);
        Shaders.clouds.alpha = 1f - Vars.ui.planet.planets.orbitAlpha;
    }
}
