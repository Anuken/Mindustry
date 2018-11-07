package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.FortressGenerator;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Bundles;

import static io.anuke.mindustry.Vars.*;

public class BattleMission extends Mission{
    final int spacing = 30;

    @Override
    public String getIcon(){
        return "icon-mission-battle";
    }

    @Override
    public GameMode getMode(){
        return GameMode.noWaves;
    }

    @Override
    public String displayString(){
        return Bundles.get("text.mission.battle");
    }

    @Override
    public Array<GridPoint2> getSpawnPoints(Generation gen){
        return Array.with(new GridPoint2(50, 50), new GridPoint2(gen.width - 1 - spacing, gen.height - 1 - spacing));
    }

    @Override
    public void generate(Generation gen){
        generateCoreAt(gen, 50, 50, defaultTeam);

        if(state.teams.get(defaultTeam).cores.size == 0){
            return;
        }

        Tile core = state.teams.get(defaultTeam).cores.first();
        int enx = gen.width - 1 - spacing;
        int eny = gen.height - 1 - spacing;
        new FortressGenerator().generate(gen, Team.red, core.x, core.y, enx, eny);
    }

    @Override
    public boolean isComplete(){
        for(Team team : Vars.state.teams.enemiesOf(Vars.defaultTeam)){
            if(Vars.state.teams.isActive(team)){
                return false;
            }
        }
        return true;
    }
}
