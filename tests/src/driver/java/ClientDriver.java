import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.util.*;
import com.sun.net.httpserver.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.maps.*;
import mindustry.type.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;
import static org.lwjgl.sdl.SDLVideo.*;

/**
 * A long-lived client that an agent (or a human with curl) can drive interactively.
 * <p>
 * Boots the real client once, then serves one-line text commands on http://127.0.0.1:PORT (loopback only), e.g.
 * <pre>  curl -s localhost:8765 -d 'click 320 240'</pre>
 * Every command returns plain text. Failures return HTTP 500 with "ERROR: ...". Run `help` for the list, see CLIENT_TESTING.md for details.
 * <p>
 * Extends ClientTestBase only to reuse its protected helpers (playMap, toMenu, closeDialogs); no JUnit lifecycle runs here.
 */
public class ClientDriver extends ClientTestBase{
    private static final CountDownLatch quit = new CountDownLatch(1);
    private static Fi outDir;

    private static final String help = """
    looking:  shot [name] | ui | find <text> | state | errors | size | tilepx <x> <y> | getblock <x> <y>
    building: placeblock <x> <y> <block-name> [rotation]
    time:     frames [n] | freeze | thaw
    input:    click <x> <y> [right] | move <x> <y> | drag <x1> <y1> <x2> <y2> | scroll <dy> | key <KeyCode> [down|up] | type <text>
              clicktext <text> | clicktile <tx> <ty> [right]
    loading:  menu | map <internal map, e.g. serpulo/groundZero> | sector <preset, e.g. groundZero> | js <code>
    network:  connect [host] [port] | disconnect | net
    window:   resize <w> <h>
    other:    ping | help | quit
    pixel coordinates are screenshot pixels, origin top-left""";

    public static void main(String[] args){
        try{
            serve();
        }catch(Throwable t){
            t.printStackTrace();
            //the render thread is a daemon, so this also ends the JVM
            System.exit(1);
        }
    }

