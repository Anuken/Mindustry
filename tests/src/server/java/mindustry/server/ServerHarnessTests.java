package mindustry.server;

import mindustry.maps.Map;
import org.junit.jupiter.api.*;

import java.net.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks that the harness itself works: boot, commands, error claiming, hosting and resetting. */
public class ServerHarnessTests extends ServerTestBase{

    @Test
    void bootsIdle(){
        assertNotNull(ServerControl.instance);
        assertTrue(ServerHarness.call(() -> state.isMenu()), "a fresh server should not be hosting");
        assertEquals(ServerHarness.port(), ServerHarness.call(() -> netServer.config.port));
    }

    @Test
    void commandsReportOutputAndErrors(){
        ServerHarness.command("version").assertHas("Version");
        ServerHarness.command("nosuchcommand").expectError("Invalid command");
    }

    @Test
    void createdMapCanBeHostedAndStopped(){
        Map map = ServerHarness.createMap("harness_map");
        assertTrue(ServerHarness.call(() -> maps.all().contains(map)), "the map should be in the map list");

        ServerHarness.command("host harness_map").assertHas("Map loaded");
        ServerHarness.waitUntil("the map to start", () -> state.isGame(), 60);
        assertEquals("harness_map", ServerHarness.call(() -> state.map.plainName()));
        assertDoesNotThrow(() -> new Socket("localhost", ServerHarness.port()).close(), "the server should accept connections");

        ServerHarness.command("stop").assertHas("Stopped server");
        assertTrue(ServerHarness.call(() -> state.isMenu()));
    }

    @Test
    void resetStopsHosting(){
        ServerHarness.createMap("harness_map2");
        ServerHarness.command("host harness_map2");
        ServerHarness.command("config name Changed");
        ServerHarness.reset();

        assertTrue(ServerHarness.call(() -> state.isMenu()));
        assertEquals("Server", ServerHarness.call(() -> netServer.config.name));
    }
}
