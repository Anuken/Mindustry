package mindustry.ai.ai.fma.patterns;

import arc.math.*;
import mindustry.ai.ai.fma.*;
import mindustry.ai.ai.utils.*;

/**
 * The defensive circle posts members around the circumference of a circle, so their backs are to the center of the circle. The
 * circle can consist of any number of members. Although a huge number of members might look silly, this implementation doesn't
 * put any fixed limit.
 * @author davebaol
 */
public class DefensiveCircleFormationPattern implements FormationPattern{
    /** The number of slots currently in the pattern. */
    int numberOfSlots;

    /** The radius of one member. This is needed to determine how close we can pack a given number of members around circle. */
    float memberRadius;

    /**
     * Creates a {@code DefensiveCircleFormationPattern}
     */
    public DefensiveCircleFormationPattern(float memberRadius){
        this.memberRadius = memberRadius;
    }

    @Override
    public void setNumberOfSlots(int numberOfSlots){
        this.numberOfSlots = numberOfSlots;
    }

    @Override
    public Location calculateSlotLocation(Location outLocation, int slotNumber){
        if(numberOfSlots > 1){
            // Place the slot around the circle based on its slot number
            float angleAroundCircle = (Mathf.PI2 * slotNumber) / numberOfSlots;

            // The radius depends on the radius of the member,
            // and the number of members in the circle:
            // we want there to be no gap between member's shoulders.
            float radius = memberRadius / (float)Math.sin(Math.PI / numberOfSlots);

            // Fill location components based on the angle around circle.
            outLocation.angleToVector(outLocation.getPosition(), angleAroundCircle).scl(radius);

            // The members should be facing out
            outLocation.setOrientation(angleAroundCircle);
        }else{
            outLocation.getPosition().setZero();
            outLocation.setOrientation(Mathf.PI2 * slotNumber);
        }

        // Return the slot location
        return outLocation;
    }

    @Override
    public boolean supportsSlots(int slotCount){
        // In this case we support any number of slots.
        return true;
    }

}
