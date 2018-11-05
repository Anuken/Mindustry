package io.anuke.mindustry.maps.missions;

import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.state;

public abstract class MissionWithStartingCore extends Mission{

    /**
     * Retrieves a tile for the starting core based on the generation parameters.
     */
    @FunctionalInterface
    public interface StartingCorePositionRetriever{
        Tile getCoreTile(Generation gen, Team team);
    }

    /**
     * Stores a function which calculates the position of the starting core.
     */
    private final StartingCorePositionRetriever startingCorePositionRetriever;

    /**
     * Default constructor. Missions created this way will have a player starting core in the center of the map.
     */
    MissionWithStartingCore(){
        this((gen, team) -> gen.tiles[gen.width/2][gen.height/2]);
    }

    /**
     * Creates a mission with a core on a non-default location.
     * @param startingCorePositionRetriever
     */
    MissionWithStartingCore(StartingCorePositionRetriever startingCorePositionRetriever){
        this.startingCorePositionRetriever = startingCorePositionRetriever;
    }

    /**
     * Generates a player core based on generation parameters.
     * @param gen The map generation parameters.
     * @param team The team to generate the core for.
     */
    public void generateCore(Generation gen, Team team){
        Tile startingCoreTile = startingCorePositionRetriever.getCoreTile(gen, team);
        startingCoreTile.setBlock(StorageBlocks.core);
        startingCoreTile.setTeam(team);
        state.teams.get(team).cores.add(startingCoreTile);
    }
}
