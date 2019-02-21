import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.anuke.arc.ApplicationCore;
import io.anuke.arc.backends.headless.HeadlessApplication;
import io.anuke.arc.backends.headless.HeadlessApplicationConfiguration;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.Logic;
import io.anuke.mindustry.core.NetServer;
import io.anuke.mindustry.core.World;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.io.BundleLoader;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.logic;
import static io.anuke.mindustry.Vars.netServer;
import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class WorldTests {
    static Tile[][] tiles;

    @BeforeAll
    static void launchApplication(){
        try{
            boolean[] begins = {false};
            Throwable[] exceptionThrown = {null};
            Log.setUseColors(false);

            ApplicationCore core = new ApplicationCore(){
                @Override
                public void setup(){
                    Vars.init();

                    headless = true;

                    BundleLoader.load();
                    content.load();

                    add(logic = new Logic());
                    add(world = new World());
                    add(netServer = new NetServer());

                    content.initialize(Content::init);
                }

                @Override
                public void init(){
                    super.init();
                    begins[0] = true;
                    world.createTiles(10,10);
                    tiles = world.getTiles();
                }
            };

            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();

            new HeadlessApplication(core, config);

            for(Thread thread : Thread.getAllStackTraces().keySet()){
                if(thread.getName().equals("HeadlessApplication")){
                    thread.setUncaughtExceptionHandler((t, throwable) -> exceptionThrown[0] = throwable);
                    break;
                }
            }

            while(!begins[0]){
                if(exceptionThrown[0] != null){
                    fail(exceptionThrown[0]);
                }
                Thread.sleep(10);
            }
        }catch(Throwable r){
            fail(r);
        }
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
