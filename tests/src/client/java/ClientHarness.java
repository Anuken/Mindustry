import arc.*;
import arc.backend.sdl.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Log.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.game.EventType.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boots the real {@link ClientLauncher} (the same class DesktopLauncher extends) on an SDL window, on a dedicated render thread,
 * and lets tests run code on that thread, one frame at a time.
 * <p>
 * Everything in {@link Vars} / {@link Core} is static, so there is exactly one client per JVM. The gradle task forks one JVM per test class.
 * <p>
 * Threading rules:
 * <ul>
 *     <li>JUnit runs on its own thread. Anything touching GL, the scene, or the world must go through {@link #run}, {@link #call} or {@link #frames}.</li>
 *     <li>Posted tasks run once per frame, after all modules have updated (and drawn) and just before the buffer swap.
 *     That is also the right moment to read back the framebuffer, see {@link #capture()}.</li>
 *     <li>An exception thrown by a module (Renderer, UI, Logic...) kills the render loop, exactly like it would crash the real game.
 *     It is recorded, and every later harness call fails with it as the cause.</li>
 * </ul>
 */
public final class ClientHarness{
    /** Substrings of logged errors that are expected on CI machines and do not indicate a bug. */
    private static final String[] ignoredErrors = {
    //the SDL backend logs this for each GL version it tries and cannot get, before falling back to a lower one
    "Failed to initialize OpenGL"
    };

    private static final int loadTimeoutSeconds = 180, taskTimeoutSeconds = 120;

    private static final Seq<String> errors = new Seq<>();
    private static volatile Throwable crash;
    private static volatile boolean loaded;
    private static boolean booted;
    private static Thread renderThread;
    private static Seq<String> bootErrors = new Seq<>();
    private static Fi outputDir;

    private ClientHarness(){}

    /** Starts the client and blocks until it has fully loaded and is sitting in the main menu. Does nothing if already started. */
    public static synchronized void boot(){
        if(booted){
            ensureAlive();
            return;
        }
        booted = true;

        Fi data = dirFromProperty("clienttest.data.dir", "mindustry-client-test");
        data.deleteDirectory(); //a leftover launchid.dat would make the game think the last launch crashed
        data.mkdirs();
        outputDir = dirFromProperty("clienttest.out.dir", "mindustry-client-test-out");
        outputDir.mkdirs();

        //read by ClientLauncher#setup
        System.setProperty("mindustry.data.dir", data.absolutePath());
        System.setProperty("mindustry.test", "true");

        //same prelude as DesktopLauncher#main
        Log.useColors = false;
        Version.init();
        Vars.loadLogger();

        //record everything logged at error level. Many "caught" failures in the game (bad shader, missing region, failed content load...) only get logged.
        LogHandler inner = Log.logger;
        Log.logger = (level, text) -> {
            if(level == LogLevel.err && !isIgnored(text)){
                synchronized(errors){
                    errors.add(text);
                }
            }
            inner.log(level, text);
        };

        Events.on(ClientLoadEvent.class, e -> loaded = true);

        renderThread = new Thread(() -> {
            try{
                //blocks until the application exits
                new SdlApplication(new TestLauncher(), config());
            }catch(Throwable t){
                crash = t;
            }
        }, "client-render");
        renderThread.setDaemon(true);
        renderThread.start();

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(loadTimeoutSeconds);
        while(!loaded){
            ensureAlive();
            if(System.nanoTime() > deadline){
                fail("The client did not finish loading within " + loadTimeoutSeconds + "s.");
            }
            Threads.sleep(50);
        }

        //ClientLauncher finishes the launch (final resize, finishLaunch()) a few frames after ClientLoadEvent
        frames(8);

        run(() -> {
            //one frame is one tick, so that tests are deterministic no matter how slow software GL is
            Time.setDeltaProvider(() -> 1f);
            //don't sleep to hit a frame rate target
            Core.settings.put("fpscap", Integer.getInteger("clienttest.fps", 0));
        });

        bootErrors = drainErrors();
    }

    /** Asks the client to exit and waits for the render thread to finish. */
    public static void shutdown(){
        if(!booted || renderThread == null || !renderThread.isAlive()) return;

        Core.app.post(() -> Core.app.exit());
        try{
            renderThread.join(15_000);
        }catch(InterruptedException ignored){
        }
    }

    private static SdlConfig config(){
        SdlConfig cfg = new SdlConfig();
        cfg.title = "Mindustry (client test)";
        cfg.width = Integer.getInteger("clienttest.width", 640);
        cfg.height = Integer.getInteger("clienttest.height", 480);
        cfg.coreProfile = true;
        //same list as DesktopLauncher
        cfg.glVersions = new int[][]{{4, 5}, {4, 4}, {4, 1}, {3, 3}, {3, 2}, {3, 1}, {3, 0}};
        cfg.vSyncEnabled = false;
        //there is no sound device on CI
        cfg.disableAudio = true;
        cfg.appName = "Mindustry";
        cfg.appIdentifier = "io.anuke.mindustry";
        cfg.appVersion = Version.buildString();
        return cfg;
    }

    /**
     * What DesktopLauncher does, minus Steam, Discord, native file dialogs and the crash dialog.
     * Not extending DesktopLauncher on purpose: the desktop module pulls in Steamworks and Discord natives.
     * Everything the tests care about (asset loading, module setup, the update loop) lives in ClientLauncher.
     */
    private static class TestLauncher extends ClientLauncher{
    }

    //region state

    /** Errors logged during startup that were not on the ignore list. Tests that care about boot cleanliness can assert on this. */
    public static Seq<String> bootErrors(){
        return bootErrors.copy();
    }

    /** @return and forget all errors logged since the last call. */
    public static Seq<String> drainErrors(){
        synchronized(errors){
            Seq<String> copy = errors.copy();
            errors.clear();
            return copy;
        }
    }

    /** Fails if anything was logged at error level since the last drain. */
    public static void assertNoErrors(){
        Seq<String> logged = drainErrors();
        if(logged.any()){
            fail("The client logged " + logged.size + " error(s):\n" + logged.toString("\n"));
        }
    }

    private static boolean isIgnored(String text){
        for(String s : ignoredErrors){
            if(text.contains(s)) return true;
        }
        return false;
    }

    /** Throws if the render thread has died (crash or exit). */
    public static void ensureAlive(){
        Throwable t = crash;
        if(t != null){
            throw new AssertionError("The client crashed on the render thread: " + Strings.getFinalMessage(t), t);
        }
        if(booted && renderThread != null && !renderThread.isAlive()){
            throw new AssertionError("The client exited unexpectedly.");
        }
    }

    //endregion
    //region running code on the render thread

    /** Runs the code on the render thread, in the post-update phase of the next frame, and waits for it. Exceptions are rethrown here. */
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
                //don't let a failing test kill the render loop: complete the future instead
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
                    throw new AssertionError("The render thread did not run a task within " + taskTimeoutSeconds + "s. Is the client stuck?");
                }
            }catch(ExecutionException e){
                Throwable cause = e.getCause();
                if(cause instanceof Error err) throw err;
                if(cause instanceof RuntimeException re) throw re;
                throw new AssertionError("Exception on the render thread: " + cause, cause);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for the render thread", e);
            }
        }
    }

    /** Lets the client run exactly this many complete frames (update, draw) and returns right before the last one is presented. */
    public static void frames(int count){
        ensureAlive();

        CountDownLatch latch = new CountDownLatch(count);
        Runnable[] tick = {null};
        tick[0] = () -> {
            latch.countDown();
            if(latch.getCount() > 0) Core.app.post(tick[0]);
        };
        Core.app.post(tick[0]);

        //allow for very slow software rendering: at least 5 frames per second
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(taskTimeoutSeconds + count / 5);
        try{
            while(!latch.await(100, TimeUnit.MILLISECONDS)){
                ensureAlive();
                if(System.nanoTime() > deadline){
                    throw new AssertionError("Timed out after rendering " + (count - latch.getCount()) + " of " + count + " frames.");
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for frames", e);
        }
        ensureAlive();
    }

    /** Renders frames until the condition (evaluated on the render thread) holds. */
    public static void waitUntil(String description, BooleanSupplier condition, int maxFrames){
        for(int i = 0; i < maxFrames; i++){
            if(call(condition::getAsBoolean)) return;
            frames(1);
        }
        fail("Timed out waiting for " + description + " after " + maxFrames + " frames.");
    }

    //endregion
    //region screenshots

    /** @return the current contents of the default framebuffer (top row first). Dispose after use. */
    public static Pixmap capture(){
        return call(() -> ScreenUtils.getFrameBufferPixmap(0, 0, Core.graphics.getBackBufferWidth(), Core.graphics.getBackBufferHeight(), true));
    }

    /** @return the number of distinct colors in a coarse sample of the image. A blank or single-color frame returns 1. */
    public static int distinctColors(Pixmap pixmap){
        IntSet colors = new IntSet();
        for(int y = 0; y < pixmap.height; y += 3){
            for(int x = 0; x < pixmap.width; x += 3){
                colors.add(pixmap.get(x, y));
            }
        }
        return colors.size;
    }

    /** Writes a screenshot to the test output directory, for humans to look at when a test fails (or just to see what the renderer did). */
    public static void saveScreenshot(String name){
        Pixmap pixmap = capture();
        try{
            savePixmap(name, pixmap);
        }finally{
            pixmap.dispose();
        }
    }

    public static void savePixmap(String name, Pixmap pixmap){
        PixmapIO.writePng(outputDir.child(name + ".png"), pixmap);
    }

    //endregion

    private static Fi dirFromProperty(String property, String tempName){
        String path = System.getProperty(property);
        if(path != null) return new Fi(path);

        try{
            return new Fi(java.nio.file.Files.createTempDirectory(tempName).toString());
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }
}
