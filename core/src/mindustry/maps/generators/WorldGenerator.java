package mindustry.maps.generators;

import mindustry.core.*;
import mindustry.world.*;

public interface WorldGenerator{
    void generate(World tiles, WorldParams params);

    /** Do not modify tiles here. This is only for specialized configuration. */
    default void postGenerate(World tiles){}
}
