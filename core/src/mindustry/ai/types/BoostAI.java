package mindustry.ai.types;

import mindustry.ai.*;
import mindustry.entities.units.*;

//not meant to be used outside RTS-AI-controlled units
public class BoostAI extends AIController{

    @Override
    public void updateUnit(){
        if(unit.controller() instanceof CommandAI ai){
            ai.defaultBehavior();
            unit.updateBoosting(true);

            //auto land when near target
            if(ai.attackTarget != null && unit.within(ai.attackTarget, unit.range())){
                unit.command().command(UnitCommand.moveCommand);
            }
        }
    }
}
