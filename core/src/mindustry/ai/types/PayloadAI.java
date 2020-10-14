package mindustry.ai.types;

import arc.math.geom.*;
import mindustry.entities.*;
import mindustry.entities.units.UnitCommand;
import mindustry.gen.*;
import mindustry.world.blocks.*;

public class PayloadAI extends FlyingAI{

    @Override
    public void updateMovement(){
        if(target instanceof Building){
            Vec2 unitPos = new Vec2(unit.x, unit.y);
            Vec2 targetPos = new Vec2(((Building) target).x, ((Building) target).y);
            if(unitPos.dst(targetPos) <= (unit.type().range < (Float.MAX_VALUE * 0.96f) ? unit.type().range * 0.45f : 10f)) {
                ((Payloadc) unit).dropLastPayload();
            }
        }

        if(target != null){
            moveTo(target, 10f);

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
    }
}
