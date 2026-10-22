package mindustry.server;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpRequest.*;
import java.net.http.HttpResponse.*;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.regex.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A real graphical client in a child JVM, controlled through the client driver's HTTP commands (see tests/driver/CLIENT_TESTING.md).
 * The child cannot share a JVM with the server because Vars and Core are static.
 */
public final class DriverClient{
    private static final int bootTimeoutSeconds = 240, commandTimeoutSeconds = 120, tailLines = 40;

    private final Process process;
    private final int httpPort;
    private final HttpClient http = HttpClient.newHttpClient();
    private final CountDownLatch ready = new CountDownLatch(1);
    private final Deque<String> tail = new ArrayDeque<>();
    private final List<String> pendingErrors = new ArrayList<>();

    private DriverClient(Process process, int httpPort){
        this.process = process;
        this.httpPort = httpPort;

        Thread pump = new Thread(this::pumpOutput, "client-output");
        pump.setDaemon(true);
        pump.start();
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));
    }

    /** Starts the client and blocks until it has loaded and is sitting in the main menu. */
    public static DriverClient start(){
        String classpath = System.getProperty("nettest.driver.classpath");
        assertNotNull(classpath, "nettest.driver.classpath is not set. Run these tests through the gradle netTest task.");

        int port = freePort();
        Path data = directory("nettest.data.dir", "mindustry-net-test-client");
        Path out = directory("nettest.out.dir", "mindustry-net-test-out");

        List<String> command = List.of(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+ShowCodeDetailsInExceptionMessages", "--enable-native-access=ALL-UNNAMED",
            "-Dclienttest.data.dir=" + data, "-Dclienttest.out.dir=" + out,
            "-Dclientdriver.port=" + port,
            "-Dclienttest.width=1280", "-Dclienttest.height=720", "-Dclientdriver.width=1280", "-Dclientdriver.height=720",
            "-cp", classpath, "ClientDriver");

        DriverClient client;
        try{
            //same working directory as this JVM: the game loads assets relative to it
            client = new DriverClient(new ProcessBuilder(command).redirectErrorStream(true).start(), port);
        }catch(IOException e){
            throw new UncheckedIOException("Could not start the client process", e);
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(bootTimeoutSeconds);
        try{
            while(!client.ready.await(500, TimeUnit.MILLISECONDS)){
                if(!client.process.isAlive()) fail("The client exited while starting.\n" + client.tail());
                if(System.nanoTime() > deadline){
                    client.process.destroyForcibly();
                    fail("The client did not start within " + bootTimeoutSeconds + "s.\n" + client.tail());
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while starting the client", e);
        }
        return client;
    }

    /** Asks the client to quit, and kills it if it does not. */
    public void stop(){
        if(!process.isAlive()) return;

        try{
            command("quit");
            if(!process.waitFor(15, TimeUnit.SECONDS)) process.destroyForcibly();
        }catch(Throwable t){
            process.destroyForcibly();
        }
    }

    //region commands

    /** Runs a driver command and returns its output. A failing command fails the test. */
    public String command(String line){
        ensureAlive();

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + httpPort))
            .timeout(Duration.ofSeconds(commandTimeoutSeconds)).POST(BodyPublishers.ofString(line)).build();
        try{
            HttpResponse<String> response = http.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body().strip();
            if(response.statusCode() != 200) throw new AssertionError("Client command '" + line + "' failed: " + body);
            return body;
        }catch(IOException e){
            ensureAlive();
            throw new AssertionError("Client command '" + line + "' could not be sent: " + e, e);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while running client command '" + line + "'", e);
        }
    }

    /** Joins the server on localhost. Returns right away; use {@link #waitUntilConnected}. */
    public void connect(int port){
        command("connect localhost " + port);
    }

    public void disconnect(){
        command("disconnect");
    }

    /** Closes dialogs and returns to the main menu. */
    public void menu(){
        command("menu");
    }

    /** Evaluates JavaScript on the render thread, with the whole game in scope. One line only. */
    public String js(String code){
        return command("js " + code);
    }

    /** Saves a screenshot to the output directory and returns its path. */
    public String shot(String name){
        return command("shot " + name);
    }

    public String state(){
        return command("state");
    }

    public String net(){
        return command("net");
    }

    /** @return the chat messages the client has received, newest first, one per line. */
    public String chat(){
        return command("chat");
    }

    /** Sends a chat message to the server, as if typed in the chat box. */
    public void say(String text){
        command("say " + text);
    }

    /** @return the single-word value of a key in the output of {@link #state} (state, tick, wave...) or null. Use {@link #js} for anything else. */
    public String stateValue(String key){
        Matcher matcher = Pattern.compile("(?:^|\\s)" + Pattern.quote(key) + "=(\\S*)").matcher(state());
        return matcher.find() ? matcher.group(1) : null;
    }

    /** @return whether the client is connected to a server and playing in it. */
    public boolean connected(){
        return net().contains("client=true") && "playing".equals(stateValue("state"));
    }

    //endregion
    //region waiting

    /** Polls the condition, which may use client commands, until it holds. */
    public void waitUntil(String description, BooleanSupplier condition, double seconds){
        long deadline = System.nanoTime() + (long)(seconds * 1_000_000_000L);
        while(System.nanoTime() < deadline){
            if(condition.getAsBoolean()) return;
            ensureAlive();
            try{
                Thread.sleep(100);
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for " + description, e);
            }
        }
        fail("Timed out waiting for " + description + " after " + seconds + "s.\nClient: " + state() + " / " + net());
    }

    public void waitUntilConnected(double seconds){
        waitUntil("the client to be connected", this::connected, seconds);
    }

    //endregion
    //region errors

    /** @return and forget all unclaimed errors the client logged. Each entry is one batch of lines from the driver's errors command. */
    public List<String> drainErrors(){
        fetchErrors();
        List<String> copy = new ArrayList<>(pendingErrors);
        pendingErrors.clear();
        return copy;
    }

    /** Fails if the client logged an error that was not claimed. */
    public void assertNoErrors(){
        List<String> logged = drainErrors();
        if(!logged.isEmpty()) fail("The client logged " + logged.size() + " error(s):\n" + String.join("\n", logged));
    }

    /** Asserts that the client logged an error containing the text, and stops it from failing the test. */
    public void expectError(String text){
        fetchErrors();
        String found = pendingErrors.stream().filter(e -> e.contains(text)).findFirst().orElse(null);
        assertNotNull(found, "The client did not log an error containing '" + text + "'.");
        pendingErrors.remove(found);
    }

    private void fetchErrors(){
        String result = command("errors");
        if(!result.equals("no errors")) pendingErrors.add(result);
    }

    //endregion

    private void ensureAlive(){
        if(!process.isAlive()) throw new AssertionError("The client process is not running.\n" + tail());
    }

    private void pumpOutput(){
        try(BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))){
            String line;
            while((line = in.readLine()) != null){
                System.out.println("[client] " + line);
                synchronized(tail){
                    tail.add(line);
                    if(tail.size() > tailLines) tail.removeFirst();
                }
                if(line.startsWith("CLIENT_DRIVER_READY")) ready.countDown();
            }
        }catch(IOException ignored){
        }
    }

    private String tail(){
        synchronized(tail){
            return "Last client output:\n" + String.join("\n", tail);
        }
    }

    private static int freePort(){
        try(ServerSocket socket = new ServerSocket(0)){
            return socket.getLocalPort();
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }

    private static Path directory(String property, String tempName){
        try{
            String path = System.getProperty(property);
            return path != null ? Files.createDirectories(Path.of(path)) : Files.createTempDirectory(tempName);
        }catch(IOException e){
            throw new UncheckedIOException(e);
        }
    }
}
