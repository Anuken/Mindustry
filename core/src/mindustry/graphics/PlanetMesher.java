package mindustry.graphics;

import arc.graphics.*;
import arc.math.geom.*;

/** Defines color and height for a planet mesh. */
public interface PlanetMesher{
    float getHeight(Vec3 position);
    Color getColor(Vec3 position);
}
