package mindustry.ai.types;

import mindustry.entities.units.*;

public class MissileAI extends AIController{
    //TODO store 'main' target and use that as a fallback

    //TODO UNPREDICTABLE TARGETING
    @Override
    public void updateMovement(){
        unloadPayloads();

        if(target != null){
            unit.lookAt(target);
        }

        //move forward forever
        unit.moveAt(vec.trns(unit.rotation, unit.speed()));

        var build = unit.buildOn();

        //kill instantly on enemy building contact
        if(build != null && build.team != unit.team){
            unit.kill();
        }
    }

    @Override
    public boolean retarget(){
        //more frequent retarget. TODO won't this lag?
        return timer.get(timerTarget, 10f);
    }
}
