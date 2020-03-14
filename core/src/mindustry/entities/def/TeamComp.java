package mindustry.entities.def;

import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.state;

@Component
abstract class TeamComp implements Posc{
    @Import float x, y;

    Team team = Team.derelict;

    public @Nullable
    Tilec closestCore(){
        return state.teams.closestCore(x, y, team);
    }
}
