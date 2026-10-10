package mindustry.server;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Base class for tests that need the real server. Boots it once per class, resets it before each test, and fails tests during which it logged an error. */
public abstract class ServerTestBase{

    @BeforeAll
    static void bootServer(){
        ServerHarness.boot();
    }

    @AfterAll
    static void shutdownServer(){
        ServerHarness.shutdown();
    }

    @BeforeEach
    void resetServer(){
        ServerHarness.reset();
    }

    @AfterEach
    void checkErrors(){
        ServerHarness.assertNoErrors();
    }

    /** Runs "host" with the arguments, expects a game to start, and stops the wave timer so that nothing spawns while the test runs. */
    protected static CommandResult host(String... args){
        CommandResult result = ServerHarness.command(args.length == 0 ? "host" : "host " + String.join(" ", args));
        ServerHarness.run(() -> {
            assertTrue(state.isGame(), "'host' did not start a game:\n" + result.output());
            state.rules.waveTimer = false;
        });
        return result;
    }

    /** @return whether something accepts TCP connections on the server's port. */
    protected static boolean canConnect(){
        try(Socket socket = new Socket()){
            socket.connect(new InetSocketAddress("localhost", ServerHarness.port()), 500);
            return true;
        }catch(IOException e){
            return false;
        }
    }

    protected static boolean logged(String text){
        return ServerHarness.logLines().stream().anyMatch(l -> l.text().contains(text));
    }
}
