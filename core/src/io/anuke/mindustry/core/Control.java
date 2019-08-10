package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.storage.*;

import java.io.*;

import static io.anuke.arc.Core.*;
import static io.anuke.mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener{
    public final Saves saves;
    public final MusicControl music;
    public final Tutorial tutorial;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false;
    private InputHandler input;

    public Control(){
        batch = new SpriteBatch();
        saves = new Saves();
        tutorial = new Tutorial();
        music = new MusicControl();

        UnitScl.dp.setProduct(settings.getInt("uiscale", 100) / 100f);

        Core.input.setCatch(KeyCode.BACK, true);

        content.initialize(Content::init);
        Core.atlas = new TextureAtlas("sprites/sprites.atlas");
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
            tutorial.reset();

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
            if(state.rules.zone != null && !Net.client()){
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
            Net.reset();
            world.loadGenerator(zone.generator);
            zone.rules.accept(state.rules);
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

    public void playTutorial(){
        Zone zone = Zones.groundZero;
        ui.loadAnd(() -> {
            logic.reset();
            Net.reset();

            world.beginMapLoad();

            world.createTiles(zone.generator.width, zone.generator.height);
            zone.generator.generate(world.getTiles());

            Tile coreb = null;

            out:
            for(int x = 0; x < world.width(); x++){
                for(int y = 0; y < world.height(); y++){
                    if(world.rawTile(x, y).block() instanceof CoreBlock){
                        coreb = world.rawTile(x, y);
                        break out;
                    }
                }
            }

            Geometry.circle(coreb.x, coreb.y, 10, (cx, cy) -> {
                Tile tile = world.ltile(cx, cy);
                if(tile != null && tile.getTeam() == defaultTeam && !(tile.block() instanceof CoreBlock)){
                    world.removeBlock(tile);
                }
            });

            Geometry.circle(coreb.x, coreb.y, 5, (cx, cy) -> world.tile(cx, cy).clearOverlay());

            world.endMapLoad();

            zone.rules.accept(state.rules);
            state.rules.zone = zone;
            for(Tile core : state.teams.get(defaultTeam).cores){
                for(ItemStack stack : zone.getStartingItems()){
                    core.entity.items.add(stack.item, stack.amount);
                }
            }
            Tile core = state.teams.get(defaultTeam).cores.first();
            core.entity.items.clear();

            logic.play();
            state.rules.waveTimer = false;
            state.rules.waveSpacing = 60f * 30;
            state.rules.buildCostMultiplier = 0.3f;
            state.rules.tutorial = true;
        });
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        content.dispose();
        Net.dispose();
        Musics.dispose();
        Sounds.dispose();
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

        //play tutorial on stop
        if(!settings.getBool("tutorial", false)){
            Core.app.post(this::playTutorial);
        }

        //display UI scale changed dialog
        if(Core.settings.getBool("uiscalechanged", false)){
            FloatingDialog dialog = new FloatingDialog("$confirm");

            float[] countdown = {60 * 11};
            Runnable exit = () -> {
                Core.settings.put("uiscale", 100);
                Core.settings.put("uiscalechanged", false);
                settings.save();
                dialog.hide();
                Core.app.exit();
            };

            dialog.setFillParent(false);
            dialog.cont.label(() -> {
                if(countdown[0] <= 0){
                    exit.run();
                }
                return Core.bundle.format("uiscale.reset", (int)((countdown[0] -= Time.delta()) / 60f));
            }).pad(10f).expand().left();

            dialog.buttons.defaults().size(200f, 60f);
            dialog.buttons.addButton("$uiscale.cancel", exit);

            dialog.buttons.addButton("$ok", () -> {
                Core.settings.put("uiscalechanged", false);
                settings.save();
                dialog.hide();
            });

            Core.app.post(dialog::show);
        }
    }

    @Override
    public void update(){
        saves.update();

        input.updateController();

        //autosave global data if it's modified
        data.checkSave();

        if(state.is(State.menu)){
            if(ui.deploy.isShown()){
                music.play(Musics.launch);
            }else if(ui.editor.isShown()){
                music.play(Musics.editor);
            }else{
                music.play(Musics.menu);
            }
        }else{
            //TODO game music
            music.silence();
        }

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

            if(state.rules.tutorial){
                tutorial.update();
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

            if(!scene.hasDialog() && !scene.root.getChildren().isEmpty() && !(scene.root.getChildren().peek() instanceof Dialog) && Core.input.keyTap(KeyCode.BACK)){
                Platform.instance.hide();
            }
        }
    }
}
