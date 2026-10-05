package mindustry.server;

import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerRulesTests extends ServerTestBase{

    @BeforeAll
    static void createMaps(){
        ServerHarness.createMap("rules_map");
    }

    static String rulesFile(){
        return dataDirectory.child("rules.hjson").readString();
    }

    @Test
    void listShowsDefaults(){
        ServerHarness.command("rules").assertHas("reactorExplosions");
    }

    @Test
    void defaultRulesApplyToHostedMaps(){
        host("rules_map");
        assertFalse(ServerHarness.call(() -> state.rules.reactorExplosions));
    }

    @Test
    void addChangesLiveRulesAndFile(){
        host("rules_map");
        assertTrue(ServerHarness.call(() -> state.rules.waves));

        ServerHarness.command("rules add waves false").assertHas("Changed rule");

        assertFalse(ServerHarness.call(() -> state.rules.waves));
        assertTrue(rulesFile().contains("waves"));
    }

    @Test
    void addAppliesToTheNextHostedMap(){
        ServerHarness.command("rules add waves false");
        host("rules_map");

        assertFalse(ServerHarness.call(() -> state.rules.waves));
    }

    @Test
    void removeStopsOverridingTheMap(){
        ServerHarness.command("rules add waves false");
        ServerHarness.command("rules remove waves").assertHas("removed");
        assertFalse(rulesFile().contains("waves"));

        host("rules_map");
        assertTrue(ServerHarness.call(() -> state.rules.waves));
    }

    @Test
    void removingUnknownRuleFails(){
        ServerHarness.command("rules remove notARule").expectError("Rule not defined");
    }

    @Test
    void malformedValueIsNotSaved(){
        String before = rulesFile().trim();
        ServerHarness.command("rules add waves {").expectError("Error parsing rule JSON");
        //the command rewrites the file even on failure, so only compare the contents
        assertEquals(before, rulesFile().trim());
    }

    @Test
    void usageErrors(){
        ServerHarness.command("rules add").expectError("Invalid usage");
        ServerHarness.command("rules change waves true").expectError("Either add or remove");
        ServerHarness.command("rules add waves").expectError("Missing last argument");
    }
}