    static void serve() throws Exception{
        //don't kill my CPU by running at unlimited framerate
        System.setProperty("clienttest.fps", "60");
        ClientHarness.boot();

        String outProperty = System.getProperty("clienttest.out.dir");
        outDir = outProperty != null ? new Fi(outProperty) : Fi.tempDirectory("client-driver-out");
        outDir.mkdirs();

        var bootErrors = ClientHarness.bootErrors();
        if(bootErrors.any()) System.out.println("BOOT_ERRORS (" + bootErrors.size + "):\n" + bootErrors.toString("\n"));

        //a window that never has focus must not pause the game under the agent's feet
        ClientHarness.run(() -> Core.settings.put("backgroundpause", false));

        try{
            int width = Integer.getInteger("clientdriver.width", 1280), height = Integer.getInteger("clientdriver.height", 720);
            //newer harnesses already created the window at this size (clienttest.width/height)
            boolean sized = ClientHarness.call(() -> Core.graphics.getWidth() == width && Core.graphics.getHeight() == height);
            if(!sized) resize(width, height);
        }catch(Throwable t){
            System.out.println("WARNING: could not resize the window, staying at the default size: " + t);
        }
        ClientHarness.drainErrors();

        int port = Integer.getInteger("clientdriver.port", 8765);
        //loopback only: `js` runs arbitrary code inside the game
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        //single thread: commands are strictly sequential, like one person using the game
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/", ex -> {
            String cmd = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
            if(cmd.isEmpty() && ex.getRequestURI().getRawQuery() != null) cmd = URLDecoder.decode(ex.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);

            int status = 200;
            String response;
            try{
                response = execute(cmd);
            }catch(Throwable t){
                status = 500;
                String message = Strings.getFinalMessage(t);
                response = "ERROR: " + (message == null || message.isEmpty() ? t.getClass().getSimpleName() : message);
            }
            byte[] bytes = (response + "\n").getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(status, bytes.length);
            try(OutputStream out = ex.getResponseBody()){
                out.write(bytes);
            }
        });
        server.start();

        //scripts wait for this exact line
        System.out.println("CLIENT_DRIVER_READY port=" + port + " screenshots=" + outDir.absolutePath());

        quit.await();
        server.stop(0);
        ClientHarness.shutdown();
        System.exit(0);
    }

    static String execute(String line) throws Exception{
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0], rest = parts.length > 1 ? parts[1].trim() : "";
        String[] a = rest.isEmpty() ? new String[0] : rest.split("\\s+");

        switch(cmd){
            case "ping": ClientHarness.ensureAlive(); return "pong";
            case "help": return help;

            //--- looking
            case "shot": {
                String name = a.length > 0 ? a[0].replaceAll("[^A-Za-z0-9_\\-]", "_") : "shot";
                Fi file = outDir.child(name + ".png");
                Pixmap pixmap = ClientHarness.capture();
                try{
                    PixmapIO.writePng(file, pixmap);
                    return file.absolutePath() + " (" + pixmap.width + "x" + pixmap.height + ")";
                }finally{
                    pixmap.dispose();
                }
            }
            case "size": return ClientHarness.call(() -> Core.graphics.getWidth() + "x" + Core.graphics.getHeight());
            case "ui": return ClientHarness.call(UiDump::dump);
            case "find": {
                var hits = ClientHarness.call(() -> UiDump.find(rest));
                return hits.isEmpty() ? "no matches" : hits.toString("\n");
            }
            case "state": return ClientHarness.call(() -> "state=" + state.getState()
                + " map=" + (state.map == null ? "none" : state.map.name())
                + " campaign=" + state.isCampaign()
                + (state.isCampaign() ? " sector=" + (state.rules.sector.preset != null ? state.rules.sector.preset.name : "#" + state.rules.sector.id) : "")
                + " tick=" + state.tick + " wave=" + state.wave
                + " playerUnits=" + state.rules.defaultTeam.data().units.size
                + " dialog=" + (Core.scene.getDialog() == null ? "none" : Core.scene.getDialog().getClass().getSimpleName()));
            case "errors": {
                var errs = ClientHarness.drainErrors();
                return errs.isEmpty() ? "no errors" : errs.toString("\n");
            }
            case "tilepx": {
                float tx = num(a, 0), ty = num(a, 1);
                Vec2 p = ClientHarness.call(() -> tilePixels(tx, ty));
                return (int)p.x + " " + (int)p.y + (onScreen(p) ? "" : " (off screen)");
            }
            case "getblock": {
                int x = integer(a, 0), y = integer(a, 1);
                return ClientHarness.call(() -> {
                    var tile = state.world.tile(x, y);
                    return tile == null ? "null" : tile.toString();
                });
            }

            //--- time
            case "frames": ClientHarness.frames(a.length > 0 ? (int)num(a, 0) : 1); return "ok";
            //game time stops; UI animations run on real time, so dialogs still open and close
            case "freeze": ClientHarness.run(() -> Time.setDeltaProvider(() -> 0f)); return "game time frozen";
            case "thaw": ClientHarness.run(() -> Time.setDeltaProvider(() -> 1f)); return "game time running, 1 tick per frame";

            //--- input, in screenshot pixels
            case "move": ClientInput.move(num(a, 0), num(a, 1)); return "ok";
            case "click": ClientInput.click(num(a, 0), num(a, 1), button(a, 2)); return "ok";
            case "drag": ClientInput.drag(num(a, 0), num(a, 1), num(a, 2), num(a, 3), 10); return "ok";
            case "scroll": ClientInput.scroll(num(a, 0)); return "ok";
            case "key": {
                if(a.length == 0) throw new IllegalArgumentException("missing KeyCode, e.g. `key escape`");
                KeyCode k = KeyCode.valueOf(a[0]);
                if(a.length > 1) ClientInput.key(k, a[1].equals("down")); else ClientInput.key(k);
                return "ok";
            }
            case "type": ClientInput.type(rest); return "ok";
            //click the button/label whose text or name contains the argument
            case "clicktext": {
                UiDump.Hit h = ClientHarness.call(() -> UiDump.findClickable(rest));
                ClientInput.click(h.cx(), h.cy());
                return "clicked " + h;
            }
            case "clicktile": {
                float tx = num(a, 0), ty = num(a, 1);
                Vec2 p = ClientHarness.call(() -> tilePixels(tx, ty));
                if(!onScreen(p)) throw new IllegalStateException("tile " + tx + "," + ty + " is off screen (at pixel " + (int)p.x + "," + (int)p.y + "). Move the camera first, e.g. with `js`.");
                ClientInput.click(p.x, p.y, button(a, 2));
                return "clicked tile at pixel " + (int)p.x + "," + (int)p.y;
            }
            case "placeblock": {
                if(a.length < 3) throw new IllegalArgumentException("usage: placeblock <x> <y> <block-name> [rotation]");
                int x = integer(a, 0), y = integer(a, 1);
                int rotation = a.length > 3 ? integer(a, 3) : 0;
                var block = ClientHarness.call(() -> content.block(a[2]));
                if(block == null) throw new IllegalArgumentException("no block named '" + a[2] + "'");
                return ClientHarness.call(() -> {
                    if(player == null || player.dead()) throw new IllegalStateException("player is dead; block was not queued");
                    player.unit().plans.addFirst(new BuildPlan(x, y, rotation, block));
                    return "queued " + block.name + " at " + x + "," + y + " rotation " + rotation;
                });
            }

            //--- loading things
            case "menu": closeDialogs(); toMenu(); return "in main menu";
            case "map": {
                //internal map name like serpulo/groundZero
                Map m = ClientHarness.call(() -> maps.loadInternalMap(rest));
                playMap(m);
                return "playing " + rest;
            }
            case "sector": {
                SectorPreset preset = ClientHarness.call(() -> content.sector(rest));
                if(preset == null) throw new IllegalArgumentException("no sector preset named '" + rest + "'. They are lowerCamelCase, e.g. groundZero, frozenForest.");
                closeDialogs();
                toMenu();
                ClientHarness.run(() -> control.playSector(preset.sector));
                ClientHarness.waitUntil("the sector to start", () -> state.isGame() && state.isCampaign(), 600);
                return "playing sector " + rest;
            }
            //escape hatch: JavaScript on the render thread, same engine and syntax as the in-game console
            case "js": return ClientHarness.call(() -> mods.getScripts().runConsole(rest));

            //--- network
            case "connect": {
                String host = a.length > 0 ? a[0] : "localhost";
                int serverPort = a.length > 1 ? integer(a, 1) : port;
                ClientHarness.run(() -> {
                    if(player.name.trim().isEmpty()) player.name = "driver";
                    //what JoinDialog#connect does, minus the dialog
                    logic.reset();
                    net.reset();
                    netClient.beginConnecting();
                    net.connect(host, serverPort, () -> {});
                });
                return "connecting to " + host + ":" + serverPort + ", poll `net` and `state`";
            }
            case "disconnect": ClientHarness.run(() -> netClient.disconnectQuietly()); return "disconnected";
            case "net": return ClientHarness.call(() -> "client=" + net.client() + " active=" + net.active()
                + " players=" + state.entities.player.size() + " me=" + player.name + "#" + player.id()
                + " state=" + state.getState());

            //--- window
            case "resize": resize((int)num(a, 0), (int)num(a, 1)); return "window is " + ClientHarness.call(() -> Core.graphics.getWidth() + "x" + Core.graphics.getHeight());

            case "quit": quit.countDown(); return "bye";
            default: throw new IllegalArgumentException("unknown command '" + cmd + "'. Run `help`.");
        }
    }

    static void resize(int width, int height){
        ClientHarness.run(() -> SDL_SetWindowSize(((arc.backend.sdl.SdlApplication)Core.app).getWindow(), width, height));
        ClientHarness.waitUntil("the window to become " + width + "x" + height, () -> Core.graphics.getWidth() == width && Core.graphics.getHeight() == height, 300);
        ClientHarness.frames(5);
    }

    /** Render thread only. World tile -> screenshot pixel (top-left origin). */
    static Vec2 tilePixels(float tileX, float tileY){
        Vec2 p = Core.camera.project(tileX * tilesize, tileY * tilesize);
        return new Vec2(p.x, Core.graphics.getHeight() - p.y);
    }

    static boolean onScreen(Vec2 p){
        //safe from any thread: only reads the size, which only changes on the render thread between frames
        return p.x >= 0 && p.y >= 0 && p.x < Core.graphics.getWidth() && p.y < Core.graphics.getHeight();
    }

    private static int button(String[] a, int i){
        return a.length > i && a[i].equals("right") ? org.lwjgl.sdl.SDLMouse.SDL_BUTTON_RIGHT : org.lwjgl.sdl.SDLMouse.SDL_BUTTON_LEFT;
    }

    private static float num(String[] a, int i){
        if(i >= a.length) throw new IllegalArgumentException("missing argument #" + (i + 1) + ". Run `help`.");
        try{
            return Float.parseFloat(a[i]);
        }catch(NumberFormatException e){
            throw new IllegalArgumentException("argument #" + (i + 1) + " must be a number, got '" + a[i] + "'");
        }
    }

    private static int integer(String[] a, int i){
        if(i >= a.length) throw new IllegalArgumentException("missing argument #" + (i + 1) + ". Run `help`.");
        try{
            return Integer.parseInt(a[i]);
        }catch(NumberFormatException e){
            throw new IllegalArgumentException("argument #" + (i + 1) + " must be an integer, got '" + a[i] + "'");
        }
    }
}
