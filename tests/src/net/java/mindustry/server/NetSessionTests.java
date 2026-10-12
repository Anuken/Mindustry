package mindustry.server;

import mindustry.net.Administration.*;
import org.junit.jupiter.api.*;

import java.util.regex.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** What a player's session looks like from both sides: joining, leaving, chatting, being kicked or banned, and surviving a game over. */
public class NetSessionTests extends NetTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("net_a");
        ServerHarness.createMap("net_b");
        ServerHarness.createMap("net_c");
    }

    //kicks and bans are remembered by the server for the whole JVM, and a kicked player is refused for a while
    @BeforeEach
    void clearPunishments(){
        ServerHarness.run(() -> {
            var admins = netServer.admins;
            admins.getBanned().each(info -> admins.unbanPlayerID(info.id));
            admins.getBannedIPs().copy().each(ip -> admins.unbanPlayerIP(ip));
            admins.kickedIPs.clear();
            for(PlayerInfo info : admins.playerInfo.values()){
                info.lastKicked = 0;
            }
        });
    }

    //region join and leave

    @Test
    void joiningAddsThePlayerOnBothSides(){
        host("net_a");
        joinServer();

        String name = ServerHarness.call(() -> state.entities.player.first().name());
        assertEquals(1, ServerHarness.call(() -> state.entities.player.size()));
        assertTrue(ServerHarness.call(() -> net.server()));

        String status = client.net();
        assertTrue(status.contains("client=true") && status.contains("active=true"), status);
        assertEquals("playing", client.stateValue("state"));
        assertEquals(1, clientPlayers(), status);
        assertTrue(status.contains("me=" + name + "#"), "the client should be the player the server knows as '" + name + "': " + status);
        assertEquals(ServerHarness.call(() -> state.map.name()), client.stateValue("map"));
    }

    @Test
    void leavingRemovesThePlayerOnBothSides(){
        host("net_a");
        joinServer();

        client.disconnect();
        ServerHarness.waitUntilSeconds("the player to leave", () -> state.entities.player.isEmpty(), 15);

        String status = client.net();
        assertTrue(status.contains("client=false") && status.contains("active=false"), status);
        assertFalse(client.connected());
        assertTrue(ServerHarness.call(() -> state.isGame()), "the server should keep hosting after the player leaves");
        assertTrue(canConnect());
    }

    @Test
    void aPlayerCanJoinAgainAfterLeaving(){
        host("net_a");

        for(int i = 0; i < 2; i++){
            joinServer();
            assertEquals(1, ServerHarness.call(() -> state.entities.player.size()), "join " + i);
            assertEquals(1, clientPlayers(), "join " + i);

            client.disconnect();
            ServerHarness.waitUntilSeconds("the player to leave", () -> state.entities.player.isEmpty(), 15);
        }
    }

    //endregion
    //region chat

    @Test
    void sayReachesTheClient(){
        host("net_a");
        joinServer();

        ServerHarness.command("say hello-from-server");

        client.waitUntil("the client to receive the server's message", () -> client.chat().contains("hello-from-server"), 15);
        assertTrue(client.chat().contains("Server"), "the message should be marked as coming from the server:\n" + client.chat());
    }

    @Test
    void clientChatReachesTheServer(){
        host("net_a");
        joinServer();
        awaitChatAllowed();

        client.say("hello-from-client");

        ServerHarness.waitUntilSeconds("the server to receive the message", () -> logged("hello-from-client"), 15);
        //the server relays it to everyone, the sender included
        client.waitUntil("the client to see its own message relayed", () -> client.chat().contains("hello-from-client"), 15);
    }

    //endregion
    //region kick and ban

    @Test
    void kickShowsTheReasonOnTheClient(){
        host("net_a");
        joinServer();
        String name = ServerHarness.call(() -> state.entities.player.first().name());

        ServerHarness.command("kick " + name).assertHas("It is done");

        ServerHarness.waitUntilSeconds("the player to be removed", () -> state.entities.player.isEmpty(), 15);
        awaitReasonShown("server.kicked.kick");
        assertFalse(client.connected());
        assertTrue(client.net().contains("client=false"), client.net());
    }

    @Test
    void banBlocksRejoinAndUnbanAllowsIt(){
        host("net_a");
        joinServer();
        String uuid = ServerHarness.call(() -> state.entities.player.first().uuid());

        //banning a connected player kicks them
        ServerHarness.command("ban id " + uuid).assertHas("Banned");
        ServerHarness.waitUntilSeconds("the player to be removed", () -> state.entities.player.isEmpty(), 15);
        awaitReasonShown("server.kicked.banned");
        assertFalse(client.connected());

        //trying again is refused before the player is ever added
        client.menu();
        client.connect(ServerHarness.port());
        awaitReasonShown("server.kicked.banned");
        assertFalse(client.connected());
        assertTrue(ServerHarness.call(() -> state.entities.player.isEmpty()), "a banned player must not be added");
        assertEquals(2, ServerHarness.logLines().stream().filter(l -> l.text().contains("Reason: banned")).count(),
            "the server should have kicked the banned connection twice");

        ServerHarness.command("unban " + uuid).assertHas("Unbanned player");
        //being kicked also starts a cooldown that unban leaves alone, so lift it to test the ban in isolation
        ServerHarness.command("pardon " + uuid).assertHas("Pardoned player");

        client.menu();
        joinServer();
        assertEquals(1, ServerHarness.call(() -> state.entities.player.size()));
        assertTrue(client.connected());
    }

    //endregion
    //region game over

    @Test
    void gameoverKeepsTheClientConnectedAcrossMaps(){
        host("net_a");
        joinServer();
        String before = client.stateValue("map");
        assertEquals("net_a", before);

        ServerHarness.command("gameover").assertHas("Selected next map");

        ServerHarness.waitUntilSeconds("the next map to load", () -> state.isGame() && !ServerControl.instance.inGameOverWait, 20);
        String next = ServerHarness.call(() -> state.map.name());
        assertNotEquals("net_a", next, "the server should have changed maps");

        client.waitUntil("the client to be on " + next, () -> next.equals(client.stateValue("map")) && client.connected(), 60);
        assertNotEquals(before, client.stateValue("map"));

        //the player is back on the server after the world reload, and the connection never dropped
        ServerHarness.waitUntilSeconds("the player to be on the new map", () -> state.entities.player.size() == 1, 30);
        String status = client.net();
        assertTrue(status.contains("client=true") && status.contains("active=true"), status);
        assertEquals(1, clientPlayers(), status);
        assertTrue(canConnect());

        ServerHarness.ticks(5);
        ServerHarness.assertNoErrors();
        client.assertNoErrors();
    }

    //endregion

    /** @return how many players the client knows about. */
    private static int clientPlayers(){
        Matcher matcher = Pattern.compile("players=(\\d+)").matcher(client.net());
        assertTrue(matcher.find(), "no player count in " + client.net());
        return Integer.parseInt(matcher.group(1));
    }

    /** The server ignores chat from connections that are younger than half a second. */
    private static void awaitChatAllowed(){
        ServerHarness.waitUntilSeconds("the connection to be old enough to chat",
            () -> !state.entities.player.isEmpty() && arc.util.Time.timeSinceMillis(state.entities.player.first().con.connectTime) > 600, 10);
    }

    /** Waits for a dialog on the client that shows the text of this bundle key, such as the reason for a kick. */
    private static void awaitReasonShown(String bundleKey){
        String text = client.js("Core.bundle.get(\"" + bundleKey + "\")");

        //dialogs strip color tags, so look for the longest piece of text between them
        String needle = "";
        for(String part : text.split("\\[[^\\]]*\\]|\\R")){
            if(part.strip().length() > needle.length()) needle = part.strip();
        }
        assertFalse(needle.isEmpty(), "no text for bundle key " + bundleKey + ": " + text);

        String search = needle;
        client.waitUntil("a dialog saying '" + search + "' (" + bundleKey + ")", () -> !client.command("find " + search).equals("no matches"), 15);
    }
}
