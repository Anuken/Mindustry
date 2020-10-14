package mindustry.ai.types;

import arc.math.Angles;
import arc.math.Mathf;
import mindustry.*;
import mindustry.ai.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;

import static mindustry.Vars.state;

public class PayloadAI extends FlyingAI{

    @Override
    public void updateMovement(){
        if(command() == UnitCommand.attack){
            if(target != null) {
                moveTo(target, unit.range() * 0.8f);
                unit.lookAt(target);
            }
        }

        if(command() == UnitCommand.rally){
            moveTo(targetFlag(unit.x, unit.y, BlockFlag.rally, false), 60f);
        }
    }

    @Override
    protected Teamc findTarget(float x, float y, float range, boolean air, boolean ground){
        Teamc result = target(x, y, range, false, true);
        if(result != null) return result;

        result = targetFlag(x, y, BlockFlag.repair, true);
        if(result != null) return result;

        result = targetFlag(x, y, BlockFlag.producer, true);
        if(result != null) return result;

        result = targetFlag(x, y, BlockFlag.turret, true);
        if(result != null) return result;

        return null;
    }

    public void updateUnit(){
        if(target != null){
            ((Payloadc) unit).dropLastPayload();
        }
        updateMovement();
    }
}
