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
            //skip null entities or un-rebuildables, for obvious reasons
            if(tile.build == null || !tile.block().rebuildable) return;

            tile.build.addPlan(true);
        });

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                TeamData data = event.team.data();
                Iterator<BlockPlan> it = data.blocks.iterator();
                while(it.hasNext()){
                    BlockPlan b = it.next();
                    Block block = content.block(b.block);
                    if(event.tile.block().bounds(event.tile.x, event.tile.y, Tmp.r1).overlaps(block.bounds(b.x, b.y, Tmp.r2))){
                        b.removed = true;
                        it.remove();
                    }
                }
            }
        });

        //when loading a 'damaged' sector, propagate the damage
        Events.on(SaveLoadEvent.class, e -> {
            if(state.isCampaign()){
                state.rules.coreIncinerates = true;

                SectorInfo info = state.rules.sector.info;
                info.write();

                //how much wave time has passed
                int wavesPassed = info.wavesPassed;

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
                info.damage = 0f;
                info.wavesPassed = 0;
                info.hasCore = true;
                info.secondsPassed = 0;

                state.rules.sector.saveInfo();
            }
        });

        Events.on(WorldLoadEvent.class, e -> {
            //enable infinite ammo for wave team by default
            state.rules.waveTeam.rules().infiniteAmmo = true;

            if(state.isCampaign()){
                //enable building AI on campaign unless the preset disables it
                if(!(state.getSector().preset != null && !state.getSector().preset.useAI)){
                    state.rules.waveTeam.rules().ai = true;
                }
                state.rules.coreIncinerates = true;
                state.rules.waveTeam.rules().aiTier = state.getSector().threat * 0.8f;
                state.rules.waveTeam.rules().infiniteResources = true;

                //fill enemy cores by default.
                for(var core : state.rules.waveTeam.cores()){
                    for(Item item : content.items()){
                        core.items.set(item, core.block.itemCapacity);
                    }
                }
            }

            //save settings
            Core.settings.manualSave();
        });

        //sync research
        Events.on(UnlockEvent.class, e -> {
            if(net.server()){
                Call.researched(e.content);
            }
        });

        Events.on(SectorCaptureEvent.class, e -> {
            if(!net.client() && e.sector == state.getSector() && e.sector.isBeingPlayed()){
                state.rules.waveTeam.data().destroyToDerelict();
            }
        });

        //send out items to each client
        Events.on(TurnEvent.class, e -> {
            if(net.server() && state.isCampaign()){
                int[] out = new int[content.items().size];
                state.getSector().info.production.each((item, stat) -> {
                    out[item.id] = Math.max(0, (int)(stat.mean * turnDuration / 60));
                });

                Call.sectorProduced(out);
            }
        });

        //listen to core changes; if all cores have been destroyed, set to derelict.
        Events.on(CoreChangeEvent.class, e -> Core.app.post(() -> {
            if(state.rules.cleanupDeadTeams && state.rules.pvp && !e.core.isAdded() && e.core.team != Team.derelict && e.core.team.cores().isEmpty()){
                e.core.team.data().destroyToDerelict();
            }
        }));
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
                    CoreBuild entity = team.core();
                    entity.items.clear();
                    for(ItemStack stack : state.rules.loadout){
                        //make sure to cap storage
                        entity.items.add(stack.item, Math.min(stack.amount, entity.storageCapacity - entity.items.get(stack.item)));
                    }
                }
            }
        }

        //heal all cores on game start
        for(TeamData team : state.teams.getActive()){
            for(var entity : team.cores){
                entity.heal();
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
        runWave();
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
            if(state.rules.waves && (state.enemies == 0 && state.rules.winWave > 0 && state.wave >= state.rules.winWave && !spawner.isSpawning()) ||
                (state.rules.attackMode && state.rules.waveTeam.cores().isEmpty())){

                Call.sectorCapture();
            }
        }else{
            if(!state.rules.attackMode && state.teams.playerCores().size == 0 && !state.gameOver){
                state.gameOver = true;
                Events.fire(new GameOverEvent(state.rules.waveTeam));
            }else if(state.rules.attackMode){
                //count # of teams alive
                int countAlive = state.teams.getActive().count(t -> t.hasCore() && t.team != Team.derelict);

                if((countAlive <= 1 || (!state.rules.pvp && state.rules.defaultTeam.core() == null)) && !state.gameOver){
                    //find team that won
                    TeamData left = state.teams.getActive().find(t -> t.hasCore() && t.team != Team.derelict);
                    Events.fire(new GameOverEvent(left == null ? Team.derelict : left.team));
                    state.gameOver = true;
                }
            }
        }
    }

    private void updateWeather(){
        state.rules.weather.removeAll(w -> w.weather == null);

        for(WeatherEntry entry : state.rules.weather){
            //update cooldown
            entry.cooldown -= Time.delta;

            //create new event when not active
            if((entry.cooldown < 0 || entry.always) && !entry.weather.isActive()){
                float duration = entry.always ? Float.POSITIVE_INFINITY : Mathf.random(entry.minDuration, entry.maxDuration);
                entry.cooldown = duration + Mathf.random(entry.minFrequency, entry.maxFrequency);
                Tmp.v1.setToRandomDirection();
                Call.createWeather(entry.weather, entry.intensity, duration, Tmp.v1.x, Tmp.v1.y);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void sectorCapture(){
        //the sector has been conquered - waves get disabled
        state.rules.waves = false;

        if(state.rules.sector == null){
            //disable attack mode
            state.rules.attackMode = false;
            return;
        }

        state.rules.sector.info.wasCaptured = true;

        //fire capture event
        Events.fire(new SectorCaptureEvent(state.rules.sector));

        //disable attack mode
        state.rules.attackMode = false;

        //save, just in case
        if(!headless && !net.client()){
            control.saves.saveSector(state.rules.sector);
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

        var node = u.node();

        //unlock all direct dependencies on client, permanently
        while(node != null){
            node.content.unlock();
            node = node.parent;
        }

        state.rules.researched.add(u.name);
    }

    //called when the remote server runs a turn and produces something
    @Remote
    public static void sectorProduced(int[] amounts){
        if(!state.isCampaign()) return;
        Planet planet = state.rules.sector.planet;
        boolean any = false;

        for(Item item : content.items()){
            int am = amounts[item.id];
            if(am > 0){
                int sumMissing = planet.sectors.sum(s -> s.hasBase() ? s.info.storageCapacity - s.info.items.get(item) : 0);
                if(sumMissing == 0) continue;
                //how much % to add
                double percent = Math.min((double)am / sumMissing, 1);
                for(Sector sec : planet.sectors){
                    if(sec.hasBase()){
                        int added = (int)Math.ceil(((sec.info.storageCapacity - sec.info.items.get(item)) * percent));
                        sec.info.items.add(item, added);
                        any = true;
                    }
                }
            }
        }

        if(any){
            for(Sector sec : planet.sectors){
                sec.saveInfo();
            }
        }
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
                state.enemies = Groups.unit.count(u -> u.team() == state.rules.waveTeam && u.isCounted());
            }

            if(!state.isPaused()){
                state.teams.updateTeamStats();

                if(state.isCampaign()){
                    state.rules.sector.info.update();
                }

                if(state.isCampaign()){
                    universe.update();
                }
                Time.update();

                //weather is serverside
                if(!net.client() && !state.isEditor()){
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
                state.envAttrs.add(state.rules.attributes);
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
