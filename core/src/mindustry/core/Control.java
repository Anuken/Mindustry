package mindustry.core;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Saves.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
import static mindustry.Vars.net;
import static mindustry.Vars.*;

/**
 * Control module.
 * Handles all input, saving, keybinds and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener, Loadable{
    public Saves saves;
    public mindustry.audio.MusicControl music;
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
            player.team(netServer.assignTeam(player));
            player.add();

            state.set(State.playing);
        });

        Events.on(WorldLoadEvent.class, event -> {
            if(Mathf.zero(player.x()) && Mathf.zero(player.y())){
                Tilec core = state.teams.closestCore(0, 0, player.team());
                if(core != null){
                    player.set(core);
                    camera.position.set(core);
                }
            }else{
                camera.position.set(player);
            }
        });

        Events.on(SaveLoadEvent.class, event -> {
            input.checkUnit();
        });

        Events.on(ResetEvent.class, event -> {
            player.reset();
            tutorial.reset();

            hiscore = false;

            saves.resetSave();
        });

        Events.on(WaveEvent.class, event -> {
            if(state.map.getHightScore() < state.wave){
                hiscore = true;
                state.map.setHighScore(state.wave);
            }

            Sounds.wave.play();
        });

        Events.on(GameOverEvent.class, event -> {
            state.stats.wavesLasted = state.wave;
            Effects.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.onGameOver(event.winner);
            //TODO set meta to indicate game over
            /*
            if(state.rules.zone != null && !net.client()){
                //remove zone save on game over
                if(saves.getZoneSlot() != null && !state.rules.tutorial){
                    saves.getZoneSlot().delete();
                }
            }*/
        });

        //autohost for pvp maps
        Events.on(WorldLoadEvent.class, event -> app.post(() -> {
            player.add();
            if(state.rules.pvp && !net.active()){
                try{
                    net.host(port);
                    player.admin(true);
                }catch(IOException e){
                    ui.showException("$server.error", e);
                    state.set(State.menu);
                }
            }
        }));

        Events.on(UnlockEvent.class, e -> ui.hudfrag.showUnlock(e.content));

        Events.on(BlockBuildEndEvent.class, e -> {
            if(e.team == player.team()){
                if(e.breaking){
                    state.stats.buildingsDeconstructed++;
                }else{
                    state.stats.buildingsBuilt++;
                }
            }
        });

        Events.on(BlockDestroyEvent.class, e -> {
            if(e.tile.team() == player.team()){
                state.stats.buildingsDestroyed++;
            }
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(e.unit.team() != player.team()){
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
            Tilec core = player.closestCore();

            if(core == null) return;

            //TODO this sounds pretty bad due to conflict
            if(settings.getInt("musicvol") > 0){
                Musics.land.stop();
                Musics.land.play();
                Musics.land.setVolume(settings.getInt("musicvol") / 100f);
            }

            app.post(() -> ui.hudfrag.showLand());
            renderer.zoomIn(Fx.coreLand.lifetime);
            app.post(() -> Fx.coreLand.at(core.getX(), core.getY(), 0, core.block()));
            Time.run(Fx.coreLand.lifetime, () -> {
                Fx.launch.at(core);
                Effects.shake(5f, 5f, core);
            });
        });

        Events.on(UnitDestroyEvent.class, e -> {
            if(state.isCampaign()){
                data.unlockContent(e.unit.type());
            }
        });
    }

    @Override
    public void loadAsync(){
        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();

        Core.input.setCatch(KeyCode.back, true);

        data.load();

        Core.settings.defaults(
        "ip", "localhost",
        "color-0", playerColors[8].rgba(),
        "name", "",
        "lastBuild", 0
        );

        createPlayer();

        saves.load();
    }

    void createPlayer(){
        player = PlayerEntity.create();
        player.name(Core.settings.getString("name"));
        player.color().set(Core.settings.getInt("color-0"));

        if(mobile){
            input = new MobileInput();
        }else{
            input = new DesktopInput();
        }

        if(state.isGame()){
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
            state.rules.sector = null;
            state.rules.editor = false;
            logic.play();
            if(settings.getBool("savecreate") && !world.isInvalidMap()){
                control.saves.addSave(map.name() + " " + new SimpleDateFormat("MMM dd h:mm", Locale.getDefault()).format(new Date()));
            }
            Events.fire(Trigger.newGame);
        });
    }

    public void playSector(Sector sector){
        ui.loadAnd(() -> {
            ui.planet.hide();
            SaveSlot slot = sector.save;
            if(slot != null && !clearSectors){
                try{
                    net.reset();
                    slot.load();
                    state.rules.sector = sector;
                    state.set(State.playing);
                }catch(SaveException e){
                    Log.err(e);
                    sector.save = null;
                    ui.showErrorMessage("$save.corrupted");
                    slot.delete();
                    playSector(sector);
                }
                ui.planet.hide();
            }else{
                net.reset();
                logic.reset();
                world.loadSector(sector);
                state.rules.sector = sector;
                logic.play();
                control.saves.saveSector(sector);
                Events.fire(Trigger.newGame);
            }
        });
    }

    public void playTutorial(){
        //TODO implement
        //ui.showInfo("death");
        /*
        Zone zone = Zones.groundZero;
        ui.loadAnd(() -> {
            logic.reset();
            net.reset();

            world.beginMapLoad();

            world.resize(zone.generator.width, zone.generator.height);
            zone.generator.generate(world.tiles);

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
                if(tile != null && tile.team() == state.rules.defaultTeam && !(tile.block() instanceof CoreBlock)){
                    tile.remove();
                }
            });

            Geometry.circle(coreb.x, coreb.y, 5, (cx, cy) -> world.tile(cx, cy).clearOverlay());

            world.endMapLoad();

            zone.rules.get(state.rules);
            //TODO assign zone!!
            //state.rules.zone = zone;
            for(Tilec core : state.teams.playerCores()){
                for(ItemStack stack : zone.getStartingItems()){
                    core.items().add(stack.item, stack.amount);
                }
            }
            Tilec core = state.teams.playerCores().first();
            core.items().clear();

            logic.play();
            state.rules.waveTimer = false;
            state.rules.waveSpacing = 60f * 30;
            state.rules.buildCostMultiplier = 0.3f;
            state.rules.tutorial = true;
            Events.fire(Trigger.newGame);
        });*/
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        //try to save when exiting
        if(saves != null && saves.getCurrent() != null && saves.getCurrent().isAutosave() && !net.client() && !state.isMenu()){
            try{
                SaveIO.save(control.saves.getCurrent().file);
                Log.info("Saved on exit.");
            }catch(Throwable e){
                e.printStackTrace();
            }
        }

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
                dialog.buttons.button("$uiscale.cancel", exit);

                dialog.buttons.button("$ok", () -> {
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

        if(state.isGame()){
            input.update();

            if(state.isCampaign()){
                for(Tilec tile : state.teams.cores(player.team())){
                    for(Item item : content.items()){
                        if(tile.items().has(item)){
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

            if(Core.input.keyTap(Binding.pause) && !scene.hasDialog() && !scene.hasKeyboard() && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.is(State.playing) ? State.paused : State.playing);
            }

            if(Core.input.keyTap(Binding.menu) && !ui.restart.isShown() && !ui.minimapfrag.shown()){
                if(ui.chatfrag.shown()){
                    ui.chatfrag.hide();
                }else if(!ui.paused.isShown() && !scene.hasDialog()){
                    ui.paused.show();
                    state.set(State.paused);
                }
            }

            if(!mobile && Core.input.keyTap(Binding.screenshot) && !(scene.getKeyboardFocus() instanceof TextField) && !scene.hasKeyboard()){
                renderer.takeMapScreenshot();
            }

        }else{
            if(!state.isPaused()){
                Time.update();
            }

            if(!scene.hasDialog() && !scene.root.getChildren().isEmpty() && !(scene.root.getChildren().peek() instanceof Dialog) && Core.input.keyTap(KeyCode.back)){
                platform.hide();
            }
        }
    }
}
