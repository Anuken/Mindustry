package mindustry.ai.ai.fma;

import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.ai.utils.*;

/**
 * A {@code FormationMotionModerator} moderates the movement of the formation based on the current positions of the members in its
 * slots: in effect to keep the anchor point on a leash. If the members in the slots are having trouble reaching their targets,
 * then the formation as a whole should be held back to give them a chance to catch up.
 * @author davebaol
 */
public abstract class FormationMotionModerator{

    private Location tempLocation;

    /**
     * Update the anchor point to moderate formation motion. This method is called at each frame.
     * @param anchor the anchor point
     */
    public abstract void updateAnchorPoint(Location anchor);

    /**
     * Calculates the drift offset when members are in the given set of slots for the specified pattern.
     * @param centerOfMass the output location set to the calculated drift offset
     * @param slotAssignments the set of slots
     * @param pattern the pattern
     * @return the given location for chaining.
     */
    public Location calculateDriftOffset(Location centerOfMass, Array<SlotAssignment> slotAssignments,
                                         FormationPattern pattern){

        // Clear the center of mass
        centerOfMass.getPosition().setZero();
        float centerOfMassOrientation = 0;

        // Make sure tempLocation is instantiated
        if(tempLocation == null) tempLocation = new VecLocation();

        Vec2 centerOfMassPos = centerOfMass.getPosition();
        Vec2 tempLocationPos = tempLocation.getPosition();

        // Go through each assignment and add its contribution to the center
        float numberOfAssignments = slotAssignments.size;
        for(int i = 0; i < numberOfAssignments; i++){
            pattern.calculateSlotLocation(tempLocation, slotAssignments.get(i).slotNumber);
            centerOfMassPos.add(tempLocationPos);
            centerOfMassOrientation += tempLocation.getOrientation();
        }

        // Divide through to get the drift offset.
        centerOfMassPos.scl(1f / numberOfAssignments);
        centerOfMassOrientation /= numberOfAssignments;
        centerOfMass.setOrientation(centerOfMassOrientation);

        return centerOfMass;
    }

}
