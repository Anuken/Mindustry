package mindustry.ai.formations;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;

public class DistanceAssignmentStrategy implements SlotAssignmentStrategy{
    private final Vec3 vec = new Vec3();
    private final FormationPattern form;

    public DistanceAssignmentStrategy(FormationPattern form){
        this.form = form;
    }

    @Override
    public void updateSlotAssignments(Seq<SlotAssignment> assignments){
        IntSeq slots = IntSeq.range(0, assignments.size);

        for(SlotAssignment slot : assignments){
            int mindex = 0;
            float mcost = Float.MAX_VALUE;

            for(int i = 0; i < slots.size; i++){
                float cost = cost(slot.member, slots.get(i));
                if(cost < mcost){
                    mcost = cost;
                    mindex = i;
                }
            }

            slot.slotNumber = slots.get(mindex);
            slots.removeIndex(mindex);

        }
    }

    @Override
    public int calculateNumberOfSlots(Seq<SlotAssignment> assignments){
        return assignments.size;
    }

    @Override
    public void removeSlotAssignment(Seq<SlotAssignment> assignments, int index){
        assignments.remove(index);
    }

    float cost(FormationMember member, int slot){
        form.calculateSlotLocation(vec, slot);
        return Mathf.dst2(member.formationPos().x, member.formationPos().y, vec.x, vec.y);
    }
}
