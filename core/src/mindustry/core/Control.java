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
import arc.util.ArcAnnotate.*;
import mindustry.*;
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
import mindustry.world.blocks.storage.CoreBlock.*;

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
                for(Sound sound : assets.getAll(Sound.class, new Seq<>())){
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
            Call.gameOver(event.winner);
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

        //delete save on campaign game over
        Events.on(GameOverEvent.class, e -> {
            if(state.isCampaign() && !net.client() && !headless){

                //delete the save, it is gone.
                if(saves.getCurrent() != null && !state.rules.tutorial){
                    Sector sector = state.getSector();
                    sector.save = null;
                    saves.getCurrent().delete();
                }
            }
        });

        Events.on(Trigger.newGame, () -> {
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
            app.post(() -> Fx.coreLand.at(core.getX(), core.getY(), 0, core.block()));
            Time.run(Fx.coreLand.lifetime, () -> {
                Fx.launch.at(core);
                Effects.shake(5f, 5f, core);
            });
        });

    }

    @Override
    public void loadAsync(){
        Draw.scl = 1f / Core.atlas.find("scale_marker").getWidth();

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

    //TODO move
    public void handleLaunch(CoreEntity tile){
        LaunchCorec ent = LaunchCore.create();
        ent.set(tile);
        ent.block(Blocks.coreShard);
        ent.lifetime(Vars.launchDuration);
        ent.add();

        //remove schematic requirements from core
        tile.items.remove(universe.getLastLoadout().requirements());
        tile.items.remove(universe.getLaunchResources());
    }

    public void playSector(Sector sector){
        playSector(sector, sector);
    }

    public void playSector(@Nullable Sector origin, Sector sector){
        ui.loadAnd(() -> {
            ui.planet.hide();
            SaveSlot slot = sector.save;
            sector.planet.setLastSector(sector);
            if(slot != null && !clearSectors){

                try{
                    net.reset();
                    slot.load();
                    state.rules.sector = sector;

                    //if there is no base, simulate a new game and place the right loadout at the spawn position
                    //TODO this is broken?
                    if(state.rules.defaultTeam.cores().isEmpty()){

                        //kill all friendly units, since they should be dead anwyay
                        for(Unit unit : Groups.unit){
                            if(unit.team() == state.rules.defaultTeam){
                                unit.remove();
                            }
                        }

                        Tile spawn = world.tile(sector.getSpawnPosition());
                        //TODO PLACE CORRECT LOADOUT
                        Schematics.placeLoadout(universe.getLastLoadout(), spawn.x, spawn.y);

                        //set up camera/player locations
                        player.set(spawn.x * tilesize, spawn.y * tilesize);
                        camera.position.set(player);

                        Events.fire(Trigger.newGame);
                    }

                    state.set(State.playing);

                }catch(SaveException e){
                    Log.err(e);
                    sector.save = null;
                    Time.runTask(10f, () -> ui.showErrorMessage("$save.corrupted"));
                    slot.delete();
                    playSector(origin, sector);
                }
                ui.planet.hide();
            }else{
                net.reset();
                logic.reset();
                world.loadSector(sector);
                state.rules.sector = sector;
                //assign origin when launching
                state.secinfo.origin = origin;
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
            for(Building core : state.teams.playerCores()){
                for(ItemStack stack : zone.getStartingItems()){
                    core.items.add(stack.item, stack.amount);
                }
            }
            Building core = state.teams.playerCores().first();
            core.items.clear();

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

        //just a regular reminder
        if(!OS.prop("user.name").equals("anuke") && !OS.hasEnv("iknowwhatimdoing")){
            app.post(() -> app.post(() -> {
                ui.showStartupInfo("[accent]v6[] is currently in [accent]pre-alpha[].\n" +
                "[lightgray]This means:[]\n" +
                "- Content is missing\n" +
                "- Most [scarlet]Unit AI[] does not work\n" +
                "- Many units are [scarlet]missing[] or unfinished\n" +
                "- The campaign is completely unfinished\n" +
                "- Everything you see is subject to change or removal." +
                "\n\nReport bugs or crashes on [accent]Github[].");
            }));
        }

        //play tutorial on stop
        if(!settings.getBool("playedtutorial", false)){
            //Core.app.post(() -> Core.app.post(this::playTutorial));
        }

        //display UI scale changed dialog
        if(Core.settings.getBool("uiscalechanged", false)){
            Core.app.post(() -> Core.app.post(() -> {
                BaseDialog dialog = new BaseDialog("$confirm");
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
                dialog.buttons.button("$uiscale.cancel", exit);

                dialog.buttons.button("$ok", () -> {
                    Core.settings.put("uiscalechanged", false);
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

        music.update();
        loops.update();

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

            if(state.rules.tutorial){
                tutorial.update();
            }

            //auto-update rpc every 5 seconds
            if(timer.get(0, 60 * 5)){
                platform.updateRPC();
            }

            if(Core.input.keyTap(Binding.pause) && !state.isOutOfTime() && !scene.hasDialog() && !scene.hasKeyboard() && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
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
