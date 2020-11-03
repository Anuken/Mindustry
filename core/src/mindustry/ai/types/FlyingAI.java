package mindustry.ai.types;

import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.Units;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class FlyingAI extends AIController{

    @Override
    public void updateMovement(){
        if(unit instanceof Payloadc && ((Payloadc) unit).hasPayload() && target instanceof Building){
            Vec2 unitPos = new Vec2(unit.x, unit.y);
            Vec2 targetPos = new Vec2(((Building) target).x, ((Building) target).y);

            if(unitPos.dst(targetPos) <= (unit.type().range < (Float.MAX_VALUE * 0.96f) ? unit.type().range : 40f)) {
                ((Payloadc) unit).dropLastPayload();
            }
        }

        if(target != null && (unit instanceof Payloadc || unit.hasWeapons()) && command() == UnitCommand.attack){
            if((unit.hasWeapons() && unit.type().weapons.first().rotate) || (unit instanceof Payloadc && ((Payloadc) unit).hasPayload())){
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
            if (result != null) return result;

            if (ground) result = targetFlag(x, y, BlockFlag.producer, true);
            if (result != null) return result;

            if (ground) result = targetFlag(x, y, BlockFlag.turret, true);
            if (result != null) return result;
        }

        return null;
    }

    @Override
    protected void updateTargeting(){
        if(unit.hasWeapons()){
            updateWeapons();
        } else {
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

        vec.setLength(unit.type().speed);

        unit.moveAt(vec);
    }
}
