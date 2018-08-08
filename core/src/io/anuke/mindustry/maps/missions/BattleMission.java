package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.FortressGenerator;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class BattleMission implements Mission{
    private final static int coreX = 60, coreY = 60;
    @Override
    public void display(Table table){
        table.add("$text.mission.battle");
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
    public void generate(Generation gen){
        int enemyX = gen.width-1-coreX, enemyY = gen.height-1-coreX;

        generateCoreAt(gen, coreX, coreY, Team.blue);
        generateCoreAt(gen, enemyX, enemyY, Team.red);

        new FortressGenerator().generate(gen, Team.red, coreX, coreY, enemyX, enemyY);
    }

    @Override
    public boolean isComplete(){
        //TODO check all enemy teams, not just the first
        return Vars.state.teams.getTeams(false).first().cores.size == 0;
    }

    @Override
    public Array<GridPoint2> getSpawnPoints(Generation gen){
        return Array.with(new GridPoint2(coreX, coreY), new GridPoint2(gen.width - 1 - coreX, gen.height - 1 - coreY));
    }
}
