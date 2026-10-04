package mindustry.game.objectives;

import arc.*;

import static mindustry.Vars.*;

/** Command any unit to do anything. Always compete in headless mode. */
public class CommandModeObjective extends MapObjective{
    @Override
    public boolean update(){
        return headless || control.input.selectedUnits.contains(u -> u.isCommandable() && u.command().hasCommand());
    }

    @Override
    public String text(){
        return Core.bundle.get("objective.command");
    }

    @Override
    public String toString(){
        return "commandMode";
    }
}
