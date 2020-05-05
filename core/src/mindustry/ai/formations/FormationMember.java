package mindustry.ai.formations;

import arc.math.geom.*;

/**
 * Game characters coordinated by a {@link Formation} must implement this interface. Any {@code FormationMember} has a target
 * location which is the place where it should be in order to stay in formation. This target location is calculated by the
 * formation itself.
 * @author davebaol
 */
public interface FormationMember{
    /** Returns the target location of this formation member. */
    Vec3 formationPos();
}
