package mindustry.game.objectives;

import arc.*;

import static mindustry.Vars.*;

/** Destroy all enemy core(s). */
public class DestroyCoreObjective extends MapObjective{
    @Override
    public boolean update(){
        return state.rules.waveTeam.cores().size == 0;
    }

    @Override
    public String text(){
        return Core.bundle.get("objective.destroycore");
    }

    @Override
    public String toString(){
        return "destroyCore";
    }
}
