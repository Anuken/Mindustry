package mindustry.ai.formations;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;

/**
 * A {@code Formation} coordinates the movement of a group of characters so that they retain some group organization. Characters
 * belonging to a formation must implement the {@link FormationMember} interface. At its simplest, a formation can consist of
 * moving in a fixed geometric pattern such as a V or line abreast, but it is not limited to that. Formations can also make use of
 * the environment. Squads of characters can move between cover points using formation steering with only minor modifications, for
 * example.
 * <p>
 * Formation motion is used in team sports games, squad-based games, real-time strategy games, and sometimes in first-person
 * shooters, driving games, and action adventures too. It is a simple and flexible technique that is much quicker to write and
 * execute and can produce much more stable behavior than collaborative tactical decision making.
 * @author davebaol
 */
public class Formation{

    /** A list of slots assignments. */
    public Seq<SlotAssignment> slotAssignments;

    /** The anchor point of this formation. */
    public Vec3 anchor;
    /** The formation pattern */
    public FormationPattern pattern;
    /** The strategy used to assign a member to his slot */
    public SlotAssignmentStrategy slotAssignmentStrategy;
    /** The formation motion moderator */
    public FormationMotionModerator motionModerator;

    private final Vec2 positionOffset;
    private final Mat orientationMatrix = new Mat();

    /** The location representing the drift offset for the currently filled slots. */
    private final Vec3 driftOffset;

    /**
     * Creates a {@code Formation} for the specified {@code pattern} using a {@link FreeSlotAssignmentStrategy} and no motion
     * moderator.
     * @param anchor the anchor point of this formation, Cannot be {@code null}.
     * @param pattern the pattern of this formation
     * @throws IllegalArgumentException if the anchor point is {@code null}
     */
    public Formation(Vec3 anchor, FormationPattern pattern){
        this(anchor, pattern, new FreeSlotAssignmentStrategy(), null);
    }

    /**
     * Creates a {@code Formation} for the specified {@code pattern} and {@code slotAssignmentStrategy} using no motion moderator.
     * @param anchor the anchor point of this formation, Cannot be {@code null}.
     * @param pattern the pattern of this formation
     * @param slotAssignmentStrategy the strategy used to assign a member to his slot
     * @throws IllegalArgumentException if the anchor point is {@code null}
     */
    public Formation(Vec3 anchor, FormationPattern pattern, SlotAssignmentStrategy slotAssignmentStrategy){
        this(anchor, pattern, slotAssignmentStrategy, null);
    }

    /**
     * Creates a {@code Formation} for the specified {@code pattern}, {@code slotAssignmentStrategy} and {@code moderator}.
     * @param anchor the anchor point of this formation, Cannot be {@code null}.
     * @param pattern the pattern of this formation
     * @param slotAssignmentStrategy the strategy used to assign a member to his slot
     * @param motionModerator the motion moderator. Can be {@code null} if moderation is not needed
     * @throws IllegalArgumentException if the anchor point is {@code null}
     */
    public Formation(Vec3 anchor, FormationPattern pattern, SlotAssignmentStrategy slotAssignmentStrategy,
                     FormationMotionModerator motionModerator){
        if(anchor == null) throw new IllegalArgumentException("The anchor point cannot be null");
        this.anchor = anchor;
        this.pattern = pattern;
        this.slotAssignmentStrategy = slotAssignmentStrategy;
        this.motionModerator = motionModerator;

        this.slotAssignments = new Seq<>();
        this.driftOffset = new Vec3();
        this.positionOffset = new Vec2(anchor.x, anchor.y).cpy();
    }

    /** Updates the assignment of members to slots */
    public void updateSlotAssignments(){
        pattern.slots = slotAssignments.size;

        // Apply the strategy to update slot assignments
        slotAssignmentStrategy.updateSlotAssignments(slotAssignments);

        // Set the newly calculated number of slots
        pattern.slots = slotAssignmentStrategy.calculateNumberOfSlots(slotAssignments);

        // Update the drift offset if a motion moderator is set
        if(motionModerator != null) motionModerator.calculateDriftOffset(driftOffset, slotAssignments, pattern);
    }

