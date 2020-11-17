package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.maps.generators.*;

public class TantrosPlanetGenerator extends PlanetGenerator{
    Color c1 = Color.valueOf("5057a6"), c2 = Color.valueOf("272766"), out = new Color();

    @Override
    public float getHeight(Vec3 position){
        return 0;
    }

    @Override
    public Color getColor(Vec3 position){
        float depth = (float)noise.octaveNoise3D(2, 0.56, 1.7f, position.x, position.y, position.z) / 1.7f;
        return c1.write(out).lerp(c2, Mathf.clamp(Mathf.round(depth, 0.15f))).a(0.6f);
    }
}
