package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.geom.*;
import mindustry.world.*;

public interface PlanetGenerator{
    float getHeight(Vec3 position);
    Color getColor(Vec3 position);
    void generate(Vec3 position, TileGen tile);
}
