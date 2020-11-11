package mindustry.ai.types;

import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.ConstructBlock.*;

public class RepairAI extends AIController{

    @Override
    protected void updateMovement(){
        if(target instanceof Building){
            boolean shoot = false;

            if(target.within(unit, unit.type.range)){
                unit.aim(target);
                shoot = true;
            }

            unit.controlWeapons(shoot);
        }else if(target == null){
            unit.controlWeapons(false);
        }

        if(target != null){
            if(!target.within(unit, unit.type.range * 0.65f) && target instanceof Building){
                moveTo(target, unit.type.range * 0.65f);
            }

            unit.lookAt(target);
        }
    }

    @Override
    protected void updateTargeting(){
        Building target = Units.findDamagedTile(unit.team, unit.x, unit.y);

        if(target instanceof ConstructBuild) target = null;

        if(target == null){
            super.updateTargeting();
        }else{
            this.target = target;
        }
    }
}
