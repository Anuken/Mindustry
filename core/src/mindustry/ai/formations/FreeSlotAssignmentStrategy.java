package mindustry.ai.formations;


import arc.struct.*;

/**
 * {@code FreeSlotAssignmentStrategy} is the simplest implementation of {@link SlotAssignmentStrategy}. It simply go through
 * each assignment in the list and assign sequential slot numbers. The number of slots is just the length of the list.
 * <p>
 * Because each member can occupy any slot this implementation does not support roles.
 * @author davebaol
 */
public class FreeSlotAssignmentStrategy implements SlotAssignmentStrategy{

    @Override
    public void updateSlotAssignments(Seq<SlotAssignment> assignments){
        // A very simple assignment algorithm: we simply go through
        // each assignment in the list and assign sequential slot numbers
        for(int i = 0; i < assignments.size; i++)
            assignments.get(i).slotNumber = i;
    }

    @Override
    public int calculateNumberOfSlots(Seq<SlotAssignment> assignments){
        return assignments.size;
    }

    @Override
    public void removeSlotAssignment(Seq<SlotAssignment> assignments, int index){
        assignments.remove(index);
    }

}
