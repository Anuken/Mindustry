package mindustry.ai.formations;

import arc.struct.*;

/**
 * This interface defines how each {@link FormationMember} is assigned to a slot in the {@link Formation}.
 * @author davebaol
 */
public interface SlotAssignmentStrategy{

    /** Updates the assignment of members to slots */
    void updateSlotAssignments(Seq<SlotAssignment> assignments);

    /** Calculates the number of slots from the assignment data. */
    int calculateNumberOfSlots(Seq<SlotAssignment> assignments);

    /** Removes the slot assignment at the specified index. */
    void removeSlotAssignment(Seq<SlotAssignment> assignments, int index);

}
