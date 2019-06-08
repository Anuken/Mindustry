package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.GL20;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureAtlas;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.ui.Dialog;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.dialogs.FloatingDialog;
import io.anuke.mindustry.world.Tile;

import java.io.IOException;
import java.nio.IntBuffer;

import static io.anuke.arc.Core.scene;
import static io.anuke.mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener{
    public final Saves saves;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false;
    private InputHandler input;

    public Control(){
        IntBuffer buf = BufferUtils.newIntBuffer(1);
        Core.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buf);
        int maxSize = buf.get(0);

        saves = new Saves();
        data = new GlobalData();

        Core.input.setCatch(KeyCode.BACK, true);

        Effects.setShakeFalloff(10000f);

        content.initialize(Content::init);
        Core.atlas = new TextureAtlas(maxSize < 2048 ? "sprites/sprites_fallback.atlas" : "sprites/sprites.atlas");
        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();
        content.initialize(Content::load, true);

        data.load();

        Core.settings.setAppName(appName);
        Core.settings.defaults(
            "ip", "localhost",
            "color-0", Color.rgba8888(playerColors[8]),
            "color-1", Color.rgba8888(playerColors[11]),
            "color-2", Color.rgba8888(playerColors[13]),
            "color-3", Color.rgba8888(playerColors[9]),
            "name", "",
            "lastBuild", 0
        );

        createPlayer();

        saves.load();

        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Time.runTask(5f, Platform.instance::updateRPC);
            }
        });

        Events.on(PlayEvent.class, event -> {
            player.setTeam(defaultTeam);
            player.setDead(true);
            player.add();

            state.set(State.playing);
        });

        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> Core.app.post(() -> {
                if(Net.active() && player.getClosestCore() != null){
                    //set to closest core since that's where the player will probably respawn; prevents camera jumps
                    Core.camera.position.set(player.getClosestCore());
                }else{
                    //locally, set to player position since respawning occurs immediately
                    Core.camera.position.set(player);
                }
            }));
        });

        Events.on(ResetEvent.class, event -> {
            player.reset();

            hiscore = false;

            saves.resetSave();
        });

        Events.on(WaveEvent.class, event -> {
            if(world.getMap().getHightScore() < state.wave){
                hiscore = true;
                world.getMap().setHighScore(state.wave);
            }
        });

        Events.on(GameOverEvent.class, event -> {
            state.stats.wavesLasted = state.wave;
            Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.onGameOver(event.winner);
            if(state.rules.zone != null){
                //remove zone save on game over
                if(saves.getZoneSlot() != null){
                    saves.getZoneSlot().delete();
                }
            }
        });

        //autohost for pvp maps
        Events.on(WorldLoadEvent.class, event -> {
            if(state.rules.pvp && !Net.active()){
                try{
                    Net.host(port);
                    player.isAdmin = true;
                }catch(IOException e){
                    ui.showError(Core.bundle.format("server.error", Strings.parseException(e, true)));
                    Core.app.post(() -> state.set(State.menu));
                }
            }
        });

        Events.on(UnlockEvent.class, e -> ui.hudfrag.showUnlock(e.content));

        Events.on(BlockBuildEndEvent.class, e -> {
            if(e.team == player.getTeam()){
                if(e.breaking){
                    state.stats.buildingsDeconstructed++;
                }else{
                    state.stats.buildingsBuilt++;
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.getTeam() == player.getTeam()){
                state.stats.buildingsDestroyed++;
            }
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit.getTeam() != player.getTeam()){
                state.stats.enemyUnitsDestroyed++;
            }
        });

        Events.on(ZoneRequireCompleteEvent.class, e -> {
            ui.hudfrag.showToast(Core.bundle.format("zone.requirement.complete", state.wave, e.zone.localizedName));
        });

        Events.on(ZoneConfigureCompleteEvent.class, e -> {
            ui.hudfrag.showToast(Core.bundle.format("zone.config.complete", e.zone.configureWave));
        });
    }

    void createPlayer(){
        player = new Player();
        player.name = Core.settings.getString("name");
        player.color.set(Core.settings.getInt("color-0"));
        player.isLocal = true;
        player.isMobile = mobile;

        if(mobile){
            input = new MobileInput();
        }else{
            input = new DesktopInput();
        }

        if(!state.is(State.menu)){
            player.add();
        }

        Core.input.addProcessor(input);
    }

    public InputHandler input(){
        return input;
    }

    public void playMap(Map map, Rules rules){
        ui.loadAnd(() -> {
            logic.reset();
            world.loadMap(map);
            state.rules = rules;
            logic.play();
        });
    }

    public void playZone(Zone zone){
        ui.loadAnd(() -> {
            logic.reset();
            world.loadGenerator(zone.generator);
            state.rules = zone.rules.get();
            state.rules.zone = zone;
            for(Tile core : state.teams.get(defaultTeam).cores){
                for(ItemStack stack : zone.getStartingItems()){
                    core.entity.items.add(stack.item, stack.amount);
                }
            }
            state.set(State.playing);
            control.saves.zoneSave();
            logic.play();
        });
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        content.dispose();
        Net.dispose();
        ui.editor.dispose();
    }

    @Override
    public void pause(){
        wasPaused = state.is(State.paused);
        if(state.is(State.playing)) state.set(State.paused);
    }

    @Override
    public void resume(){
        if(state.is(State.paused) && !wasPaused){
            state.set(State.playing);
        }
    }

    @Override
    public void init(){
        Platform.instance.updateRPC();

        if(!Core.settings.getBool("4.0-warning-2", false)){

            Time.run(5f, () -> {
                FloatingDialog dialog = new FloatingDialog("VERY IMPORTANT");
                dialog.buttons.addButton("$ok", () -> {
                    dialog.hide();
                    Core.settings.put("4.0-warning-2", true);
                    Core.settings.save();
                }).size(100f, 60f);
                dialog.cont.add("Reminder: The alpha version you are about to play is very unstable, and is [accent]not representative of the final v4 release.[]\n\n " +
                "\nThere is currently[scarlet] no sound implemented[]; this is intentional.\n" +
                "All current art and UI is unfinished, and will be changed before release. " +
                "\n\n[accent]Saves may be corrupted without warning between updates.").wrap().width(400f);
                dialog.show();
            });
        }
    }

    @Override
    public void update(){
        saves.update();

        input.updateController();

        //autosave global data if it's modified
        data.checkSave();

        if(!state.is(State.menu)){
            input.update();

            if(world.isZone()){
                for(Tile tile : state.teams.get(player.getTeam()).cores){
                    for(Item item : content.items()){
                        if(tile.entity.items.has(item)){
                            data.unlockContent(item);
                        }
                    }
                }
            }

            //auto-update rpc every 5 seconds
            if(timer.get(0, 60 * 5)){
                Platform.instance.updateRPC();
            }

            if(Core.input.keyTap(Binding.pause) && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.is(State.playing) ? State.paused : State.playing);
            }

            if(Core.input.keyTap(Binding.menu) && !ui.restart.isShown()){
                if(ui.chatfrag.chatOpen()){
                    ui.chatfrag.hide();
                }else if(!ui.paused.isShown() && !scene.hasDialog()){
                    ui.paused.show();
                    state.set(State.paused);
                }
            }

            if(!mobile && Core.input.keyTap(Binding.screenshot) && !(scene.getKeyboardFocus() instanceof TextField) && !ui.chatfrag.chatOpen()){
                renderer.takeMapScreenshot();
            }

        }else{
            if(!state.isPaused()){
                Time.update();
            }

            if(!scene.hasDialog() && !(scene.root.getChildren().peek() instanceof Dialog) && Core.input.keyTap(KeyCode.BACK)){
                Platform.instance.hide();
            }
        }
    }
}
