package mindustry.entities.abilities;

import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;

/** Implemented by abilities that shield their unit from lasers. Ability instances are per-unit copies, so the owner is passed in. */
public interface UnitShieldProvider{
    /** @return maximum distance the shield can reach from the unit's center. */
    float shieldBounds();

    /** @return the first point where the segment hits this shield, stored in a shared vector (copy it), or null if it doesn't. */
    @Nullable Vec2 intersectLaser(Unit unit, float x1, float y1, float x2, float y2, float damage);

    /** Applies a laser hit at x, y. @return how much of the damage was absorbed; anything less than the full damage means the laser continues through. */
    float absorbLaser(Unit unit, float x, float y, float damage);
}
