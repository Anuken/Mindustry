package mindustry.graphics.g3d;

import arc.graphics.*;
import arc.math.geom.*;

/** Defines color and height for a planet mesh. */
public interface HexMesher{

    default float getHeight(Vec3 position){
        return 0f;
    }

    default void getColor(Vec3 position, Color out){

    }

    default void getEmissiveColor(Vec3 position, Color out){

    }

    default boolean isEmissive(){
        return false;
    }

    default boolean skip(Vec3 position){
        return false;
    }
}
