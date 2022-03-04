package mindustry.ai.types;

import arc.math.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class DefenderAI extends AIController{

    @Override
    public void updateMovement(){
        unloadPayloads();

        if(target != null){
            moveTo(target, (target instanceof Sized s ? s.hitSize()/2f * 1.1f : 0f) + unit.hitSize/2f + 15f, 50f);
            unit.lookAt(target);
        }
    }

    @Override
    public void updateTargeting(){
        if(retarget()) target = findTarget(unit.x, unit.y, unit.range(), true, true);
    }
    
    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){

        //Sort by max health and closer target.
        var result = Units.closest(unit.team, x, y, Math.max(range, 400f), u -> !u.dead() && u.type != unit.type, (u, tx, ty) -> -u.maxHealth + Mathf.dst2(u.x, u.y, tx, ty) / 6400f);
        if(result != null) return result;

        //return core if found
        var core = unit.closestCore();
        if(core != null) return core;

        //for enemies, target the enemy core.
        if(state.rules.waves && unit.team == state.rules.waveTeam){
            return unit.closestEnemyCore();
        }

        return null;
    }
}
