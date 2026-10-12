package mindustry.server;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class ServerStartupTests extends ServerTestBase{

    //hides the base class boot
    @BeforeAll
    static void bootServer(){
        ServerHarness.boot("startCommands: \"version,config name FromStartup\"");
    }

    @Test
    void startCommandsRunAtBoot(){
        assertTrue(logged("Found 2 startup commands"));
        assertTrue(logged("Version: Mindustry"));
        assertTrue(logged("name set to FromStartup"));
    }
}
