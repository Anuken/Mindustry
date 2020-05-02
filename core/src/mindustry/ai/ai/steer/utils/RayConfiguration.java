package mindustry.ai.ai.steer.utils;

import mindustry.ai.ai.utils.*;

/**
 * A {@code RayConfiguration} is a collection of rays typically used for collision avoidance.
 * @author davebaol
 */
public interface RayConfiguration{
    Ray[] updateRays();
}
