package mindustry.ai.types;

import mindustry.entities.units.*;

public class MissileAI extends AIController{

    //TODO UNPREDICTABLE TARGETING
    @Override
    public void updateMovement(){
        unloadPayloads();

        if(target != null){
            unit.lookAt(target);

            var build = unit.buildOn();

            //kill instantly on building contact
            //TODO kill on target unit contact too
            if(build != null && build == target){
                unit.kill();
            }
        }

        //move forward forever
        unit.moveAt(vec.trns(unit.rotation, unit.speed()));

    }

    @Override
    public boolean retarget(){
        //more frequent retarget. TODO lag?
        return timer.get(timerTarget, 10f);
    }
}
