package mindustry.ai.types;

import arc.math.*;
import mindustry.entities.*;
import mindustry.entities.Units.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

public class DefenderAI extends AIController{

    @Override
    public void updateMovement(){
        if(target != null){
            moveTo(target, unit.range(), 5f);
            unit.lookAt(target);
        }else{
            Teamc block = targetFlag(unit.x, unit.y, BlockFlag.rally, false);
            if(block == null) block = unit.closestCore();
            moveTo(block, 60f);
        }
    }

    @Override
    protected void updateTargeting(){
        if(retarget()) target = findTarget(unit.x, unit.y, 0f, true, true);
    }
    
    @Override
    protected Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        //Sort by max health and closer target.
        return Units.closest(unit.team, x, y, u -> !u.dead() && u.type != unit.type, (u, tx, ty) -> -u.maxHealth + Mathf.dst2(u.x, u.y, tx, ty) / 800f);
    }
}
