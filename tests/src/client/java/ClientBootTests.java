import arc.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Does the real client start, and does the main menu render? */
public class ClientBootTests extends ClientTestBase{

    @Test
    void bootedCleanly(){
        //anything logged at error level while loading (and not on ClientHarness' ignore list) is a bug or an environment problem worth knowing about
        assertTrue(ClientHarness.bootErrors().isEmpty(), "Errors logged while the client was loading:\n" + ClientHarness.bootErrors().toString("\n"));
    }

    @Test
    void modulesExist(){
        ClientHarness.run(() -> {
            assertTrue(clientLoaded, "ClientLoadEvent must have fired");
            assertFalse(headless, "this must be the graphical client");

            assertNotNull(Core.atlas, "atlas");
            assertNotNull(Core.batch, "batch");
            assertNotNull(Core.scene, "scene");
            assertNotNull(renderer, "renderer");
            assertNotNull(control, "control");
            assertNotNull(ui, "ui");
            assertNotNull(ui.menufrag, "main menu");
            assertNotNull(ui.hudfrag, "hud");
            assertNotNull(netClient, "net client");
            assertNotNull(netServer, "net server");

            assertTrue(content.blocks().size > 0, "content must be loaded");
            assertTrue(state.isMenu(), "must start in the menu");
        });
    }

    @Test
    void glContextIsUsable(){
        ClientHarness.run(() -> {
            assertTrue(Core.graphics.getWidth() > 0 && Core.graphics.getHeight() > 0, "window has no size");
            assertTrue(Core.graphics.getGLVersion().atLeast(3, 0), "need at least GL 3.0, got " + Core.graphics.getGLVersion());
        });
    }

    @Test
    void menuRenders(){
        //Renderer checks glGetError every few frames and logs failures as errors, which the base class turns into a test failure
        ClientHarness.frames(60);
        assertFrameNotBlank("menu");
    }
}
