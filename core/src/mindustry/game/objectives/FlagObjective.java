package mindustry.game.objectives;

import arc.*;
import arc.util.*;
import mindustry.game.objectives.MapObjectives.*;

import static mindustry.Vars.*;

/** Wait until a logic flag is set. */
public class FlagObjective extends MapObjective{
    public String flag = "flag";
    public @Multiline String text;

    public FlagObjective(String flag, String text){
        this.flag = flag;
        this.text = text;
    }

    public FlagObjective(){
    }

    @Override
    public boolean update(){
        return state.rules.objectiveFlags.contains(flag);
    }

    @Nullable
    @Override
    public String text(){
        if(text == null) return null;

        if(text.startsWith("@")){
            if(state.mapLocales.containsProperty(text.substring(1))) return state.mapLocales.getProperty(text.substring(1));
            return Core.bundle.get(text.substring(1));
        }else{
            return text;
        }
    }

    @Override
    public String toString(){
        return "flag: " + flag;
    }
}
