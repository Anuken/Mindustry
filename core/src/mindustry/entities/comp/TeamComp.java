package mindustry.entities.comp;

import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.state;

@Component
abstract class TeamComp implements Posc{
    @Import float x, y;

    Team team = Team.derelict;

    public boolean cheating(){
        return team.rules().cheat;
    }

    public @Nullable Tilec core(){
        return team.core();
    }

    public @Nullable Tilec closestCore(){
        return state.teams.closestCore(x, y, team);
    }

    public @Nullable Tilec closestEnemyCore(){
        return state.teams.closestEnemyCore(x, y, team);
    }
}
