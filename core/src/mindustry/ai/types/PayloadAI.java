package mindustry.ai.types;

import arc.math.geom.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.world.blocks.*;

public class PayloadAI extends FlyingAI{
    public Teamc healingTarget;

    @Override
    public void updateMovement(){
        if(target instanceof Building){

            if(unit.type().payloadHeal && healingTarget.within(unit, unit.type().range)){
                unit.aim(healingTarget);
                unit.controlWeapons(true, true);
            }

            Vec2 unitPos = new Vec2(unit.x, unit.y);
            Vec2 targetPos = new Vec2(((Building) target).x, ((Building) target).y);
            if(unitPos.dst(targetPos) <= (unit.type().range < (Float.MAX_VALUE * 0.96f) ? unit.type().range * 0.85f : 40f)) {
                ((Payloadc) unit).dropLastPayload();
            }
        }

        if(target != null){
            moveTo(target, 20f);

            unit.lookAt(target);
        }
    }

    @Override
    protected void updateTargeting(){
        target = Units.findEnemyTile(unit.team, unit.x, unit.y, unit.type().range * 10, b -> b.health > 0);

        if(target instanceof ConstructBlock.ConstructBuild) target = null;

        if(unit.hasWeapons()){
            updateWeapons();
        }

        healingTarget = Units.findDamagedTile(unit.team, unit.x, unit.y);

        if(healingTarget instanceof ConstructBlock.ConstructBuild) healingTarget = null;
    }
}
