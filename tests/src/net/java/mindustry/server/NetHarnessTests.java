package mindustry.server;

import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Checks that the two-sided harness itself works: the client boots, joins, and leaves. */
public class NetHarnessTests extends NetTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("net_map");
    }

    @Test
    void clientStartsInTheMenu(){
        assertEquals("menu", client.stateValue("state"));
        assertFalse(client.connected());
    }

    @Test
    void clientJoinsAndLeaves(){
        host("net_map");
        joinServer();

        assertEquals(1, ServerHarness.call(() -> state.entities.player.size()));

        client.disconnect();
        ServerHarness.waitUntilSeconds("the player to leave", () -> state.entities.player.isEmpty(), 15);
        assertFalse(client.connected());
    }
}