    /**
     * Changes the pattern of this formation and updates slot assignments if the number of member is supported by the given
     * pattern.
     * @param pattern the pattern to set
     * @return {@code true} if the pattern has effectively changed; {@code false} otherwise.
     */
    public boolean changePattern(FormationPattern pattern){
        // Find out how many slots we have occupied
        int occupiedSlots = slotAssignments.size;

        // Check if the pattern supports one more slot
        if(pattern.supportsSlots(occupiedSlots)){
           this.pattern = pattern;

            // Update the slot assignments and return success
            updateSlotAssignments();

            return true;
        }

        return false;
    }

    /** Much more efficient than adding a single member.
     * @return number of members added. */
    public int addMembers(Iterable<? extends FormationMember> members){
        int added = 0;
        for(FormationMember member : members){
            if(pattern.supportsSlots(slotAssignments.size + 1)){
                slotAssignments.add(new SlotAssignment(member, slotAssignments.size));
                added ++;
            }
        }

        updateSlotAssignments();
        return added;
    }

    /**
     * Adds a new member to the first available slot and updates slot assignments if the number of member is supported by the
     * current pattern.
     * @param member the member to add
     * @return {@code false} if no more slots are available; {@code true} otherwise.
     */
    public boolean addMember(FormationMember member){

        // Check if the pattern supports one more slot
        if(pattern.supportsSlots(slotAssignments.size + 1)){
            // Add a new slot assignment
            slotAssignments.add(new SlotAssignment(member, slotAssignments.size));

            // Update the slot assignments and return success
            updateSlotAssignments();
            return true;
        }

        return false;
    }

    /**
     * Removes a member from its slot and updates slot assignments.
     * @param member the member to remove
     */
    public void removeMember(FormationMember member){
        // Find the member's slot
        int slot = findMemberSlot(member);

        // Make sure we've found a valid result
        if(slot >= 0){
            // Remove the slot
            // slotAssignments.removeIndex(slot);
            slotAssignmentStrategy.removeSlotAssignment(slotAssignments, slot);

            // Update the assignments
            updateSlotAssignments();
        }
    }

    private int findMemberSlot(FormationMember member){
        for(int i = 0; i < slotAssignments.size; i++){
            if(slotAssignments.get(i).member == member) return i;
        }
        return -1;
    }

    // debug
    public SlotAssignment getSlotAssignmentAt(int index){
        return slotAssignments.get(index);
    }

    // debug
    public int getSlotAssignmentCount(){
        return slotAssignments.size;
    }

    /** Writes new slot locations to each member */
    public void updateSlots(){
        positionOffset.set(anchor);
        float orientationOffset = anchor.z;
        if(motionModerator != null){
            positionOffset.sub(driftOffset);
            orientationOffset -= driftOffset.z;
        }

        // Get the orientation of the anchor point as a matrix
        orientationMatrix.idt().rotate(anchor.z);

        // Go through each member in turn
        for(int i = 0; i < slotAssignments.size; i++){
            SlotAssignment slotAssignment = slotAssignments.get(i);

            // Retrieve the location reference of the formation member to calculate the new value
            Vec3 relativeLoc = slotAssignment.member.formationPos();
            float z = relativeLoc.z;

            // Ask for the location of the slot relative to the anchor point
            pattern.calculateSlotLocation(relativeLoc, slotAssignment.slotNumber);

            // Transform it by the anchor point's position and orientation
            relativeLoc.mul(orientationMatrix);

            // Add the anchor and drift components
            relativeLoc.add(positionOffset.x, positionOffset.y, 0);
            relativeLoc.z = z + orientationOffset;
        }

        // Possibly reset the anchor point if a moderator is set
        if(motionModerator != null){
            motionModerator.updateAnchorPoint(anchor);
        }
    }
}
