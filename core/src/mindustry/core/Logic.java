package mindustry.core;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.type.Weather.*;
import mindustry.world.*;

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

            tile.build.addPlan(true);
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
        Events.on(SaveLoadEvent.class, e -> {
            if(state.isCampaign()){
                state.secinfo.write();

                //how much wave time has passed
                int wavesPassed = state.secinfo.wavesPassed;

                //wave has passed, remove all enemies, they are assumed to be dead
                if(wavesPassed > 0){
                    Groups.unit.each(u -> {
                        if(u.team == state.rules.waveTeam){
                            u.remove();
                        }
                    });
                }

                //simulate passing of waves
                if(wavesPassed > 0){
                    //simulate wave counter moving forward
                    state.wave += wavesPassed;
                    state.wavetime = state.rules.waveSpacing;

                    SectorDamage.applyCalculatedDamage();
                }

                //reset values
                state.secinfo.damage = 0f;
                state.secinfo.wavesPassed = 0;
                state.secinfo.hasCore = true;
                state.secinfo.secondsPassed = 0;

                state.rules.sector.saveInfo();
            }
        });

        Events.on(WorldLoadEvent.class, e -> {
            //enable infinite ammo for wave team by default
            state.rules.waveTeam.rules().infiniteAmmo = true;

            //save settings
            Core.settings.manualSave();
        });

        //sync research
        Events.on(ResearchEvent.class, e -> {
            if(net.server()){
                Call.researched(e.content);
            }
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
        state.wavetime = 0;
    }

    public void runWave(){
        spawner.spawnEnemies();
        state.wave++;
        state.wavetime = state.rules.waveSpacing;

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
    public static void updateGameOver(Team winner){
        state.gameOver = true;
    }

    @Remote(called = Loc.both)
    public static void gameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        ui.restart.show(winner);
        netClient.setQuiet();
    }

    //called when the remote server researches something
    @Remote
    public static void researched(Content content){
        if(!(content instanceof UnlockableContent u)) return;

        state.rules.researched.add(u.name);
        ui.hudfrag.showUnlock(u);
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

            if(!state.isPaused()){
                state.teams.updateTeamStats();

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
