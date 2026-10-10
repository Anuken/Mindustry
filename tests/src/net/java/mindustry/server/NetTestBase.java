package mindustry.server;

import org.junit.jupiter.api.*;

import java.util.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Base class for tests with a real server (in this JVM) and one real client (in a child JVM). The client is started once per class. */
public abstract class NetTestBase extends ServerTestBase{
    protected static DriverClient client;

    //runs after the server has booted, since superclass methods go first
    @BeforeAll
    static void startClient(){
        client = DriverClient.start();
    }

    //runs before the server shuts down, since subclass methods go first
    @AfterAll
    static void stopClient(){
        if(client != null) client.stop();
    }

    //the client has to leave before the server is torn down, otherwise it logs a lost connection
    @Override
    @BeforeEach
    void resetServer(){
        client.disconnect();
        client.menu();
        ServerHarness.waitUntilSeconds("the client to leave", () -> state.entities.player.isEmpty(), 15);

        super.resetServer();
        client.drainErrors();
    }

    @Override
    @AfterEach
    void checkErrors(){
        List<String> server = ServerHarness.drainErrors(), clientErrors = client.drainErrors();
        if(!server.isEmpty() || !clientErrors.isEmpty()){
            fail("Server logged " + server.size() + " error(s), client logged " + clientErrors.size() + ".\n"
                + (server.isEmpty() ? "" : "Server:\n" + String.join("\n", server) + "\n")
                + (clientErrors.isEmpty() ? "" : "Client:\n" + String.join("\n", clientErrors)));
        }
    }

    /** Joins the hosted game and waits until the player exists on both sides. */
    protected static void joinServer(){
        client.connect(ServerHarness.port());
        client.waitUntilConnected(60);
        ServerHarness.waitUntilSeconds("the player to join", () -> !state.entities.player.isEmpty(), 30);
    }
}
