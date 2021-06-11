package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.geom.*;
import arc.util.noise.*;
import mindustry.graphics.*;
import mindustry.type.*;

public class NoiseMesh extends HexMesh{
    Simplex sim;

    public NoiseMesh(Planet planet, int seed, int divisions, Color color, float radius, int octaves, float persistence, float scale, float mag){
        this.planet = planet;
        this.sim = new Simplex(planet.id + seed);
        this.shader = Shaders.planet;
        this.mesh =  MeshBuilder.buildHex(new HexMesher(){
            @Override
            public float getHeight(Vec3 position){
                return (float)sim.octaveNoise3D(octaves, persistence, scale, 5f + position.x, 5f + position.y, 5f + position.z) * mag;
            }

            @Override
            public Color getColor(Vec3 position){
                return color;
            }
        }, divisions, false, radius, 0.2f);
    }
}
