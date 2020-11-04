package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.Units;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class FlyingAI extends AIController{

    @Override
    public void updateMovement(){
        boolean hasPayload = unit instanceof Payloadc && ((Payloadc) unit).hasPayload();
        boolean targetBuild = target instanceof Building;
        
        if(hasPayload && targetBuild){
            if(target.within(unit, (unit.type().range < (Float.MAX_VALUE * 0.96f) ? unit.type().range : 40f))){
                ((Payloadc) unit).dropLastPayload();
            }
        }

        if(target != null && (unit instanceof Payloadc || unit.hasWeapons()) && command() == UnitCommand.attack){
            if((unit.hasWeapons() && unit.type().weapons.first().rotate) || hasPayload){
                moveTo(target, unit.hasWeapons() ? unit.range() * 0.8f : 20f);
                unit.lookAt(target);
            }else{
                attack(100f);
            }
        }

        if(target == null && command() == UnitCommand.attack && state.rules.waves && unit.team == state.rules.defaultTeam){
            moveTo(getClosestSpawner(), state.rules.dropZoneRadius + 120f);
        }

        if(command() == UnitCommand.rally){
            moveTo(targetFlag(unit.x, unit.y, BlockFlag.rally, false), 60f);
        }
    }

    @Override
    protected Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        if(unit.hasWeapons()){
            Teamc result = target(x, y, range, air, ground);
            if(result != null) return result;

            if(ground) result = targetFlag(x, y, BlockFlag.generator, true);
            if(result != null) return result;

            if(ground) result = targetFlag(x, y, BlockFlag.turret, true);
            if(result != null) return result;
        }

        return null;
    }

    @Override
    protected void updateTargeting(){
        if(unit.hasWeapons()){
            updateWeapons();
        }else{
            target = Units.findEnemyTile(unit.team, unit.x, unit.y, Math.max(unit.range(), 120f), b -> b.health > 0);
            if(target instanceof ConstructBlock.ConstructBuild) target = null;
        }
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

        vec.setLength(unit.type.speed);

        unit.moveAt(vec);
    }
}
