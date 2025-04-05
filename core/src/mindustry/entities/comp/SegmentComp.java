package mindustry.entities.comp;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.async.*;
import mindustry.gen.*;
import mindustry.type.*;

@Component
abstract class SegmentComp implements Posc, Rotc, Hitboxc, Unitc, Segmentc{
    @Import float x, y, rotation;
    @Import UnitType type;
    @Import Vec2 vel;

    transient @Nullable Segmentc parentSegment, childSegment, headSegment;
    transient int segmentIndex;

    int parentId;

    public boolean isHead(){
        return parentSegment == null;
    }

    public void addChild(Unit other){
        if(other == self()) return;

        if(childSegment != null){
            childSegment.parentSegment(null);
        }

        if(other instanceof Segmentc seg){
            if(seg.parentSegment() != null){
                seg.parentSegment().childSegment(null);
            }

            childSegment = seg;
            seg.parentSegment(this);
        }
    }

    @Override
    @Replace
    public boolean ignoreSolids(){
        return isFlying() || parentSegment != null;
    }

    //TODO make it phase through things.

    @Override
    public void update(){
        if(childSegment != null && !childSegment.isValid()){
            childSegment = null;
        }

        if(parentSegment != null && !parentSegment.isValid()){
            parentSegment = null;
        }

        if(parentSegment == null){
            segmentIndex = 0;

            if(childSegment != null){
                headSegment = this;
                childSegment.updateSegment(this, this, 1);
            }
        }

    }

    @Replace
    @Override
    public boolean playerControllable(){
        return type.playerControllable && isHead();
    }

    @Override
    @Replace
    public boolean shouldUpdateController(){
        return isHead();
    }

    @Override
    @Replace
    public boolean moving(){
        if(isHead()){
            return !vel.isZero(0.01f);
        }else{
            return deltaLen() / Time.delta >= 0.01f;
        }
    }

    @Override
    @Replace
    public int collisionLayer(){
        if(parentSegment != null) return -1;
        return type.allowLegStep && type.legPhysicsLayer ? PhysicsProcess.layerLegs : isGrounded() ? PhysicsProcess.layerGround : PhysicsProcess.layerFlying;
    }

    @Replace
    @Override
    public boolean isCommandable(){
        return parentSegment == null && controller() instanceof CommandAI;
    }

    @Override
    public void afterSync(){
        checkParent();
    }

    @Override
    public void afterReadAll(){
        checkParent();
    }

    @Override
    public void beforeWrite(){
        parentId = parentSegment == null ? -1 : parentSegment.id();
    }

    public void checkParent(){
        if(parentId != -1){
            var parent = Groups.unit.getByID(parentId);
            if(parent instanceof Segmentc seg){
                parentSegment = seg;
                seg.childSegment(this);
                return;
            }
            parentId = -1;
        }
        //TODO should this unassign the parent's child too?
        parentSegment = null;
    }

    public void updateSegment(Segmentc head, Segmentc parent, int index){
        rotation = Angles.clampRange(rotation,  parent.rotation(), type.segmentRotationRange);
        segmentIndex = index;
        headSegment = head;

        float headDelta = head.deltaLen();

        //TODO should depend on the head's speed.
        if(headDelta > 0.001f){
            rotation = Mathf.slerpDelta(rotation, parent.rotation(), type.baseRotateSpeed * Mathf.clamp(headDelta / type().speed / Time.delta));
        }

        Vec2 moveVec = Tmp.v1.trns(rotation + 180f, type.segmentSpacing).add(parent).sub(x, y);
        float prefSpeed = type.speed * Time.delta * 9999f;
        move(moveVec.limit(prefSpeed)); //TODO other segments are left behind

        if(childSegment != null){
            childSegment.updateSegment(head, this, index + 1);
        }
    }

}
