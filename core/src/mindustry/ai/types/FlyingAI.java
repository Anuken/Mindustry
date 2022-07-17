package mindustry.ai.types;

import arc.math.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//TODO very strange idle behavior sometimes
public class FlyingAI extends AIController{

    @Override
    public void updateMovement(){
        unloadPayloads();

        if(target != null && unit.hasWeapons()){
            if(unit.type.circleTarget){
                circleAttack(120f);
            }else{
                moveTo(target, unit.type.range * 0.4f);
                unit.lookAt(target);
            }
        }

        if(target == null && state.rules.waves && unit.team == state.rules.defaultTeam){
            moveTo(getClosestSpawner(), state.rules.dropZoneRadius + 130f);
        }
    }

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        return findMainTarget(x, y, range, air, ground);
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground){
        var core = targetFlag(x, y, BlockFlag.core, true);

        if(core != null && Mathf.within(x, y, core.getX(), core.getY(), range)){
            return core;
        }

        Teamc result = target(x, y, range, air, ground);
        if(unit.team == state.rules.waveTeam){
            result = target(x, y, range, false, false);
        }

        if(result != null) return result;
        return core;
    }
}
