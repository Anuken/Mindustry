package mindustry.ai.types;

import arc.math.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class FlyingAI extends AIController{

    @Override
    public void updateMovement(){
        if(target != null && unit.hasWeapons() && command() == UnitCommand.attack){
            if(unit.type().weapons.first().rotate){
                moveTo(target, unit.range() * 0.8f);
                unit.lookAt(target);
            }else{
                attack(80f);
            }
        }

        if(target == null && command() == UnitCommand.attack && state.rules.waves && unit.team == state.rules.defaultTeam){
            moveTo(getClosestSpawner(), state.rules.dropZoneRadius + 120f);
        }

        if(command() == UnitCommand.rally){
            target = targetFlag(unit.x, unit.y, BlockFlag.rally, false);
            moveTo(target, 60f);
        }
    }

    @Override
    protected Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        Teamc result = target(x, y, range, air, ground);
        if(result != null) return result;

        if(ground) result = targetFlag(x, y, BlockFlag.producer, true);
        if(result != null) return result;

        if(ground) result = targetFlag(x, y, BlockFlag.turret, true);
        if(result != null) return result;

        return null;
    }

    //TODO clean up

    protected void attack(float circleLength){
        vec.set(target).sub(unit);

        float ang = unit.angleTo(target);
        float diff = Angles.angleDist(ang, unit.rotation());

        if(diff > 100f && vec.len() < circleLength){
            vec.setAngle(unit.vel().angle());
        }else{
            vec.setAngle(Mathf.slerpDelta(unit.vel().angle(), vec.angle(), 0.6f));
        }

        vec.setLength(unit.type().speed);

        unit.moveAt(vec);
    }
}
