package io.anuke.mindustry.maps.goals;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.Sector;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

public class BattleMission implements Mission{

    @Override
    public void generate(Tile[][] tiles, Sector sector){
        int x = Mathf.randomSeed(sector.getSeed(), 1, tiles.length - 2);
        int y = Mathf.randomSeed(sector.getSeed(), 1, tiles[0].length - 2);

        tiles[x][y].setBlock(StorageBlocks.core);
        tiles[x][y].setTeam(Team.red);
    }

    @Override
    public boolean isComplete(){
        //TODO check all enemy teams, not just the first
        return Vars.state.teams.getTeams(false).first().cores.size == 0;
    }
}
