package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class CommandAI extends AIController{
    private static final float localInterval = 30f;
    private static final Vec2 vecOut = new Vec2();

    public @Nullable Vec2 targetPos;
    public @Nullable Teamc attackTarget;

    private int pathId = -1;
    private Seq<Unit> local = new Seq<>(false);

    @Override
    public void updateUnit(){
        updateVisuals();
        updateTargeting();

        if(attackTarget != null && invalid(attackTarget)){
            attackTarget = null;
            targetPos = null;
        }

        if(targetPos != null){
            if(timer.get(timerTarget3, localInterval)){
                local.clear();
                float size = unit.hitSize * 3f;
                unit.team.data().tree().intersect(unit.x - size / 2f, unit.y - size/2f, size, size, local);
            }
        }else{
            //make sure updates are staggered randomly
            timer.reset(timerTarget3, Mathf.random(localInterval));
        }

        if(attackTarget != null){
            if(targetPos == null) targetPos = new Vec2();
            targetPos.set(attackTarget);

            if(unit.isGrounded() && attackTarget instanceof Building build && build.tile.solid() && unit.pathType() != Pathfinder.costLegs){
                Tile best = build.findClosestEdge(unit, Tile::solid);
                if(best != null){
                    targetPos.set(best);
                }
            }
        }

        if(targetPos != null){
            boolean move = true;
            vecOut.set(targetPos);

            if(unit.isGrounded()){
                move = Vars.controlPath.getPathPosition(unit, pathId, targetPos, vecOut);
            }

            float engageRange = unit.type.range - 10f;

            if(move){
                moveTo(vecOut,
                    attackTarget != null && unit.within(attackTarget, engageRange) ? engageRange :
                    unit.isGrounded() ? 0f :
                    attackTarget != null ? engageRange :
                    0f, 100f, false);
            }

            if(unit.isFlying()){
                unit.lookAt(targetPos);
            }else{
                faceTarget();
            }

            if(attackTarget == null && unit.within(targetPos, Math.max(5f, unit.hitSize / 2.5f))){
                targetPos = null;
            }
        }else if(target != null){
            faceTarget();
        }
    }

    @Override
    public boolean keepState(){
        return true;
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return attackTarget == null ? super.findTarget(x, y, range, air, ground) : attackTarget;
    }

    @Override
    public boolean retarget(){
        //retarget instantly when there is an explicit target, there is no performance cost
        return attackTarget != null || timer.get(timerTarget, 20);
    }

    public boolean hasCommand(){
        return targetPos != null;
    }

    public void commandPosition(Vec2 pos){
        targetPos = pos;
        attackTarget = null;
        pathId = Vars.controlPath.nextTargetId();
    }

    public void commandTarget(Teamc moveTo){
        //TODO
        attackTarget = moveTo;
        pathId = Vars.controlPath.nextTargetId();
    }
}
