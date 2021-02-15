package mindustry.ai.types;

import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.ConstructBlock.*;

public class RepairAI extends AIController{
    public static float retreatDst = 160f, fleeRange = 310f, retreatDelay = Time.toSeconds * 3f;

    @Nullable Teamc avoid;
    float retreatTimer;

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
            if(!target.within(unit, unit.type.range * 0.65f) && target instanceof Building b && b.team == unit.team){
                moveTo(target, unit.type.range * 0.65f);
            }

            unit.lookAt(target);
        }

        //not repairing
        if(!(target instanceof Building)){
            if(timer.get(timerTarget4, 40)){
                avoid = target(unit.x, unit.y, fleeRange, true, true);
            }

            if((retreatTimer += Time.delta) >= retreatDelay){
                //fly away from enemy when not doing anything
                if(avoid != null){
                    var core = unit.closestCore();
                    if(core != null && !unit.within(core, retreatDst)){
                        moveTo(core, retreatDst);
                    }
                }
            }
        }else{
            retreatTimer = 0f;
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
