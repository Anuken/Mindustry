package mindustry.ai.types;

import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;

public class CommandAI extends AIController{
    public @Nullable Vec2 targetPos;

    @Override
    public void updateUnit(){
        //TODO

        if(targetPos != null){
            //if(unit.isFlying()){
                moveTo(targetPos, 5f);
            //}

            if(unit.isFlying()){
                unit.lookAt(targetPos);
            }else{
                faceTarget();
            }
        }
    }

    public void commandPosition(Vec2 pos){
        targetPos = pos;
    }

    public void commandTarget(Teamc moveTo){
        //TODO
    }
}
