package mindustry.core;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import java.util.*;

import static mindustry.Vars.*;

/**
 * Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 * <p>
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic implements ApplicationListener{

    public Logic(){

        Events.on(BlockDestroyEvent.class, event -> {
            //blocks that get broken are appended to the team's broken block queue
            Tile tile = event.tile;
            Block block = tile.block();
            //skip null entities or un-rebuildables, for obvious reasons; also skip client since they can't modify these requests
            if(tile.build == null || !tile.block().rebuildable || net.client()) return;

            if(block instanceof ConstructBlock){

                ConstructBuild entity = tile.bc();

                //update block to reflect the fact that something was being constructed
                if(entity.cblock != null && entity.cblock.synthetic()){
                    block = entity.cblock;
                }else{
                    //otherwise this was a deconstruction that was interrupted, don't want to rebuild that
                    return;
                }
            }

            TeamData data = state.teams.get(tile.team());

            //remove existing blocks that have been placed here.
            //painful O(n) iteration + copy
            for(int i = 0; i < data.blocks.size; i++){
                BlockPlan b = data.blocks.get(i);
                if(b.x == tile.x && b.y == tile.y){
                    data.blocks.removeIndex(i);
                    break;
                }
            }

            data.blocks.addFirst(new BlockPlan(tile.x, tile.y, (short)tile.build.rotation, block.id, tile.build.config()));
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                TeamData data = state.teams.get(event.team);
                Iterator<BlockPlan> it = data.blocks.iterator();
                while(it.hasNext()){
                    BlockPlan b = it.next();
                    Block block = content.block(b.block);
                    if(event.tile.block().bounds(event.tile.x, event.tile.y, Tmp.r1).overlaps(block.bounds(b.x, b.y, Tmp.r2))){
                        it.remove();
                    }
                }
            }
        });

        Events.on(LaunchItemEvent.class, e -> state.secinfo.handleItemExport(e.stack));

        //when loading a 'damaged' sector, propagate the damage
        Events.on(WorldLoadEvent.class, e -> {
            if(state.isCampaign()){
                long seconds = state.rules.sector.getSecondsPassed();
                CoreBuild core = state.rules.defaultTeam.core();

                //apply fractional damage based on how many turns have passed for this sector
                float turnsPassed = seconds / (turnDuration / 60f);

                if(state.rules.sector.hasWaves() && turnsPassed > 0 && state.rules.sector.hasBase()){
                    SectorDamage.apply(turnsPassed / sectorDestructionTurns);
                }

                //add resources based on turns passed
                if(state.rules.sector.save != null && core != null){
                    //update correct storage capacity
                    state.rules.sector.save.meta.secinfo.storageCapacity = core.storageCapacity;

                    //add new items received
                    state.rules.sector.calculateReceivedItems().each((item, amount) -> core.items.add(item, amount));

                    //clear received items
                    state.rules.sector.setExtraItems(new ItemSeq());

                    //validation
                    for(Item item : content.items()){
                        //ensure positive items
                        if(core.items.get(item) < 0) core.items.set(item, 0);
                        //cap the items
                        if(core.items.get(item) > core.storageCapacity) core.items.set(item, core.storageCapacity);
                    }
                }

                state.rules.sector.setSecondsPassed(0);
            }

            //enable infinite ammo for wave team by default
            state.rules.waveTeam.rules().infiniteAmmo = true;

            //save settings
            Core.settings.manualSave();
        });

    }

    /** Adds starting items, resets wave time, and sets state to playing. */
    public void play(){
        state.set(State.playing);
        //grace period of 2x wave time before game starts
        state.wavetime = state.rules.waveSpacing * 2;
        Events.fire(new PlayEvent());

        //add starting items
        if(!state.isCampaign()){
            for(TeamData team : state.teams.getActive()){
                if(team.hasCore()){
                    Building entity = team.core();
                    entity.items.clear();
                    for(ItemStack stack : state.rules.loadout){
                        entity.items.add(stack.item, stack.amount);
                    }
                }
            }
        }
    }

    public void reset(){
        State prev = state.getState();
        //recreate gamestate - sets state to menu
        state = new GameState();
        //fire change event, since it was technically changed
        Events.fire(new StateChangeEvent(prev, State.menu));

        Groups.clear();
        Time.clear();
        Events.fire(new ResetEvent());

        //save settings on reset
        Core.settings.manualSave();
    }

    public void skipWave(){
        if(state.isCampaign()){
            //warp time spent forward because the wave was just skipped.
            state.secinfo.internalTimeSpent += state.wavetime;
        }

        state.wavetime = 0;
    }

    public void runWave(){
        spawner.spawnEnemies();
        state.wave++;
        state.wavetime = state.hasSector() && state.getSector().isLaunchWave(state.wave) ? state.rules.waveSpacing * state.rules.launchWaveMultiplier : state.rules.waveSpacing;

        Events.fire(new WaveEvent());
    }

    private void checkGameState(){
        //campaign maps do not have a 'win' state!
        if(state.isCampaign()){
            //gameover only when cores are dead
            if(state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }

            //check if there are no enemy spawns
            if(state.rules.waves && spawner.countSpawns() + state.teams.cores(state.rules.waveTeam).size <= 0){
                //if yes, waves get disabled
                state.rules.waves = false;
            }

            //if there's a "win" wave and no enemies are present, win automatically
            if(state.rules.waves && state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning()){
                //the sector has been conquered - waves get disabled
                state.rules.waves = false;

                //fire capture event
                Events.fire(new SectorCaptureEvent(state.rules.sector));

                //save, just in case
                if(!headless){
                    control.saves.saveSector(state.rules.sector);
                }
            }
        }else{
            if(!state.rules.attackMode && state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }else if(state.rules.attackMode){
                Team alive = null;

                for(TeamData team : state.teams.getActive()){
                    if(team.hasCore()){
                        if(alive != null){
                            return;
                        }
                        alive = team.team;
                    }
                }

                if(alive != null && !state.gameOver){
                    Events.fire(new GameOverEvent(alive));
                    state.gameOver = true;
                }
            }
        }
    }

    private void updateWeather(){

        for(WeatherEntry entry : state.rules.weather){
            //update cooldown
            entry.cooldown -= Time.delta;

            //create new event when not active
            if(entry.cooldown < 0 && !entry.weather.isActive()){
                float duration = Mathf.random(entry.minDuration, entry.maxDuration);
                entry.cooldown = duration + Mathf.random(entry.minFrequency, entry.maxFrequency);
                Tmp.v1.setToRandomDirection();
                Call.createWeather(entry.weather, entry.intensity, duration, Tmp.v1.x, Tmp.v1.y);
            }
        }
    }

    @Remote(called = Loc.both)
    public static void launchZone(){
        if(!state.isCampaign()) return;

        if(!headless){
            ui.hudfrag.showLaunch();
        }

        //TODO better core launch effect
        for(Building tile : state.teams.playerCores()){
            Fx.launch.at(tile);
        }

        Sector sector = state.rules.sector;

        //TODO containers must be launched too
        Time.runTask(30f, () -> {
            Sector origin = sector.save.meta.secinfo.origin;
            if(origin != null){
                ItemSeq stacks = origin.getExtraItems();

                //add up all items into list
                for(Building entity : state.teams.playerCores()){
                    entity.items.each(stacks::add);
                }

                //save received items
                origin.setExtraItems(stacks);
            }

            //remove all the cores
            state.teams.playerCores().each(b -> b.tile.remove());

            state.launched = true;
            state.gameOver = true;

            //save over the data w/o the cores
            sector.save.save();

            //run a turn, since launching takes up a turn
            universe.runTurn();

            //TODO apply extra damage to sector
            //sector.setTurnsPassed(sector.getTurnsPassed() + 3);

            //TODO load the sector that was launched from
            Events.fire(new LaunchEvent());
            //manually fire game over event now
            Events.fire(new GameOverEvent(state.rules.defaultTeam));
        });
    }

    @Remote(called = Loc.both)
    public static void updateGameOver(Team winner){
        state.gameOver = true;
    }

    @Remote(called = Loc.both)
    public static void gameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        ui.restart.show(winner);
        netClient.setQuiet();
    }

    @Override
    public void dispose(){
        //save the settings before quitting
        Core.settings.manualSave();
    }

    @Override
    public void update(){
        Events.fire(Trigger.update);
        universe.updateGlobal();

        if(Core.settings.modified() && !state.isPlaying()){
            Core.settings.forceSave();
        }

        if(state.isGame()){
            if(!net.client()){
                state.enemies = Groups.unit.count(u -> u.team() == state.rules.waveTeam && u.type().isCounted);
            }

            //force pausing when the player is out of sector time
            if(state.isOutOfTime()){
                if(!state.wasTimeout){
                    universe.displayTimeEnd();
                    state.wasTimeout = true;
                }
                //if no turn was run.
                if(state.isOutOfTime()){
                    state.set(State.paused);
                }
            }

            if(!state.isPaused()){
                if(state.isCampaign()){
                    state.secinfo.update();
                }

                if(state.isCampaign()){
                    universe.update();
                }
                Time.update();

                //weather is serverside
                if(!net.client()){
                    updateWeather();

                    for(TeamData data : state.teams.getActive()){
                        if(data.hasAI()){
                            data.ai.update();
                        }
                    }
                }

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    if(!isWaitingWave()){
                        state.wavetime = Math.max(state.wavetime - Time.delta, 0);
                    }
                }

                if(!net.client() && state.wavetime <= 0 && state.rules.waves){
                    runWave();
                }

                //apply weather attributes
                state.envAttrs.clear();
                Groups.weather.each(w -> state.envAttrs.add(w.weather.attrs, w.opacity));

                Groups.update();
            }

            if(!net.client() && !world.isInvalidMap() && !state.isEditor() && state.rules.canGameOver){
                checkGameState();
            }
        }
    }

    /** @return whether the wave timer is paused due to enemies */
    public boolean isWaitingWave(){
        return (state.rules.waitEnemies || (state.wave >= state.rules.winWave && state.rules.winWave > 0)) && state.enemies > 0;
    }

}
