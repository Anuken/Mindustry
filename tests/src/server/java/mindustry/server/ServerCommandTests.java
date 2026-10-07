package mindustry.server;

import mindustry.content.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerCommandTests extends ServerTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("cmd_map");
        ServerHarness.createMap("cmd_map2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"help", "version", "status", "maps", "maps all", "mods", "rules", "saves", "bans", "admins", "players", "whitelist", "config", "gc"})
    void readOnlyCommandsRunCleanly(String command){
        assertFalse(ServerHarness.command(command).lines.isEmpty(), command + " logged nothing");
    }

    @Test
    void helpDescribesACommand(){
        ServerHarness.command("help host").assertHas("host");
    }

    @Test
    void statusReflectsHosting(){
        ServerHarness.command("status").assertHas("server closed");
        host("cmd_map");
        ServerHarness.command("status").assertHas("Playing on map").assertHas("Cmd Map");
    }

    @Test
    void mapsListsCustomAndDefaultMaps(){
        ServerHarness.command("maps").assertHas("cmd_map");
        ServerHarness.command("maps default").assertHas("Default");
    }

    @Test
    void unknownCommandIsReported(){
        ServerHarness.command("nosuchcommand").expectError("Invalid command");
    }

    @Test
    void wrongArgumentCountIsReported(){
        ServerHarness.command("say").expectError("Too few command arguments");
        ServerHarness.command("stop now").expectError("Too many command arguments");
    }

    @Test
    void typoSuggestionCanBeAccepted(){
        ServerHarness.command("stopp").expectError("Did you mean");
        ServerHarness.command("yes").assertHas("Stopped server");
    }

    @Test
    void yesWithoutSuggestionFails(){
        ServerHarness.command("version");
        ServerHarness.command("yes").expectError("nothing to say yes to");
    }

    @Test
    void gameCommandsNeedAHostedGame(){
        ServerHarness.command("say hi").expectError("Not hosting");
        ServerHarness.command("pause on").expectError("Cannot pause");
        ServerHarness.command("runwave").expectError("Not hosting");
        ServerHarness.command("fillitems").expectError("Not playing");
        ServerHarness.command("kick someone").expectError("Not hosting");
        ServerHarness.command("save slot").expectError("Not hosting");
        ServerHarness.command("gameover").expectError("Not playing");
    }

    @Test
    void sayBroadcasts(){
        host("cmd_map");
        ServerHarness.command("say hello there").assertHas("hello there");
    }

    @Test
    void pauseAndUnpause(){
        host("cmd_map");

        ServerHarness.command("pause on").assertHas("Game paused");
        assertTrue(ServerHarness.call(() -> state.isPaused()));

        ServerHarness.command("pause off").assertHas("Game unpaused");
        assertTrue(ServerHarness.call(() -> state.isPlaying()));
    }

    @Test
    void runwaveAdvancesTheWave(){
        host("cmd_map");
        int before = ServerHarness.call(() -> state.wave);

        ServerHarness.command("runwave").assertHas("Wave spawned");

        assertEquals(before + 1, ServerHarness.call(() -> state.wave));
    }

    @Test
    void fillitemsFillsTheCore(){
        host("cmd_map");

        ServerHarness.command("fillitems").assertHas("Core filled");

        ServerHarness.run(() -> {
            var core = state.teams.cores(Team.sharded).first();
            assertEquals(core.storageCapacity, core.items.get(Items.copper));
        });
        ServerHarness.command("fillitems nosuchteam").expectError("No team");
    }

    @Test
    void nextmapIsValidated(){
        ServerHarness.command("nextmap cmd_map2").assertHas("Next map set");
        ServerHarness.command("nextmap nosuchmap").expectError("No map");
    }

    @Test
    void kickWithNobodyOnline(){
        host("cmd_map");
        ServerHarness.command("kick someone").assertHas("Nobody with that name");
    }
}
