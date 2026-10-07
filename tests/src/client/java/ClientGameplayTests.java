import arc.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.io.*;
import mindustry.game.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Plays real maps and saves with the real renderer, HUD and input handler running. */
public class ClientGameplayTests extends ClientTestBase{

    @Test
    void groundZeroPlays(){
        playMap(groundZero());

        ClientHarness.frames(300);

        ClientHarness.run(() -> {
            assertTrue(state.isGame());
            assertTrue(state.teams.playerCores().size > 0, "the map has a player core");
        });
        assertFrameNotBlank("groundzero");
    }

    @Test
    void wavesRender(){
        playMap(groundZero());

        ClientHarness.run(() -> {
            assertTrue(state.spawner.countSpawns() > 0, "no spawns present");
            logic.runWave();
        });
        //enemies spawn after a delay and then walk around, shoot, and die, all while being drawn
        ClientHarness.frames(600);
        ClientHarness.run(() -> assertFalse(state.entities.unit.isEmpty(), "nothing spawned"));
    }

    @Test
    void zoomRange(){
        playMap(groundZero());

        for(boolean in : new boolean[]{true, false, true}){
            ClientHarness.run(() -> renderer.setScale(in ? renderer.maxScale() : renderer.minScale()));
            ClientHarness.frames(60);
        }
        assertFrameNotBlank("zoom");
    }

    /** Settings that switch between entirely different render paths (framebuffers, shaders, lighting...). */
    @ParameterizedTest
    @ValueSource(strings = {"pixelate", "blockstatus", "drawlight", "animatedwater", "effects", "hidedisplays", "showpings"})
    void renderSettings(String setting){
        playMap(groundZero());

        boolean old = ClientHarness.call(() -> Core.settings.getBool(setting));
        try{
            ClientHarness.run(() -> Core.settings.put(setting, !old));
            ClientHarness.frames(60);
            assertFrameNotBlank("setting_" + setting + "_" + !old);
        }finally{
            ClientHarness.run(() -> Core.settings.put(setting, old));
        }
    }

    /** The old saves from ApplicationTests, but this time they are also drawn and run for a while. */
    @ParameterizedTest
    @ValueSource(strings = {"77.msav", "85.msav", "108.msav", "114.msav", "152_be.msav", "152.msav"})
    void legacySaves(String name){
        ClientHarness.run(() -> {
            logic.reset();
            SaveIO.load(Core.files.internal(name));
            state.rules.canGameOver = false;
            logic.play();
        });
        ClientHarness.frames(120);

        ClientHarness.run(() -> assertTrue(state.isGame()));
    }

    @Test
    void saveAndLoadWhilePlaying(){
        playMap(groundZero());
        ClientHarness.frames(60);

        ClientHarness.run(() -> {
            UnitTypes.flare.spawn(Team.sharded, 20f, 30f);
            SaveIO.save(saveDirectory.child("client_test.msav"));
            logic.reset();
            SaveIO.load(saveDirectory.child("client_test.msav"));
            logic.play();
        });
        ClientHarness.frames(1);

        ClientHarness.run(() -> assertNotNull(state.entities.unit.find(u -> u.type == UnitTypes.flare), "saved dagger must persist"));
    }
}
