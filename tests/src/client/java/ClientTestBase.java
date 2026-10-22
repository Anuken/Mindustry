import arc.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.maps.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;

import java.util.function.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for tests that need the real client. Boots it once per class (the gradle task forks a JVM per class),
 * puts it back into the main menu before each test, and fails any test during which the client logged an error.
 */
public abstract class ClientTestBase{

    @BeforeAll
    static void bootClient(){
        ClientHarness.boot();
    }

    @AfterAll
    static void shutdownClient(){
        ClientHarness.shutdown();
    }

    @BeforeEach
    void resetClient(){
        closeDialogs();
        toMenu();
        ClientHarness.drainErrors();
    }

    @AfterEach
    void checkErrors(){
        ClientHarness.assertNoErrors();
    }

    //region helpers

    /** Hides every open dialog (a failed test may have left some behind). */
    protected static void closeDialogs(){
        for(int i = 0; i < 10 && ClientHarness.call(() -> Core.scene.getDialog() != null); i++){
            ClientHarness.run(() -> {
                if(Core.scene.getDialog() != null) Core.scene.getDialog().hide();
            });
            ClientHarness.frames(30);
        }
    }

    /** Leaves the current game, like "quit" in the pause menu. */
    protected static void toMenu(){
        ClientHarness.run(() -> {
            logic.reset();
            state.set(State.menu);
        });
        ClientHarness.frames(2);
    }

    /** Loads a map the way "custom game" does and waits until it is being played. */
    protected static void playMap(Map map){
        ClientHarness.run(() -> control.playMap(map, map.rules()));
        //playMap goes through ui.loadAnd, which delays by a few ticks
        ClientHarness.waitUntil("the map to start", () -> state.isGame(), 300);
        ClientHarness.run(() -> {
            state.rules.canGameOver = false;
        });
    }

    protected static Map groundZero(){
        return ClientHarness.call(() -> maps.loadInternalMap("serpulo/groundZero"));
    }

    /**
     * Replaces the world with a flat stone world, lets the filler place things in it, then starts playing it.
     * The camera is centered on the middle of the world.
     */
    protected static void playCustomWorld(int width, int height, Consumer<World> filler){
        ClientHarness.run(() -> {
            logic.reset();
            state.rules.canGameOver = false;
            state.rules.waves = false;
            state.rules.waveTimer = false;
            state.rules.borderDarkness = false;
            //prevent unit death
            state.rules.unitCap = 100000;

            state.beginMapLoad();
            World world = state.resizeWorld(width, height);
            for(int x = 0; x < width; x++){
                for(int y = 0; y < height; y++){
                    world.set(x, y, new Tile(x, y, Blocks.stone, Blocks.air, Blocks.air));
                }
            }
            filler.accept(world);
            state.endMapLoad();

            logic.play();
            Core.camera.position.set(width * tilesize / 2f, height * tilesize / 2f);
        });
    }

    /** Zooms the camera out as far as it goes and waits for the zoom to settle. */
    protected static void zoomOut(){
        ClientHarness.run(() -> renderer.setScale(renderer.minScale()));
        ClientHarness.frames(120);
    }

    /** Asserts that the camera currently sees the world position, so that "we drew it" means something. */
    protected static void assertVisible(float worldX, float worldY, String what){
        boolean visible = ClientHarness.call(() -> Core.camera.bounds(Tmp.r1).contains(worldX, worldY));
        assertTrue(visible, what + " is outside the camera view, so rendering it proved nothing.");
    }

    /** Asserts that the last frame shows something (more than a handful of distinct colors), and saves it as a screenshot either way. */
    protected static void assertFrameNotBlank(String screenshotName){
        Pixmap pixmap = ClientHarness.capture();
        try{
            ClientHarness.savePixmap(screenshotName, pixmap);
            int colors = ClientHarness.distinctColors(pixmap);
            assertTrue(colors >= 16, "The frame looks blank (" + colors + " distinct colors in a sample).");
        }finally{
            pixmap.dispose();
        }
    }

    //endregion
}
