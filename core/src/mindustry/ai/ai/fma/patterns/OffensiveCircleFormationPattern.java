package mindustry.ai.ai.fma.patterns;

import arc.math.*;
import mindustry.ai.ai.utils.*;

/**
 * The offensive circle posts members around the circumference of a circle, so their fronts are to the center of the circle. The
 * circle can consist of any number of members. Although a huge number of members might look silly, this implementation doesn't
 * put any fixed limit.
 * @author davebaol
 */
public class OffensiveCircleFormationPattern extends DefensiveCircleFormationPattern{

    /**
     * Creates a {@code OffensiveCircleFormationPattern}
     */
    public OffensiveCircleFormationPattern(float memberRadius){
        super(memberRadius);
    }

    @Override
    public Location calculateSlotLocation(Location outLocation, int slotNumber){
        super.calculateSlotLocation(outLocation, slotNumber);
        outLocation.setOrientation(outLocation.getOrientation() + Mathf.PI);
        return outLocation;
    }

}
