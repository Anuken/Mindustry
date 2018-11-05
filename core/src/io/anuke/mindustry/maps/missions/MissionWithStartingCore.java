package io.anuke.mindustry.maps.missions;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.content.blocks.StorageBlocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.state;

public abstract class MissionWithStartingCore extends Mission{


    /**
     * Stores a custom starting location for the core, or null if the default calculation (map center) shall be used
     */
    private final GridPoint2 customStartingPoint;

    /**
     * Default constructor. Missions created this way will have a player starting core in the center of the map.
     */
    MissionWithStartingCore(){
        this.customStartingPoint = null;
    }

    /**
     * Creates a mission with a core on a non-default location.
     * @param xCorePos The x coordinate of the custom core position.
     * @param yCorePos The y coordinate of the custom core position.
     */
    MissionWithStartingCore(int xCorePos, int yCorePos){
        this.customStartingPoint = new GridPoint2(xCorePos, yCorePos);
    }

    /**
     * Generates a player core based on generation parameters.
     * @param gen The map generation parameters.
     * @param team The team to generate the core for.
     */
    public void generateCore(Generation gen, Team team){
        int xPos, yPos;

        if(this.customStartingPoint == null){
            xPos = gen.width/2;
            yPos = gen.height/2;
        }else{
            xPos = this.customStartingPoint.x;
            yPos = this.customStartingPoint.y;
        }

        Tile startingCoreTile = gen.tiles[xPos][yPos];
        startingCoreTile.setBlock(StorageBlocks.core);
        startingCoreTile.setTeam(team);
        state.teams.get(team).cores.add(startingCoreTile);
    }
}
