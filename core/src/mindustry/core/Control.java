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
import mindustry.content.TechTree.*;
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
import mindustry.net.*;
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
 * Handles all input, saving and keybinds.
 * Should <i>not</i> handle any logic-critical state.
 * This class is not created in the headless server.
 */
public class Control implements ApplicationListener, Loadable{
    public Saves saves;
    public SoundControl sound;
    public InputHandler input;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false;

    public Control(){
        saves = new Saves();
        sound = new SoundControl();

        Events.on(StateChangeEvent.class, event -> {
            if((event.from == State.playing && event.to == State.menu) || (event.from == State.menu && event.to != State.menu)){
                Time.runTask(5f, platform::updateRPC);
            }
        });

        Events.on(PlayEvent.class, event -> {
            player.team(netServer.assignTeam(player));
            player.add();

            state.set(State.playing);
        });

        Events.on(WorldLoadEvent.class, event -> {
            if(Mathf.zero(player.x) && Mathf.zero(player.y)){
                Building core = state.teams.closestCore(0, 0, player.team());
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
            Effect.shake(5, 6, Core.camera.position.x, Core.camera.position.y);
            //the restart dialog can show info for any number of scenarios
            Call.gameOver(event.winner);
        });

        //add player when world loads regardless
        Events.on(WorldLoadEvent.class, e -> {
            player.add();
        });

        //autohost for pvp maps
        Events.on(WorldLoadEvent.class, event -> app.post(() -> {
            if(state.rules.pvp && !net.active()){
                try{
                    net.host(port);
                    player.admin(true);
                }catch(IOException e){
                    ui.showException("@server.error", e);
                    state.set(State.menu);
                }
            }
        }));

        Events.on(UnlockEvent.class, e -> ui.hudfrag.showUnlock(e.content));

        Events.on(UnlockEvent.class, e -> {
            checkAutoUnlocks();
        });

        Events.on(SectorCaptureEvent.class, e -> {
            checkAutoUnlocks();
        });

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

        //delete save on campaign game over
        Events.on(GameOverEvent.class, e -> {
            if(state.isCampaign() && !net.client() && !headless){

                //save gameover sate immediately
                if(saves.getCurrent() != null){
                    saves.getCurrent().save();
                }
            }
        });

        Events.run(Trigger.newGame, () -> {
            Building core = player.closestCore();

            if(core == null) return;

            //TODO this sounds pretty bad due to conflict
            if(settings.getInt("musicvol") > 0){
                Musics.land.stop();
                Musics.land.play();
                Musics.land.setVolume(settings.getInt("musicvol") / 100f);
            }

            app.post(() -> ui.hudfrag.showLand());
            renderer.zoomIn(Fx.coreLand.lifetime);
            app.post(() -> Fx.coreLand.at(core.getX(), core.getY(), 0, core.block));
            camera.position.set(core);
            player.set(core);

            Time.run(Fx.coreLand.lifetime, () -> {
                Fx.launch.at(core);
                Effect.shake(5f, 5f, core);

                if(state.isCampaign()){
                    ui.announce("[accent]" + state.rules.sector.name() + "\n" +
                        (state.rules.sector.info.resources.any() ? "[lightgray]" + bundle.get("sectors.resources") + "[white] " +
                            state.rules.sector.info.resources.toString(" ", u -> u.emoji()) : ""), 5);
                }
            });
        });

    }

    @Override
    public void loadAsync(){
        Draw.scl = 1f / Core.atlas.find("scale_marker").width;

        Core.input.setCatch(KeyCode.back, true);

        Core.settings.defaults(
        "ip", "localhost",
        "color-0", playerColors[8].rgba(),
        "name", "",
        "lastBuild", 0
        );

        createPlayer();

        saves.load();
    }

    /** Automatically unlocks things with no requirements. */
    void checkAutoUnlocks(){
        if(net.client()) return;

        for(TechNode node : TechTree.all){
            if(!node.content.unlocked() && node.requirements.length == 0 && !node.objectives.contains(o -> !o.complete())){
                node.content.unlocked();
            }
        }
    }

    void createPlayer(){
        player = Player.create();
        player.name = Core.settings.getString("name");
        player.color.set(Core.settings.getInt("color-0"));

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
        playSector(sector, sector);
    }

    public void playSector(@Nullable Sector origin, Sector sector){
        playSector(origin, sector, new WorldReloader());
    }

    void playSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        ui.loadAnd(() -> {
            if(saves.getCurrent() != null && state.isGame()){
                control.saves.getCurrent().save();
                control.saves.resetSave();
            }

            ui.planet.hide();
            SaveSlot slot = sector.save;
            sector.planet.setLastSector(sector);
            if(slot != null && !clearSectors){

                try{
                    reloader.begin();
                    slot.load();
                    slot.setAutosave(true);
                    state.rules.sector = sector;

                    //if there is no base, simulate a new game and place the right loadout at the spawn position
                    if(state.rules.defaultTeam.cores().isEmpty()){

                        //no spawn set -> delete the sector save
                        if(sector.info.spawnPosition == 0){
                            //delete old save
                            sector.save = null;
                            slot.delete();
                            //play again
                            playSector(origin, sector, reloader);
                            return;
                        }

                        //reset wave so things are more fair
                        state.wave = 1;

                        //reset win wave??
                        state.rules.winWave = state.rules.attackMode ? -1 : sector.preset != null ? sector.preset.captureWave : 40;

                        //kill all units, since they should be dead anyway
                        Groups.unit.clear();
                        Groups.fire.clear();

                        Tile spawn = world.tile(sector.info.spawnPosition);
                        Schematics.placeLaunchLoadout(spawn.x, spawn.y);

                        //set up camera/player locations
                        player.set(spawn.x * tilesize, spawn.y * tilesize);
                        camera.position.set(player);

                        Events.fire(new SectorLaunchEvent(sector));
                        Events.fire(Trigger.newGame);
                    }

                    state.set(State.playing);
                    reloader.end();

                }catch(SaveException e){
                    Log.err(e);
                    sector.save = null;
                    Time.runTask(10f, () -> ui.showErrorMessage("@save.corrupted"));
                    slot.delete();
                    playSector(origin, sector);
                }
                ui.planet.hide();
            }else{
                reloader.begin();
                world.loadSector(sector);
                state.rules.sector = sector;
                //assign origin when launching
                sector.info.origin = origin;
                sector.info.destination = origin;
                logic.play();
                control.saves.saveSector(sector);
                Events.fire(new SectorLaunchEvent(sector));
                Events.fire(Trigger.newGame);
                reloader.end();
            }
        });
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
            }catch(Throwable t){
                Log.err(t);
            }
        }

        for(Music music : assets.getAll(Music.class, new Seq<>())){
            music.stop();
        }

        content.dispose();
        net.dispose();
        Musics.dispose();
        Sounds.dispose();
        ui.editor.dispose();
    }

    @Override
    public void pause(){
        if(settings.getBool("backgroundpause", true)){
            wasPaused = state.is(State.paused);
            if(state.is(State.playing)) state.set(State.paused);
        }
    }

    @Override
    public void resume(){
        if(state.is(State.paused) && !wasPaused && settings.getBool("backgroundpause", true)){
            state.set(State.playing);
        }
    }

    @Override
    public void init(){
        platform.updateRPC();

        //display UI scale changed dialog
        if(Core.settings.getBool("uiscalechanged", false)){
            Core.app.post(() -> Core.app.post(() -> {
                BaseDialog dialog = new BaseDialog("@confirm");
                dialog.setFillParent(true);

                float[] countdown = {60 * 11};
                Runnable exit = () -> {
                    Core.settings.put("uiscale", 100);
                    Core.settings.put("uiscalechanged", false);
                    dialog.hide();
                    Core.app.exit();
                };

                dialog.cont.label(() -> {
                    if(countdown[0] <= 0){
                        exit.run();
                    }
                    return Core.bundle.format("uiscale.reset", (int)((countdown[0] -= Time.delta) / 60f));
                }).pad(10f).expand().center();

                dialog.buttons.defaults().size(200f, 60f);
                dialog.buttons.button("@uiscale.cancel", exit);

                dialog.buttons.button("@ok", () -> {
                    Core.settings.put("uiscalechanged", false);
                    dialog.hide();
                });

                dialog.show();
            }));
        }
    }

    @Override
    public void update(){
        //this happens on Android and nobody knows why
        if(assets == null) return;

        saves.update();

        //update and load any requested assets
        try{
            assets.update();
        }catch(Exception ignored){
        }

        input.updateState();

        sound.update();

        if(Core.input.keyTap(Binding.fullscreen)){
            boolean full = settings.getBool("fullscreen");
            if(full){
                graphics.setWindowedMode(graphics.getWidth(), graphics.getHeight());
            }else{
                graphics.setFullscreenMode(graphics.getDisplayMode());
            }
            settings.put("fullscreen", !full);
        }

        if(state.isGame()){
            input.update();

            //auto-update rpc every 5 seconds
            if(timer.get(0, 60 * 5)){
                platform.updateRPC();
            }

            //unlock core items
            var core = state.rules.defaultTeam.core();
            if(!net.client() && core != null && state.isCampaign()){
                core.items.each((i, a) -> i.unlock());
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
            //this runs in the menu
            if(!state.isPaused()){
                Time.update();
            }

            if(!scene.hasDialog() && !scene.root.getChildren().isEmpty() && !(scene.root.getChildren().peek() instanceof Dialog) && Core.input.keyTap(KeyCode.back)){
                platform.hide();
            }
        }
    }
}
