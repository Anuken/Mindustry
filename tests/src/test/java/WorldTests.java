import static io.anuke.mindustry.Vars.logic;
import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.Tile;

public class WorldTests {
    static Tile[][] tiles;

    @BeforeAll
    static void launchApplication(){
    	ApplicationTests.launchApplication();
    }

    @BeforeEach
    void resetWorld(){
        Time.setDeltaProvider(() ->  1f);
        logic.reset();
        state.set(State.menu);
    }

    @Test
    void addDarkness_allSolid_maxDarkness(){
        fillWith(Blocks.rocks.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                assertEquals(4, tiles[x][y].getRotation());
            }
        }
    }

    @Test
    void addDarkness_allSyntethic_noDarkness(){
        fillWith(Blocks.copperWall.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                assertEquals(0, tiles[x][y].getRotation());
            }
        }
    }

    @Test
    void addDarkness_allNotSolid_noDarkness(){
        fillWith(Blocks.air.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                assertEquals(0, tiles[x][y].getRotation());
            }
        }
    }

    @Test
    void addDarkness_allNotFilled_noDarkness(){
        fillWith(Blocks.cliffs.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                assertEquals(0, tiles[x][y].getRotation());
            }
        }
    }

    @Test
    void addDarkness_oneNotSolidMiddle_noDarkness(){
        fillWith(Blocks.rocks.id);
        tiles[5][5] = new Tile(5, 5, (byte)0, Blocks.copperWall.id, (byte)0, (byte)0);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                byte darkness = tiles[x][y].getRotation();
                int distance = Math.abs(x - 5) + Math.abs(y - 5);
                assertEquals(Math.min(Math.max(distance - 1, 0), 4), darkness);
            }
        }
    }

    @Test
    void addDarkness_oneNotSolidCorner_noDarkness(){
        fillWith(Blocks.rocks.id);
        tiles[7][7] = new Tile(5, 5, (byte)0, Blocks.copperWall.id, (byte)0, (byte)0);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                byte darkness = tiles[x][y].getRotation();
                int distance = Math.abs(x - 7) + Math.abs(y - 7);
                assertEquals(Math.min(Math.max(distance - 1, 0), 4), darkness);
            }
        }
    }

    private static void fillWith(byte tileID){
        for(int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                tiles[x][y] = new Tile(x, y, (byte)0, tileID, (byte)0, (byte)0);
            }
        }
    }
}
