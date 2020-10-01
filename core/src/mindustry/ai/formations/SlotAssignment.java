package mindustry.ai.formations;


/**
 * A {@code SlotAssignment} instance represents the assignment of a single {@link FormationMember} to its slot in the
 * {@link Formation}.
 * @author davebaol
 */
public class SlotAssignment{
    public FormationMember member;
    public int slotNumber;

    /**
     * Creates a {@code SlotAssignment} for the given {@code member}.
     * @param member the member of this slot assignment
     */
    public SlotAssignment(FormationMember member){
        this(member, 0);
    }

    /**
     * Creates a {@code SlotAssignment} for the given {@code member} and {@code slotNumber}.
     * @param member the member of this slot assignment
     */
    public SlotAssignment(FormationMember member, int slotNumber){
        this.member = member;
        this.slotNumber = slotNumber;
    }
}
