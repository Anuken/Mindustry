package mindustry.ai.types;

import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class CommandAI extends AIController{
    public @Nullable Vec2 targetPos;
    public @Nullable Teamc attackTarget;

    @Override
    public void updateUnit(){
        updateVisuals();
        updateTargeting();

        //TODO

        if(attackTarget != null){
            if(targetPos == null) targetPos = new Vec2();
            targetPos.set(attackTarget);
        }

        if(targetPos != null){
            moveTo(targetPos, attackTarget != null ? unit.type.range - 10f : 5f);

            if(unit.isFlying()){
                unit.lookAt(targetPos);
            }else{
                faceTarget();
            }
        }else if(target != null){
            faceTarget();
        }
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        return attackTarget == null ? super.findMainTarget(x, y, range, air, ground) : attackTarget;
    }

    public void commandPosition(Vec2 pos){
        targetPos = pos;
        attackTarget = null;
    }

    public void commandTarget(Teamc moveTo){
        //TODO
        attackTarget = moveTo;
    }
}
