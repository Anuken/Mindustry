package mindustry.world.blocks;

import arc.math.geom.*;
import arc.util.*;

public interface ShieldProvider{
    /** @return whether the shield was able to absorb the explosion; this should apply damage to the shield if true is returned. */
    boolean absorbExplosion(float x, float y, float damage);
    /** @return maximum (not current size!) half-size of the shield square bounding box */
    float getShieldBounds();

    /** @return the first point where the segment hits this shield, stored in a shared vector (copy it), or null if it doesn't. Must not have side effects. */
    default @Nullable Vec2 intersectLaser(float x1, float y1, float x2, float y2, float damage){
        return null;
    }

    /** Applies a laser hit at x, y. @return how much of the damage was absorbed; anything less than the full damage means the laser continues through. */
    default float absorbLaser(float x, float y, float damage){
        return 0f;
    }
}
