package mindustry.ai.ai.fma;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.ai.ai.steer.*;
import mindustry.ai.ai.utils.*;

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
    Array<SlotAssignment> slotAssignments;

    /** The anchor point of this formation. */
    protected Location anchor;

    /** The formation pattern */
    protected FormationPattern pattern;

    /** The strategy used to assign a member to his slot */
    protected SlotAssignmentStrategy slotAssignmentStrategy;

    /** The formation motion moderator */
    protected FormationMotionModerator motionModerator;

    private final Vec2 positionOffset;
    private final Mat orientationMatrix = new Mat();

    /** The location representing the drift offset for the currently filled slots. */
    private final Location driftOffset;

    /**
     * Creates a {@code Formation} for the specified {@code pattern} using a {@link FreeSlotAssignmentStrategy} and no motion
     * moderator.
     * @param anchor the anchor point of this formation, usually a {@link Steerable}. Cannot be {@code null}.
     * @param pattern the pattern of this formation
     * @throws IllegalArgumentException if the anchor point is {@code null}
     */
    public Formation(Location anchor, FormationPattern pattern){
        this(anchor, pattern, new FreeSlotAssignmentStrategy(), null);
    }

    /**
     * Creates a {@code Formation} for the specified {@code pattern} and {@code slotAssignmentStrategy} using no motion moderator.
     * @param anchor the anchor point of this formation, usually a {@link Steerable}. Cannot be {@code null}.
     * @param pattern the pattern of this formation
     * @param slotAssignmentStrategy the strategy used to assign a member to his slot
     * @throws IllegalArgumentException if the anchor point is {@code null}
     */
    public Formation(Location anchor, FormationPattern pattern, SlotAssignmentStrategy slotAssignmentStrategy){
        this(anchor, pattern, slotAssignmentStrategy, null);
    }

    /**
     * Creates a {@code Formation} for the specified {@code pattern}, {@code slotAssignmentStrategy} and {@code moderator}.
     * @param anchor the anchor point of this formation, usually a {@link Steerable}. Cannot be {@code null}.
     * @param pattern the pattern of this formation
     * @param slotAssignmentStrategy the strategy used to assign a member to his slot
     * @param motionModerator the motion moderator. Can be {@code null} if moderation is not needed
     * @throws IllegalArgumentException if the anchor point is {@code null}
     */
    public Formation(Location anchor, FormationPattern pattern, SlotAssignmentStrategy slotAssignmentStrategy,
                     FormationMotionModerator motionModerator){
        if(anchor == null) throw new IllegalArgumentException("The anchor point cannot be null");
        this.anchor = anchor;
        this.pattern = pattern;
        this.slotAssignmentStrategy = slotAssignmentStrategy;
        this.motionModerator = motionModerator;

        this.slotAssignments = new Array<>();
        this.driftOffset = anchor.newLocation();
        this.positionOffset = anchor.getPosition().cpy();
    }

    /**
     * Returns the current anchor point of the formation. This can be the location (i.e. position and orientation) of a leader
     * member, a modified center of mass of the members in the formation, or an invisible but steered anchor point for a two-level
     * steering system.
     */
    public Location getAnchorPoint(){
        return anchor;
    }

    /**
     * Sets the anchor point of the formation.
     * @param anchor the anchor point to set
     */
    public void setAnchorPoint(Location anchor){
        this.anchor = anchor;
    }

    /** @return the pattern of this formation */
    public FormationPattern getPattern(){
        return pattern;
    }

    /**
     * Sets the pattern of this formation
     * @param pattern the pattern to set
     */
    public void setPattern(FormationPattern pattern){
        this.pattern = pattern;
    }

    /** @return the slot assignment strategy of this formation */
    public SlotAssignmentStrategy getSlotAssignmentStrategy(){
        return slotAssignmentStrategy;
    }

    /**
     * Sets the slot assignment strategy of this formation
     * @param slotAssignmentStrategy the slot assignment strategy to set
     */
    public void setSlotAssignmentStrategy(SlotAssignmentStrategy slotAssignmentStrategy){
        this.slotAssignmentStrategy = slotAssignmentStrategy;
    }

    /** @return the motion moderator of this formation */
    public FormationMotionModerator getMotionModerator(){
        return motionModerator;
    }

    /**
     * Sets the motion moderator of this formation
     * @param motionModerator the motion moderator to set
     */
    public void setMotionModerator(FormationMotionModerator motionModerator){
        this.motionModerator = motionModerator;
    }

    /** Updates the assignment of members to slots */
    public void updateSlotAssignments(){
        // Apply the strategy to update slot assignments
        slotAssignmentStrategy.updateSlotAssignments(slotAssignments);

        // Set the newly calculated number of slots
        pattern.setNumberOfSlots(slotAssignmentStrategy.calculateNumberOfSlots(slotAssignments));

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
            setPattern(pattern);

            // Update the slot assignments and return success
            updateSlotAssignments();

            return true;
        }

        return false;
    }

    /**
     * Adds a new member to the first available slot and updates slot assignments if the number of member is supported by the
     * current pattern.
     * @param member the member to add
     * @return {@code false} if no more slots are available; {@code true} otherwise.
     */
    public boolean addMember(FormationMember member){
        // Find out how many slots we have occupied
        int occupiedSlots = slotAssignments.size;

        // Check if the pattern supports one more slot
        if(pattern.supportsSlots(occupiedSlots + 1)){
            // Add a new slot assignment
            slotAssignments.add(new SlotAssignment(member, occupiedSlots));

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
        // Find the anchor point
        Location anchor = getAnchorPoint();

        positionOffset.set(anchor.getPosition());
        float orientationOffset = anchor.getOrientation();
        if(motionModerator != null){
            positionOffset.sub(driftOffset.getPosition());
            orientationOffset -= driftOffset.getOrientation();
        }

        // Get the orientation of the anchor point as a matrix
        orientationMatrix.idt().rotateRad(anchor.getOrientation());

        // Go through each member in turn
        for(int i = 0; i < slotAssignments.size; i++){
            SlotAssignment slotAssignment = slotAssignments.get(i);

            // Retrieve the location reference of the formation member to calculate the new value
            Location relativeLoc = slotAssignment.member.getTargetLocation();

            // Ask for the location of the slot relative to the anchor point
            pattern.calculateSlotLocation(relativeLoc, slotAssignment.slotNumber);

            Vec2 relativeLocPosition = relativeLoc.getPosition();

            // Transform it by the anchor point's position and orientation
            //relativeLocPosition.mul(orientationMatrix).add(anchor.position);
            relativeLocPosition.mul(orientationMatrix);

            // Add the anchor and drift components
            relativeLocPosition.add(positionOffset);
            relativeLoc.setOrientation(relativeLoc.getOrientation() + orientationOffset);
        }

        // Possibly reset the anchor point if a moderator is set
        if(motionModerator != null){
            motionModerator.updateAnchorPoint(anchor);
        }
    }
}
