package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class BattleMission implements Mission{
    private final int difficulty;

    public BattleMission(int difficulty){
        this.difficulty = difficulty;
    }

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
    public void generate(Tile[][] tiles, Sector sector){
        generateCoreAt(tiles, 60, 60, Team.blue);
        generateCoreAt(tiles, tiles.length-1-60, tiles[0].length-1-60, Team.red);
    }

    @Override
    public boolean isComplete(){
        //TODO check all enemy teams, not just the first
        return Vars.state.teams.getTeams(false).first().cores.size == 0;
    }
}
