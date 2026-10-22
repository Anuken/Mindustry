package mindustry.server;

import arc.files.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;

import java.net.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerHostTests extends ServerTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("host_a");
        ServerHarness.createMap("host_b");
    }

    static String mapName(){
        return ServerHarness.call(() -> state.map.plainName());
    }

    @Test
    void hostsANamedMap(){
        host("host_a");

        assertEquals("host_a", mapName());
        assertTrue(ServerHarness.call(() -> state.rules.waves), "survival has waves");
        assertEquals(Gamemode.survival, ServerHarness.call(() -> netServer.config.lastGamemode));
        assertTrue(canConnect(), "the server should accept connections");
    }

    @Test
    void hostWithGamemode(){
        host("host_a", "sandbox");

        assertTrue(ServerHarness.call(() -> state.rules.infiniteResources));
        assertEquals(Gamemode.sandbox, ServerHarness.call(() -> netServer.config.lastGamemode));
        assertTrue(ServerControl.instance.configFile.readString().contains("sandbox"), "the mode should be saved to config.hjson");
    }

    @Test
    void hostWithoutArgumentsPicksACustomMap(){
        host().assertHas("Randomized next map");

        assertTrue(ServerHarness.call(() -> maps.customMaps().contains(state.map)));
    }

    @Test
    void stopClosesTheServer(){
        host("host_a");
        ServerHarness.command("stop").assertHas("Stopped server");

        assertTrue(ServerHarness.call(() -> state.isMenu()));
        ServerHarness.waitUntilSeconds("the port to close", () -> !canConnect(), 5);
    }

    @Test
    void canHostAgainAfterStopping(){
        host("host_a");
        ServerHarness.command("stop");
        ServerHarness.waitUntilSeconds("the port to close", () -> !canConnect(), 5);

        host("host_b");

        assertEquals("host_b", mapName());
        assertTrue(canConnect());
    }

    @Test
    void hostingTwiceIsRejected(){
        host("host_a");
        ServerHarness.command("host host_b").expectError("Already hosting");

        assertEquals("host_a", mapName());
    }

    @Test
    void unknownMapAndModeAreRejected(){
        ServerHarness.command("host nosuchmap").expectError("No map with name");
        ServerHarness.command("host host_a nosuchmode").expectError("No gamemode");

        assertTrue(ServerHarness.call(() -> state.isMenu()));
    }

    @Test
    void failsCleanlyWhenThePortIsTaken() throws Exception{
        try(ServerSocket blocker = new ServerSocket(ServerHarness.port())){
            ServerHarness.command("host host_a").expectError("already in use");
        }
    }

    @Test
    void reloadmapsFindsNewFiles(){
        Fi source = ServerHarness.createMap("host_copy_source").file;
        source.copyTo(customMapDirectory.child("host_copy.msav"));

        ServerHarness.command("reloadmaps").assertHas("new map(s) found");
    }
}
