package mindustry.ai.types;

import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.world.blocks.ConstructBlock.*;

//note that repair AI doesn't attack anything even if it theoretically can
public class RepairAI extends AIController{

    @Override
    protected void updateMovement(){
        boolean shoot = false;

        if(target != null){
            if(!target.within(unit, unit.type().range * 0.8f)){
                moveTo(target, unit.type().range * 0.8f);
            }else{
                unit.aim(target);
                shoot = true;
            }
        }

        unit.controlWeapons(shoot);
    }

    @Override
    protected void updateTargeting(){
        target = Units.findDamagedTile(unit.team, unit.x, unit.y);

        if(target instanceof ConstructBuild) target = null;
    }
}
