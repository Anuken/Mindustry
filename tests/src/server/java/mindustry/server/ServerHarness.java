package mindustry.server;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.maps.Map;
import mindustry.net.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Boots the real {@link ServerLauncher} in a {@link HeadlessApplication} and lets tests run code on its main thread.
 * <p>
 * Vars / Core are static, so there is one server per JVM; the gradle task forks a JVM per test class.
 * Everything touching the world or net must go through {@link #run}, {@link #call}, {@link #command} or {@link #ticks}.
 * Errors that the server logs fail the test unless a command's result claims them with {@link CommandResult#expectError}.
 */
public final class ServerHarness{
    private static final int loadTimeoutSeconds = 180, taskTimeoutSeconds = 120;
    private static final Pattern ansi = Pattern.compile("\u001B\\[[0-9;]*m");

    private static final List<LogLine> logLines = new ArrayList<>();
    private static final List<String> errors = new ArrayList<>();
    private static volatile Throwable crash;
    private static volatile boolean loaded, exited;
    private static boolean booted;
    private static String baseConfig;
    private static int serverPort;
    private static Fi dataDir;

    private ServerHarness(){}

    /** One logged line, with colors removed. */
    public record LogLine(LogLevel level, String text){}

    /**
     * Starts the server and blocks until it has loaded. Does nothing if already started.
     * @param config extra config.hjson lines (like "autosave: true"), applied on top of the test defaults.
     */
    public static synchronized void boot(String... config){
        if(booted){
            ensureAlive();
            return;
        }
        booted = true;

        dataDir = dirFromProperty("servertest.data.dir", "mindustry-server-test");
        dataDir.deleteDirectory();
        dataDir.mkdirs();

        serverPort = freePort();
        baseConfig = "port: " + serverPort + "\nsocketInput: false\nautoUpdate: false\nautosave: false\nlogging: false\nroundExtraTime: 1\n"
            + String.join("\n", config) + "\n";
        dataDir.child("config.hjson").writeString(baseConfig);

        //read by ServerLauncher#init and ServerControl
        System.setProperty("mindustry.data.dir", dataDir.absolutePath());
        System.setProperty("mindustry.test", "true");
        Log.useColors = false;

        Events.on(ServerLoadEvent.class, e -> {
            tapLogger();
            //one update is one tick, so that tests don't depend on machine speed
            Time.setDeltaProvider(() -> 1f);
            Core.app.addListener(new ApplicationListener(){
                @Override
                public void dispose(){
                    exited = true;
                }
            });
            loaded = true;
        });

        try{
            //same prelude as ServerLauncher#main
            ServerLauncher.args = new String[0];
            Vars.platform = new Platform(){};
            Vars.net = new Net(Vars.platform.getNet());
            new HeadlessApplication(new ServerLauncher(), t -> crash = t);
        }catch(Throwable t){
            crash = t;
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(loadTimeoutSeconds);
        while(!loaded){
            ensureAlive();
            if(System.nanoTime() > deadline){
                fail("The server did not finish loading within " + loadTimeoutSeconds + "s.");
            }
            Threads.sleep(50);
        }

        ticks(2);
        drainErrors();
    }

    /** Asks the server to exit and waits for it to finish. */
    public static void shutdown(){
        if(!loaded || exited || crash != null) return;

        Core.app.post(() -> Core.app.exit());
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while(!exited && System.nanoTime() < deadline){
            Threads.sleep(50);
        }
    }

    /** Puts the server back into its freshly booted state: not hosting, default config and rules, no saves. */
    public static void reset(){
        run(() -> {
            ServerControl control = ServerControl.instance;
            control.cancelPlayTask();
            control.inGameOverWait = false;
            net.closeServer();
            logic.reset();
            state.set(State.menu);
            maps.setNextMapOverride(null);

            saveDirectory.deleteDirectory();
            saveDirectory.mkdirs();
            control.configFile.writeString(baseConfig);
            control.loadServerConfig();
            dataDirectory.child("rules.hjson").writeString(ServerControl.defaultRuleString);
        });
        ticks(1);
        drainErrors();
    }

    //region state

    /** @return the port the server was configured to host on. */
    public static int port(){
        return serverPort;
    }

    /** @return the server's data directory (config.hjson, rules.hjson, saves, maps...). */
    public static Fi dataDir(){
        return dataDir;
    }

    /** @return everything logged since boot. */
    public static List<LogLine> logLines(){
        synchronized(logLines){
            return new ArrayList<>(logLines);
        }
    }

    /** @return and forget all unclaimed errors logged since the last call. */
    public static List<String> drainErrors(){
        synchronized(errors){
            List<String> copy = new ArrayList<>(errors);
            errors.clear();
            return copy;
        }
    }

    /** Fails if an error was logged and not claimed. */
    public static void assertNoErrors(){
        List<String> logged = drainErrors();
        if(!logged.isEmpty()){
            fail("The server logged " + logged.size() + " error(s):\n" + String.join("\n", logged));
        }
    }

    static void claimError(String line){
        synchronized(errors){
            errors.remove(line);
        }
    }

    /** Throws if the server has crashed or exited. */
    public static void ensureAlive(){
        Throwable t = crash;
        if(t != null){
            throw new AssertionError("The server crashed: " + Strings.getFinalMessage(t), t);
        }
        if(exited){
            throw new AssertionError("The server exited unexpectedly.");
        }
    }

    //endregion
    //region running code on the server thread

    /** Runs the code on the server thread, after the next update, and waits for it. Exceptions are rethrown here. */
    public static void run(UnsafeRunnable body){
        call(() -> {
            try{
                body.run();
            }catch(Throwable e){
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    /** Like {@link #run}, but returns a value. */
    public static <T> T call(Callable<T> body){
        ensureAlive();

        CompletableFuture<T> future = new CompletableFuture<>();
        Core.app.post(() -> {
            try{
                future.complete(body.call());
            }catch(Throwable t){
                //a failing test must not kill the server loop
                future.completeExceptionally(t);
            }
        });

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(taskTimeoutSeconds);
        while(true){
            ensureAlive();
            try{
                return future.get(100, TimeUnit.MILLISECONDS);
            }catch(TimeoutException e){
                if(System.nanoTime() > deadline){
                    throw new AssertionError("The server thread did not run a task within " + taskTimeoutSeconds + "s. Is it stuck?");
                }
            }catch(ExecutionException e){
                Throwable cause = e.getCause();
                if(cause instanceof Error err) throw err;
                if(cause instanceof RuntimeException re) throw re;
                throw new AssertionError("Exception on the server thread: " + cause, cause);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the server thread", e);
            }
        }
    }

    /** Runs a server console command, exactly like typing it, and returns what it logged. */
    public static CommandResult command(String line){
        return call(() -> {
            int start;
            synchronized(logLines){
                start = logLines.size();
            }
            ServerControl.instance.handleCommandString(line);
            synchronized(logLines){
                return new CommandResult(line, new ArrayList<>(logLines.subList(start, logLines.size())));
            }
        });
    }

    /** Lets the server run exactly this many updates. */
    public static void ticks(int count){
        ensureAlive();

        CountDownLatch latch = new CountDownLatch(count);
        Runnable[] tick = {null};
        tick[0] = () -> {
            latch.countDown();
            if(latch.getCount() > 0) Core.app.post(tick[0]);
        };
        Core.app.post(tick[0]);

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(taskTimeoutSeconds + count / 20);
        try{
            while(!latch.await(100, TimeUnit.MILLISECONDS)){
                ensureAlive();
                if(System.nanoTime() > deadline){
                    throw new AssertionError("Timed out after " + (count - latch.getCount()) + " of " + count + " ticks.");
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for ticks", e);
        }
        ensureAlive();
    }

    /** Runs updates until the condition (evaluated on the server thread) holds. */
    public static void waitUntil(String description, BooleanSupplier condition, int maxTicks){
        for(int i = 0; i < maxTicks; i++){
            if(call(condition::getAsBoolean)) return;
            ticks(1);
        }
        fail("Timed out waiting for " + description + " after " + maxTicks + " ticks.");
    }

    /** Like {@link #waitUntil}, but bounded by real time. Needed for things scheduled with a Timer, such as the game-over delay. */
    public static void waitUntilSeconds(String description, BooleanSupplier condition, float seconds){
        long deadline = System.nanoTime() + (long)(seconds * 1_000_000_000L);
        while(System.nanoTime() < deadline){
            if(call(condition::getAsBoolean)) return;
            Threads.sleep(20);
        }
        fail("Timed out waiting for " + description + " after " + seconds + "s.");
    }

    //endregion
    //region maps

    /** Saves a copy of ground zero as a custom map with this name, so that shuffling and hosting have something to pick. */
    public static Map createMap(String name){
        return call(() -> {
            Map source = maps.loadInternalMap("serpulo/groundZero");
            logic.reset();
            GameState.loadMap(source, source.applyRules(Gamemode.survival));

            StringMap tags = new StringMap(source.tags);
            tags.put("name", name);
            Map saved = maps.saveMap(tags);

            logic.reset();
            state.set(State.menu);
            return saved;
        });
    }

    //endregion

    /** Records every log line after ServerControl has installed its own logger, which replaces any earlier one. */
    private static void tapLogger(){
        LogHandler inner = Log.logger;
        Log.logger = (level, text) -> {
            inner.log(level, text);

            String plain = ansi.matcher(text).replaceAll("");
            synchronized(logLines){
                logLines.add(new LogLine(level, plain));
            }
            if(level == LogLevel.err){
                synchronized(errors){
                    errors.add(plain);
                }
            }
        };
    }

    private static int freePort(){
        try(ServerSocket socket = new ServerSocket(0)){
            return socket.getLocalPort();
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }

    private static Fi dirFromProperty(String property, String tempName){
        String path = System.getProperty(property);
        if(path != null) return new Fi(path);

        try{
            return new Fi(Files.createTempDirectory(tempName).toString());
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }
}
