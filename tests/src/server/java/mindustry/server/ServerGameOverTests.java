package mindustry.server;

import mindustry.game.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerGameOverTests extends ServerTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("go_a");
        ServerHarness.createMap("go_b");
        ServerHarness.createMap("go_c");
    }

    static String mapName(){
        return ServerHarness.call(() -> state.map.plainName());
    }

    /** Waits for the delayed map load that follows a game over, and returns the name of the new map. */
    static String awaitNextMap(){
        ServerHarness.waitUntilSeconds("the next map to load", () -> state.isGame() && !ServerControl.instance.inGameOverWait, 20);
        return mapName();
    }

    @Test
    void gameoverShufflesToAnotherMap(){
        host("go_a");

        CommandResult result = ServerHarness.command("gameover");

        result.assertHas("Game over").assertHas("Selected next map");
        assertTrue(ServerHarness.call(() -> state.gameOver), "the game should be marked as over while waiting");
        assertTrue(ServerHarness.call(() -> ServerControl.instance.inGameOverWait));

        String next = awaitNextMap();
        assertNotEquals("go_a", next);
        assertFalse(ServerHarness.call(() -> state.gameOver), "the new game should not start over");
        assertTrue(canConnect(), "the server should stay open across maps");
    }

    @Test
    void shufflingKeepsGoingAcrossRounds(){
        host("go_a");
        String previous = "go_a";

        for(int i = 0; i < 3; i++){
            ServerHarness.command("gameover");
            String next = awaitNextMap();
            assertNotEquals(previous, next, "round " + i);
            previous = next;
        }
    }

    @Test
    void nextmapOverridesShufflingOnce(){
        host("go_a");
        ServerHarness.command("nextmap go_c");

        ServerHarness.command("gameover");
        assertEquals("go_c", awaitNextMap());

        ServerHarness.command("gameover");
        assertNotEquals("go_c", awaitNextMap(), "the override should be used up");
    }

    @Test
    void gamemodeSurvivesTheMapChange(){
        host("go_a", "sandbox");

        ServerHarness.command("gameover");
        awaitNextMap();

        assertTrue(ServerHarness.call(() -> state.rules.infiniteResources));
    }

    @Test
    void shuffleModeNoneClosesTheServer(){
        ServerHarness.command("config shuffleMode none");
        host("go_a");

        CommandResult result = ServerHarness.command("gameover").assertHas("Game over");

        assertTrue(ServerHarness.call(() -> state.isMenu()));
        assertFalse(result.has("Selected next map"));
        ServerHarness.waitUntilSeconds("the port to close", () -> !canConnect(), 5);
    }

    @Test
    void destroyingTheCoreEndsTheGame(){
        host("go_a");

        ServerHarness.run(() -> state.teams.cores(Team.sharded).copy().each(core -> core.kill()));

        assertNotEquals("go_a", awaitNextMap());
        assertTrue(logged("Game over"));
    }

    @Test
    void gameoverNeedsAMap(){
        ServerHarness.command("gameover").expectError("Not playing a map");
    }
}
