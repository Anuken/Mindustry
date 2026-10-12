package mindustry.game.objectives;

import arc.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Produce a certain amount of a unit. */
public class UnitCountObjective extends MapObjective{
    public UnitType unit = UnitTypes.dagger;
    public int count = 1;

    public UnitCountObjective(UnitType unit, int count){
        this.unit = unit;
        this.count = count;
    }

    public UnitCountObjective(){
    }

    @Override
    public boolean update(){
        return state.rules.defaultTeam.data().countType(unit) >= count;
    }

    @Override
    public String text(){
        return Core.bundle.format("objective.buildunit", count - state.rules.defaultTeam.data().countType(unit), unit.emoji() + " ", unit.localizedName);
    }

    @Override
    public void validate(){
        if(unit == null) unit = UnitTypes.dagger;
    }

    @Override
    public String toString(){
        return "unitCount: " + unit + " x" + count;
    }
}
