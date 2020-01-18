package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.geom.*;

public interface PlanetGenerator{
    float getHeight(Vec3 position);
    Color getColor(Vec3 position);
}
