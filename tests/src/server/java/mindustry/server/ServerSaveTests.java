package mindustry.server;

import arc.files.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerSaveTests extends ServerTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("save_map");
    }

    static Fi slot(String name){
        return saveDirectory.child(name + "." + saveExtension);
    }

    static void saveAs(String name){
        ServerHarness.command("save " + name);
        //saving is posted to the next update
        ServerHarness.ticks(2);
        assertTrue(SaveIO.isSaveValid(slot(name)), "save " + name + " should be valid");
    }

    static void waitForGame(){
        ServerHarness.waitUntil("a game to be running", () -> state.isGame(), 120);
    }

    static long autosaveCount(){
        return saveDirectory.findAll(f -> f.name().startsWith("auto_")).size;
    }

    @Test
    void saveAndLoadRestoresTheWorld(){
        host("save_map");
        int[] wall = ServerHarness.call(() -> {
            var core = state.teams.cores(Team.sharded).first();
            Tile tile = state.world.tile(core.tile.x + 6, core.tile.y);
            assertNotNull(tile);
            tile.setBlock(Blocks.copperWall, Team.sharded);
            state.wave = 17;
            core.items.set(Items.copper, 1234);
            return new int[]{tile.x, tile.y};
        });

        saveAs("roundtrip");
        ServerHarness.command("saves").assertHas("roundtrip");
        ServerHarness.command("stop");
        ServerHarness.command("load roundtrip");
        waitForGame();

        ServerHarness.run(() -> {
            assertEquals("save_map", state.map.plainName());
            assertEquals(17, state.wave);
            assertEquals(1234, state.teams.cores(Team.sharded).first().items.get(Items.copper));
            Tile tile = state.world.tile(wall[0], wall[1]);
            assertEquals(Blocks.copperWall, tile.block());
            assertEquals(Team.sharded, tile.team());
        });
        assertTrue(canConnect(), "loading a save should open the server");
    }

    @Test
    void saveCanEmbedAssets(){
        host("save_map");
        ServerHarness.command("save embedded true");
        ServerHarness.ticks(2);

        assertTrue(SaveIO.isSaveValid(slot("embedded")));
    }

    @Test
    void loadingMissingSaveFails(){
        ServerHarness.command("load nosuchslot").expectError("No (valid) save data");
    }

    @Test
    void loadingWhileHostingFails(){
        host("save_map");
        saveAs("busy");

        ServerHarness.command("load busy").expectError("Already hosting");
    }

    @Test
    void autosavesPeriodically(){
        ServerHarness.command("config autosaveSpacing 1");
        ServerHarness.command("config autosave on");
        host("save_map");

        ServerHarness.waitUntil("an autosave", () -> autosaveCount() > 0, 3000);

        assertTrue(logged("Autosave completed"));
    }

    @Test
    void autosaveRotationKeepsTheConfiguredAmount(){
        host("save_map");
        saveAs("template");
        for(int i = 0; i < 3; i++){
            Fi copy = saveDirectory.child("auto_old_" + i + "." + saveExtension);
            slot("template").copyTo(copy);
            copy.file().setLastModified(1_000_000L * (i + 1));
        }

        ServerHarness.command("config autosaveAmount 2");
        ServerHarness.command("config autosaveSpacing 1");
        ServerHarness.command("config autosave on");
        ServerHarness.waitUntil("an autosave", () -> logged("Autosave completed"), 3000);

        assertEquals(2, autosaveCount());
        assertFalse(saveDirectory.child("auto_old_0." + saveExtension).exists(), "the oldest autosave should be deleted");
        assertFalse(saveDirectory.child("auto_old_1." + saveExtension).exists());
    }

    @Test
    void loadautosaveLoadsTheNewestOne(){
        host("save_map");
        ServerHarness.run(() -> state.wave = 3);
        saveAs("older");
        ServerHarness.run(() -> state.wave = 9);
        saveAs("newer");

        slot("older").copyTo(saveDirectory.child("auto_a." + saveExtension));
        slot("newer").copyTo(saveDirectory.child("auto_b." + saveExtension));
        saveDirectory.child("auto_a." + saveExtension).file().setLastModified(1000);
        saveDirectory.child("auto_b." + saveExtension).file().setLastModified(2000);
        ServerHarness.command("stop");

        ServerHarness.command("loadautosave");
        waitForGame();

        assertEquals(9, ServerHarness.call(() -> state.wave), "should load the most recently modified autosave");
    }

    @Test
    void loadautosaveWithNoneFails(){
        ServerHarness.command("loadautosave").expectError("No auto-saves found");
    }
}
