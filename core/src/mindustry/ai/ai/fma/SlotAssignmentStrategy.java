package mindustry.ai.ai.fma;

import arc.struct.*;

/**
 * This interface defines how each {@link FormationMember} is assigned to a slot in the {@link Formation}.
 * @author davebaol
 */
public interface SlotAssignmentStrategy{

    /** Updates the assignment of members to slots */
    void updateSlotAssignments(Array<SlotAssignment> assignments);

    /** Calculates the number of slots from the assignment data. */
    int calculateNumberOfSlots(Array<SlotAssignment> assignments);

    /** Removes the slot assignment at the specified index. */
    void removeSlotAssignment(Array<SlotAssignment> assignments, int index);

}
