package mindustry.entities.comp;

import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

@Component
abstract class TeamComp implements Posc{
    @Import float x, y;

    Team team = Team.derelict;

    public boolean cheating(){
        return team.rules().cheat;
    }

    @Nullable
    public Building core(){
        return team.core();
    }

    @Nullable
    public Building closestCore(){
        return state.teams.closestCore(x, y, team);
    }

    @Nullable
    public Building closestEnemyCore(){
        return state.teams.closestEnemyCore(x, y, team);
    }
}
