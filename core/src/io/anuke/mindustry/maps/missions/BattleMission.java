package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.ucore.util.Bundles;

public class BattleMission extends Mission{
    private final static int coreX = 60, coreY = 60;

    @Override
    public GameMode getMode(){
        return GameMode.noWaves;
    }

    @Override
    public String displayString(){
        return Bundles.get("text.mission.battle");
    }

    @Override
    public void generate(Generation gen){
        //int enemyX = gen.width-1-coreX, enemyY = gen.height-1-coreX;

        //generateCoreAt(gen, coreX, coreY, Team.blue);
        //generateCoreAt(gen, enemyX, enemyY, Team.red);

        //new FortressGenerator().generate(gen, Team.red, coreX, coreY, enemyX, enemyY);
    }

    @Override
    public void onBegin(){

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

    @Override
    public Array<GridPoint2> getSpawnPoints(Generation gen){
        return Array.with(new GridPoint2(coreX, coreY), new GridPoint2(gen.width - 1 - coreX, gen.height - 1 - coreY));
    }
}
