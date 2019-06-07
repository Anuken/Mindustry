import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.Tile;
import org.junit.jupiter.api.*;

import static io.anuke.mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorldTests{
    static Tile[][] tiles;

    @BeforeAll
    static void launchApplication(){
        ApplicationTests.launchApplication();
        world.createTiles(10, 10);
        tiles = world.getTiles();
    }

    @BeforeEach
    void resetWorld(){
        Time.setDeltaProvider(() -> 1f);
        logic.reset();
        state.set(State.menu);
    }

    @Test
    void addDarknessAllSolidMaxDarkness(){
        fillWith(Blocks.rocks.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                assertEquals(4, tiles[x][y].rotation());
            }
        }
    }

    @Test
    void addDarknessAllSyntethicNoDarkness(){
        fillWith(Blocks.copperWall.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                assertEquals(0, tiles[x][y].rotation());
            }
        }
    }

    @Test
    void addDarknessAllNotSolidNoDarkness(){
        fillWith(Blocks.air.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                assertEquals(0, tiles[x][y].rotation());
            }
        }
    }

    @Test
    void addDarknessAllNotFilledNoDarkness(){
        fillWith(Blocks.cliffs.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                assertEquals(0, tiles[x][y].rotation());
            }
        }
    }

    private static void fillWith(short tileID){
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                tiles[x][y] = new Tile(x, y, (short)0, (short)0, tileID);
            }
        }
    }

    @Test
    void addDarknessOneNotSolidMiddleNoDarkness(){
        fillWith(Blocks.rocks.id);
        tiles[5][5] = new Tile(5, 5, (byte)0, (byte)0, Blocks.copperWall.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                byte darkness = tiles[x][y].rotation();
                int distance = Math.abs(x - 5) + Math.abs(y - 5);
                assertEquals(Math.min(Math.max(distance - 1, 0), 4), darkness);
            }
        }
    }

    @Test
    void addDarknessOneNotSolidCornerNoDarkness(){
        fillWith(Blocks.rocks.id);
        tiles[7][7] = new Tile(5, 5, (byte)0, (byte)0, Blocks.copperWall.id);
        world.addDarkness(tiles);

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                byte darkness = tiles[x][y].rotation();
                int distance = Math.abs(x - 7) + Math.abs(y - 7);
                assertEquals(Math.min(Math.max(distance - 1, 0), 4), darkness);
            }
        }
    }
}
