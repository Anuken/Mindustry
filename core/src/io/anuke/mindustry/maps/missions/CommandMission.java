package io.anuke.mindustry.maps.missions;

import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.UnitCommand;

public class CommandMission extends Mission{
    private final UnitCommand command;

    public CommandMission(UnitCommand command){
        this.command = command;
    }

    @Override
    public boolean isComplete(){
        for(BaseUnit unit : Vars.unitGroups[Vars.defaultTeam.ordinal()].all()){
            if(unit.isCommanded() && unit.getCommand() == command){
                return true;
            }
        }
        return false;
    }

    @Override
    public String displayString(){
        return Core.bundle.format("text.mission.command", command.localized());
    }
}
