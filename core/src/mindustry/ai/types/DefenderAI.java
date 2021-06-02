package mindustry.ai.types;

import arc.math.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

public class DefenderAI extends AIController{

    @Override
    public void updateMovement(){
        if(target != null){
            moveTo(target, (target instanceof Sized s ? s.hitSize()/2f * 1.1f : 0f) + unit.hitSize/2f + 15f, 50f);
            unit.lookAt(target);
        }
    }

    @Override
    protected void updateTargeting(){
        if(retarget()) target = findTarget(unit.x, unit.y, unit.range(), true, true);
    }
    
    @Override
    protected Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        //find unit to follow if not in rally mode
        if(command() != UnitCommand.rally){
            //Sort by max health and closer target.
            var result = Units.closest(unit.team, x, y, Math.max(range, 400f), u -> !u.dead() && u.type != unit.type, (u, tx, ty) -> -u.maxHealth + Mathf.dst2(u.x, u.y, tx, ty) / 6400f);
            if(result != null) return result;
        }

        //find rally point
        var block = targetFlag(unit.x, unit.y, BlockFlag.rally, false);
        if(block != null) return block;
        //return core if found
        return unit.closestCore();
    }
}
