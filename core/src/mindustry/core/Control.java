package mindustry.core;

import arc.*;
import arc.assets.*;
import arc.audio.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.audio.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.GameState.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.game.Saves.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.Map;
import mindustry.maps.*;
import mindustry.net.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static arc.Core.*;
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
    public AttackIndicators indicators;

    private Interval timer = new Interval(2);
    private boolean hiscore = false;
    private boolean wasPaused = false, backgroundPaused = false;
    private Seq<Building> toBePlaced = new Seq<>(false);
    private Seq<Object[]> toBePlacedConfigs = new Seq<>();

    public Control(){
        saves = new Saves();
        sound = new SoundControl();
        indicators = new AttackIndicators();

        Events.on(BuildDamageEvent.class, e -> {
            if(e.build.team == Vars.player.team()){
                indicators.add(e.build.tileX(), e.build.tileY());
            }
        });

        //show dialog saying that mod loading was skipped.
        Events.on(ClientLoadEvent.class, e -> {
            if(Vars.mods.skipModLoading() && Vars.mods.list().any()){
                Time.runTask(4f, () -> {
                    ui.showInfo("@mods.initfailed");
                });
            }
            checkAutoUnlocks();
        });

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
                Building core = player.bestCore();
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
            toBePlaced.clear();
            toBePlacedConfigs.clear();
            indicators.clear();

            hiscore = false;
            saves.resetSave();
        });

        Events.on(WaveEvent.class, event -> {
            if(state.map.getHightScore() < state.wave){
                hiscore = true;
                state.map.setHighScore(state.wave);
            }

            Sounds.waveSpawn.play();
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
            //make player admin on any load when hosting
            if(net.active() && net.server()){
                player.admin = true;
            }
        });

        //autohost for pvp maps
        Events.on(WorldLoadEvent.class, event -> app.post(() -> {
            if(state.rules.pvp && !net.active()){
                try{
                    net.host(port);
                    player.admin = true;
                }catch(IOException e){
                    ui.showException("@server.error", e);
                    state.set(State.menu);
                }
            }
        }));

        Events.on(UnlockEvent.class, e -> {
            if(e.content.showUnlock()){
                ui.hudfrag.showUnlock(e.content);
            }

            checkAutoUnlocks();

            if(e.content instanceof SectorPreset){
                for(TechNode node : TechTree.all){
                    if(!node.content.unlocked() && node.objectives.contains(o -> o instanceof SectorComplete sec && sec.preset == e.content) && !node.objectives.contains(o -> !o.complete())){
                        ui.hudfrag.showToast(new TextureRegionDrawable(node.content.uiIcon), iconLarge, bundle.get("available"));
                    }
                }
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            app.post(this::checkAutoUnlocks);

            if(!net.client() && e.sector.preset != null && e.sector.preset.isLastSector && e.initialCapture){
                Time.run(60f * 2f, () -> {
                    ui.campaignComplete.show(e.sector.planet);
                });
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
            var core = player.bestCore();
            if(core == null) return;

            camera.position.set(core);
            player.set(core);

            float coreDelay = 0f;
            if(!settings.getBool("skipcoreanimation") && !state.rules.pvp){
                coreDelay = core.launchDuration();
                //delay player respawn so animation can play.
                player.deathTimer = Player.deathDelay - core.launchDuration();
                //TODO this sounds pretty bad due to conflict
                if(settings.getInt("musicvol") > 0){
                    //TODO what to do if another core with different music is already playing?
                    Music music = core.landMusic();
                   if(music != null){
                       music.stop();
                       music.play();
                       music.setVolume(settings.getInt("musicvol") / 100f);
                   }
                }

                renderer.showLanding(core);
            }

            if(state.isCampaign()){
                if(state.rules.sector.info.importRateCache != null){
                    state.rules.sector.info.refreshImportRates(state.rules.sector.planet);
                }

                //don't run when hosting, that doesn't really work.
                if(state.rules.sector.planet.prebuildBase){
                    toBePlaced.clear();
                    float unitsPerTick = 2f;
                    float buildRadius = state.rules.enemyCoreBuildRadius * 1.5f;

                    //TODO if the save is unloaded or map is hosted, these blocks do not get built.
                    boolean anyBuilds = false;
                    float maxDelay = 0f;

                    for(var build : state.rules.defaultTeam.data().buildings){
                        //some blocks need to be configured later once everything is built
                        if(build.block.delayLandingConfig){
                            toBePlacedConfigs.add(new Object[]{build, build.config()});
                        }
                    }

                    for(var build : state.rules.defaultTeam.data().buildings.copy()){
                        if(!(build instanceof CoreBuild) && !build.block.privileged){
                            var ccore = build.closestCore();

                            if(ccore != null){
                                anyBuilds = true;

                                if(!net.active()){
                                    build.pickedUp();
                                    build.tile.remove();

                                    toBePlaced.add(build);
                                    float delay = build.dst(ccore) / unitsPerTick + coreDelay;
                                    maxDelay = Math.max(delay, maxDelay);

                                    Time.run(delay, () -> {
                                        if(build.tile.build != build){
                                            placeLandBuild(build);

                                            toBePlaced.remove(build);
                                        }
                                    });
                                }else{
                                    //when already hosting, instantly build everything. this looks bad but it's better than a desync
                                    Fx.coreBuildBlock.at(build.x, build.y, 0f, build.block);
                                    build.block.placeEffect.at(build.x, build.y, build.block.size);
                                }
                            }
                        }
                    }

                    if(anyBuilds){
                        Time.run(maxDelay + 1f, this::configurePlaced);
                        for(var ccore : state.rules.defaultTeam.data().cores){
                            Time.run(coreDelay, () -> {
                                Fx.coreBuildShockwave.at(ccore.x, ccore.y, buildRadius);
                            });
                        }
                    }
                }
            }
        });

        Events.on(SaveWriteEvent.class, e -> forcePlaceAll());
        Events.on(HostEvent.class, e -> forcePlaceAll());
        Events.on(HostEvent.class, e -> {
            state.set(State.playing);
        });
    }

    private void forcePlaceAll(){
        //force set buildings when a save is done or map is hosted, to prevent desyncs
        for(var build : toBePlaced){
            placeLandBuild(build);
        }

        configurePlaced();
        toBePlaced.clear();
    }

    private void configurePlaced(){
        for(Object[] obj : toBePlacedConfigs){
            Building build = (Building)obj[0];
            Object config = obj[1];
            build.configureAny(config);
        }
        toBePlacedConfigs.clear();
    }

    private void placeLandBuild(Building build){
        build.tile.setBlock(build.block, build.team, build.rotation, () -> build);
        build.dropped();

        Fx.coreBuildBlock.at(build.x, build.y, 0f, build.block);
        build.block.placeEffect.at(build.x, build.y, build.block.size);
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

        saves.load();
    }

    @Override
    public void loadSync(){
        createPlayer();
    }

    /** Automatically unlocks things with no requirements and no locked parents. */
    public void checkAutoUnlocks(){
        if(net.client()) return;

        for(TechNode node : TechTree.all){
            if(!node.content.unlocked() && (node.parent == null || node.parent.content.unlocked()) && node.requirements.length == 0 && !node.objectives.contains(o -> !o.complete())){
                node.content.unlock();
            }
        }
    }

    void createPlayer(){
        player = Player.create();
        player.name = Core.settings.getString("name");

        String locale = Core.settings.getString("locale");
        if(locale.equals("default")){
            locale = Locale.getDefault().toString();
        }
        player.locale = locale;

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
        playMap(map, rules, false);
    }

    public void playMap(Map map, Rules rules, boolean playtest){
        ui.loadAnd(() -> {
            logic.reset();
            world.loadMap(map, rules);
            state.rules = rules;
            if(playtest) state.playtestingMap = map;
            state.rules.sector = null;
            state.rules.editor = false;
            logic.play();
            if(settings.getBool("savecreate") && !world.isInvalidMap() && !playtest){
                control.saves.addSave(map.name() + " " + new SimpleDateFormat("MMM dd h:mm", Locale.getDefault()).format(new Date()));
            }
            Events.fire(Trigger.newGame);

            //booted out of map, resume editing
            if(world.isInvalidMap() && playtest){
                Dialog current = scene.getDialog();
                ui.editor.resumeAfterPlaytest(map);
                if(current != null){
                    current.update(current::toFront);
                }
            }
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

            if(sector.preset != null){
                sector.preset.quietUnlock();
            }

            ui.planet.hide();
            SaveSlot slot = sector.save;
            sector.planet.setLastSector(sector);

            boolean clearSave = sector.planet.clearSectorOnLose || sector.planet.campaignRules.clearSectorOnLose;

            if(slot != null && !clearSectors && (!clearSave || sector.info.hasCore)){

                try{
                    boolean hadNoCore = !sector.info.hasCore;
                    reloader.begin();
                    //pass in a sector context to make absolutely sure the correct sector is written; it may differ from what's in the meta due to remapping.
                    slot.load(world.makeSectorContext(sector));
                    slot.setAutosave(true);
                    state.rules.sector = sector;
                    state.rules.cloudColor = sector.planet.landCloudColor;

                    //if there is no base, simulate a new game and place the right loadout at the spawn position
                    if(state.rules.defaultTeam.cores().isEmpty() || hadNoCore){

                        //don't carry over the spawn position and plans if the sector preset name or map size changed
                        if(clearSave || sector.info.spawnPosition == 0 || !sector.info.sectorDataMatches(sector)){
                            playNewSector(origin, sector, reloader);
                        }else{
                            int spawnPos = sector.info.spawnPosition;

                            //set spawn for sector damage to use
                            Tile spawn = world.tile(spawnPos);
                            if(spawn == null){
                                playNewSector(origin, sector, reloader);
                                return;
                            }
                            spawn.setBlock(sector.planet.defaultCore, state.rules.defaultTeam);

                            //apply damage to simulate the sector being lost
                            SectorDamage.apply(1f);

                            //save the plans and buildings from the previous save; they will be used to re-populate the sector
                            var previousPlans = state.rules.defaultTeam.data().plans.toArray(BlockPlan.class);
                            var previousBuildings = state.rules.defaultTeam.data().buildings.<Building>toArray(Building.class);
                            var previousDerelicts = Team.derelict.data().buildings.<Building>toArray(Building.class);

                            logic.reset();

                            //now, load a fresh save; the old one was only used to grab previous building data
                            playNewSector(origin, sector, reloader, new WorldParams(){{
                                corePositionOverride = spawnPos;
                            }}, () -> {
                                var teamData = state.rules.defaultTeam.data();

                                //all the derelicts from the new save have to be removed.
                                for(var generatedDerelict : Team.derelict.data().buildings.<Building>toArray(Building.class)){
                                    generatedDerelict.tile.remove();
                                }

                                //retain old derelicts from the previous save.
                                for(var build : previousDerelicts){
                                    Tile tile = world.tile(build.tileX(), build.tileY());
                                    if(tile != null && tile.build == null && Build.validPlace(build.block, Team.derelict, build.tileX(), build.tileY(), build.rotation, false, false)){
                                        tile.setBlock(build.block, Team.derelict, build.rotation, () -> build);
                                    }
                                }

                                //all the derelict power graphs are invalid
                                for(var build : previousBuildings){
                                    if(build.power != null){
                                        build.power.graph = new PowerGraph();
                                        build.power.links.clear();
                                    }
                                }

                                //copy over all buildings from the previous save, retaining config and health, and making them derelict
                                for(var build : previousBuildings){
                                    Tile tile = world.tile(build.tileX(), build.tileY());
                                    if(tile != null && tile.build == null && Build.validPlace(build.block, state.rules.defaultTeam, build.tileX(), build.tileY(), build.rotation, false, false)){
                                        build.addPlan(false, true);
                                        tile.setBlock(build.block, state.rules.defaultTeam, build.rotation, () -> build);
                                        build.changeTeam(Team.derelict);
                                        build.dropped(); //TODO: call pickedUp too? this may screw up power networks in a major way as they refer to potentially deleted entities
                                    }
                                }

                                for(var build : previousBuildings){
                                    if(build.isValid()){
                                        build.updateProximity();
                                    }
                                }

                                //carry over all previous plans that don't already have the corresponding block at their position
                                for(var plan : previousPlans){
                                    var build = world.build(plan.x, plan.y);
                                    if(!(build != null && build.block == plan.block && build.tileX() == plan.x && build.tileY() == plan.y && build.team != state.rules.waveTeam)){
                                        teamData.plans.add(plan);
                                    }
                                }
                            });

                            Core.app.post(() -> {
                                //blocks placed after WorldLoadEvent didn't queue an update, so fix that.
                                renderer.minimap.updateAll();
                            });
                        }
                    }else{
                        state.set(State.playing);
                        reloader.end();
                    }

                }catch(SaveException e){
                    Log.err(e);
                    sector.save = null;
                    Time.runTask(10f, () -> ui.showErrorMessage("@save.corrupted"));
                    slot.delete();
                    playSector(origin, sector);
                }
                ui.planet.hide();
            }else{
                playNewSector(origin, sector, reloader);
            }
        });
    }

    public void playNewSector(@Nullable Sector origin, Sector sector, WorldReloader reloader){
        playNewSector(origin, sector, reloader, new WorldParams(), null);
    }

    public void playNewSector(@Nullable Sector origin, Sector sector, WorldReloader reloader, WorldParams params, @Nullable Runnable beforePlay){
        reloader.begin();
        world.loadSector(sector, params);
        state.rules.sector = sector;
        sector.info.origin = origin;
        sector.info.destination = origin;
        sector.info.attempts ++;

        if(beforePlay != null){
            beforePlay.run();
        }

        logic.play();
        control.saves.saveSector(sector);
        Events.fire(new SectorLaunchEvent(sector));
        Events.fire(Trigger.newGame);
        reloader.end();
        state.set(State.playing);
    }

    public boolean isHighScore(){
        return hiscore;
    }

    @Override
    public void dispose(){
        //try to save when exiting
        if(saves != null && saves.getCurrent() != null && saves.getCurrent().isAutosave() && !net.client() && !state.isMenu() && !state.gameOver){
            try{
                SaveIO.save(control.saves.getCurrent().file);
                settings.forceSave();
                Log.info("Saved on exit.");
            }catch(Throwable t){
                Log.err(t);
            }
        }

        for(Music music : assets.getAll(Music.class, new Seq<>())){
            music.stop();
        }

        net.dispose();
    }

    @Override
    public void pause(){
        if(settings.getBool("backgroundpause", true) && !net.active()){
            backgroundPaused = true;
            wasPaused = state.is(State.paused);
            if(state.is(State.playing)) state.set(State.paused);
        }
    }

    @Override
    public void resume(){
        if(state.is(State.paused) && !wasPaused && settings.getBool("backgroundpause", true) && !net.active()){
            state.set(State.playing);
        }
        backgroundPaused = false;
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
                graphics.setFullscreen();
            }
            settings.put("fullscreen", !full);
        }

        if(Float.isNaN(Vars.player.x) || Float.isNaN(Vars.player.y)){
            player.set(0, 0);
            if(!player.dead()) player.unit().kill();
        }
        if(Float.isNaN(camera.position.x)) camera.position.x = world.unitWidth()/2f;
        if(Float.isNaN(camera.position.y)) camera.position.y = world.unitHeight()/2f;

        if(state.isGame()){
            input.update();
            if(!state.isPaused()){
                indicators.update();
            }

            //auto-update rpc every 5 seconds
            if(timer.get(0, 60 * 5)){
                platform.updateRPC();
            }

            //unlock core items
            var core = state.rules.defaultTeam.core();
            if(!net.client() && core != null && state.isCampaign()){
                core.items.each((i, a) -> i.unlock());
            }

            if(backgroundPaused && settings.getBool("backgroundpause") && !net.active()){
                state.set(State.paused);
            }

            //cannot launch while paused
            if(state.isPaused() && renderer.isCutscene()){
                state.set(State.playing);
            }

            if(!net.client() && Core.input.keyTap(Binding.pause) && !(state.isCampaign() && state.afterGameOver) && !renderer.isCutscene() && !scene.hasDialog() && !scene.hasKeyboard() && !ui.restart.isShown() && (state.is(State.paused) || state.is(State.playing))){
                state.set(state.isPaused() ? State.playing : State.paused);
            }

            if(state.isCampaign() && state.afterGameOver){
                state.set(State.paused);
            }

            if(Core.input.keyTap(Binding.menu) && !ui.restart.isShown() && !ui.minimapfrag.shown()){
                if(ui.chatfrag.shown()){
                    ui.chatfrag.hide();
                }else if(!ui.paused.isShown() && !scene.hasDialog()){
                    ui.paused.show();
                    if(!net.active()){
                        state.set(State.paused);
                    }
                }
            }

            if(!mobile && Core.input.keyTap(Binding.screenshot) && !scene.hasField() && !scene.hasKeyboard()){
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
