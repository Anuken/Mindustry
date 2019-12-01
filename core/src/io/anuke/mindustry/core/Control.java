package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.input.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.dialogs.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.storage.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static io.anuke.arc.Core.*;
import static io.anuke.mindustry.Vars.net;
import static io.anuke.mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener, Loadable{
    public Saves saves;
    public MusicControl music;
    public Tutorial tutorial;
    public InputHandler input;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false;

    public Control(){
        saves = new Saves();
        tutorial = new Tutorial();
        music = new MusicControl();

        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Time.runTask(5f, platform::updateRPC);
                for(Sound sound : assets.getAll(Sound.class, new Array<>())){
                    sound.stop();
                }
            }
        });

        Events.on(PlayEvent.class, event -> {
            player.setTeam(state.rules.pvp ? netServer.assignTeam(player, playerGroup.all()) : defaultTeam);
            player.setDead(true);
            player.add();

            state.set(State.playing);
        });

        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> Core.app.post(() -> {
                if(net.active() && player.getClosestCore() != null){
                    //set to closest core since that's where the player will probably respawn; prevents camera jumps
                    Core.camera.position.set(player.isDead() ? player.getClosestCore() : player);
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

            Sounds.wave.play();
        });

        Events.on(GameOverEvent.class, event -> {
            state.stats.wavesLasted = state.wave;
            Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.onGameOver(event.winner);
            if(state.rules.zone != null && !net.client()){
                //remove zone save on game over
                if(saves.getZoneSlot() != null && !state.rules.tutorial){
                    saves.getZoneSlot().delete();
                }
            }
        });

        //autohost for pvp maps
        Events.on(WorldLoadEvent.class, event -> {
            if(state.rules.pvp && !net.active()){
                try{
                    net.host(port);
                    player.isAdmin = true;
                }catch(IOException e){
                    ui.showException("$server.error", e);
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
            if(e.objective.display() != null){
                ui.hudfrag.showToast(Core.bundle.format("zone.requirement.complete", e.zoneForMet.localizedName, e.objective.display()));
            }
        });

        Events.on(ZoneConfigureCompleteEvent.class, e -> {
            if(e.zone.configureObjective.display() != null){
                ui.hudfrag.showToast(Core.bundle.format("zone.config.unlocked", e.zone.configureObjective.display()));
            }
        });

        Events.on(Trigger.newGame, () -> {
            TileEntity core = player.getClosestCore();

            if(core == null) return;

            app.post(() -> ui.hudfrag.showLand());
            renderer.zoomIn(Fx.coreLand.lifetime);
            app.post(() -> Effects.effect(Fx.coreLand, core.x, core.y, 0, core.block));
            Time.run(Fx.coreLand.lifetime, () -> {
                Effects.effect(Fx.launch, core);
                Effects.shake(5f, 5f, core);
            });
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit instanceof BaseUnit && world.isZone()){
                data.unlockContent(((BaseUnit)e.unit).getType());
            }
        });
    }

    @Override
    public void loadAsync(){
        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();

        Core.input.setCatch(KeyCode.BACK, true);

        data.load();

        Core.settings.defaults(
        "ip", "localhost",
        "color-0", Color.rgba8888(playerColors[8]),
        "name", "",
        "lastBuild", 0
        );

        createPlayer();

        saves.load();
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

        Events.on(ClientLoadEvent.class, e -> input.add());
    }

    public void setInput(InputHandler newInput){
        Block block = input.block;
        boolean added = Core.input.getInputProcessors().contains(input);
        input.remove();
        this.input = newInput;
        newInput.block = block;
        if(added){
            newInput.add();
        }
    }

    public void playMap(Map map, Rules rules){
        ui.loadAnd(() -> {
            logic.reset();
            world.loadMap(map, rules);
            state.rules = rules;
            state.rules.zone = null;
            state.rules.editor = false;
            logic.play();
            if(settings.getBool("savecreate") && !world.isInvalidMap()){
                control.saves.addSave(map.name() + " " + new SimpleDateFormat("MMM dd h:mm", Locale.getDefault()).format(new Date()));
            }
            Events.fire(Trigger.newGame);
        });
    }

    public void playZone(Zone zone){
        ui.loadAnd(() -> {
            logic.reset();
            net.reset();
            world.loadGenerator(zone.generator);
            zone.rules.get(state.rules);
            state.rules.zone = zone;
            for(Tile core : state.teams.get(defaultTeam).cores){
                for(ItemStack stack : zone.getStartingItems()){
                    core.entity.items.add(stack.item, stack.amount);
                }
            }
            state.set(State.playing);
            control.saves.zoneSave();
            logic.play();
            Events.fire(Trigger.newGame);
        });
    }

    public void playTutorial(){
        Zone zone = Zones.groundZero;
        ui.loadAnd(() -> {
            logic.reset();
            net.reset();

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

            zone.rules.get(state.rules);
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
            Events.fire(Trigger.newGame);
        });
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        content.dispose();
        net.dispose();
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
        platform.updateRPC();

        //play tutorial on stop
        if(!settings.getBool("playedtutorial", false)){
            Core.app.post(() -> Core.app.post(this::playTutorial));
        }

        //display UI scale changed dialog
        if(Core.settings.getBool("uiscalechanged", false)){
            Core.app.post(() -> Core.app.post(() -> {
                FloatingDialog dialog = new FloatingDialog("$confirm");
                dialog.setFillParent(true);

                float[] countdown = {60 * 11};
                Runnable exit = () -> {
                    Core.settings.put("uiscale", 100);
                    Core.settings.put("uiscalechanged", false);
                    settings.save();
                    dialog.hide();
                    Core.app.exit();
                };

                dialog.cont.label(() -> {
                    if(countdown[0] <= 0){
                        exit.run();
                    }
                    return Core.bundle.format("uiscale.reset", (int)((countdown[0] -= Time.delta()) / 60f));
                }).pad(10f).expand().center();

                dialog.buttons.defaults().size(200f, 60f);
                dialog.buttons.addButton("$uiscale.cancel", exit);

                dialog.buttons.addButton("$ok", () -> {
                    Core.settings.put("uiscalechanged", false);
                    settings.save();
                    dialog.hide();
                });

                dialog.show();
            }));
        }

        if(android){
            Sounds.empty.loop(0f, 1f, 0f);
        }
    }

    @Override
    public void update(){
        //TODO find out why this happens on Android
        if(assets == null) return;

        saves.update();

        //update and load any requested assets
        try{
            assets.update();
        }catch(Exception ignored){
        }

        input.updateState();

        //autosave global data if it's modified
        data.checkSave();

        music.update();
        loops.update();
        Time.updateGlobal();

        if(Core.input.keyTap(Binding.fullscreen)){
            boolean full = settings.getBool("fullscreen");
            if(full){
                graphics.setWindowedMode(graphics.getWidth(), graphics.getHeight());
            }else{
                graphics.setFullscreenMode(graphics.getDisplayMode());
            }
            settings.put("fullscreen", !full);
            settings.save();
        }

        if(!state.is(State.menu)){
            input.update();

            if(world.isZone()){
                for(Tile tile : state.teams.get(player.getTeam()).cores){
                    for(Item item : content.items()){
                        if(tile.entity != null && tile.entity.items.has(item)){
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
                platform.updateRPC();
            }

            if(Core.input.keyTap(Binding.pause) && !scene.hasDialog() && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
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
                platform.hide();
            }
        }
    }
}
