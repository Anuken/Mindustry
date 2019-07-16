package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Events;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.collection.ObjectSet.ObjectSetIterator;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.gen.BrokenBlock;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.BuildBlock;
import io.anuke.mindustry.world.blocks.BuildBlock.BuildEntity;
import io.anuke.mindustry.world.blocks.distribution.ItemEater;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

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
        Events.on(WaveEvent.class, event -> {
            if(world.isZone()){
                world.getZone().updateWave(state.wave);
            }
            if(!state.rules.resourcesWar){
                for(Player p : playerGroup.all()){
                    p.respawns = state.rules.respawns;
                }
            }
        });

        Events.on(RoundEvent.class, event -> {
            for (Player p : playerGroup.all()) {
                p.respawns = state.rules.respawns;
            }
        });

        Events.on(BlockDestroyEvent.class, event -> {
            //blocks that get broken are appended to the team's broken block queue
            Tile tile = event.tile;
            Block block = tile.block();
            if(block instanceof BuildBlock){
                BuildEntity entity = tile.entity();

                //update block to reflect the fact that something was being constructed
                if(entity.cblock != null && entity.cblock.synthetic()){
                    block = entity.cblock;
                }else{
                    //otherwise this was a deconstruction that was interrupted, don't want to rebuild that
                    return;
                }
            }

            TeamData data = state.teams.get(tile.getTeam());
            data.brokenBlocks.addFirst(BrokenBlock.get(tile.x, tile.y, tile.rotation(), block.id));
        });
    }

    /** Handles the event of content being used by either the player or some block. */
    public void handleContent(UnlockableContent content){
        if(!headless){
            data.unlockContent(content);
        }
    }

    public void play(){
        state.set(State.playing);
        state.wavetime = state.rules.waveSpacing * 2; //grace period of 2x wave time before game starts
        Events.fire(new PlayEvent());

        //add starting items
        if(!world.isZone()){
            for(Team team : Team.all){
                if(!state.teams.get(team).cores.isEmpty()){
                    TileEntity entity = state.teams.get(team).cores.first().entity;
                    entity.items.clear();
                    for(ItemStack stack : state.rules.loadout){
                        entity.items.add(stack.item, stack.amount);
                    }
                }
            }
        }
    }

    public void reset(){
        state.wave = 1;
        state.wavetime = state.rules.waveSpacing;
        state.gameOver = state.launched = false;
        state.teams = new Teams();
        state.rules = new Rules();
        state.stats = new Stats();
        state.eliminationtime = state.rules.eliminationTime;
        state.round = 1;
        state.pointsThreshold = state.rules.firstThreshold;
        state.buffTime = state.rules.buffSpacing;
        state.buffedItem = null;

        Time.clear();
        Entities.clear();
        TileEntity.sleepingEntities = 0;

        Events.fire(new ResetEvent());
    }

    public void runWave(){
        world.spawner.spawnEnemies();
        state.wave++;
        state.wavetime = world.isZone() && world.getZone().isBossWave(state.wave) ? state.rules.waveSpacing * state.rules.bossWaveMultiplier :
        world.isZone() && world.getZone().isLaunchWave(state.wave) ? state.rules.waveSpacing * state.rules.launchWaveMultiplier : state.rules.waveSpacing;

        Events.fire(new WaveEvent());
    }

    public void eliminateWeakest(){
        state.eliminationtime = state.rules.eliminationTime;
        state.round++;

        //TODO is this efficient?
        //filter only active teams
        Team[] activeTeams = Structs.filter(Team.class, Team.all, t -> state.teams.isActive(t));
        //sort in ascending order
        Arrays.sort(activeTeams, (s1, s2) -> state.points(s1) - state.points(s2));
        //if 2 firsts are equal there is an tie
        if(state.points(activeTeams[0]) == state.points(activeTeams[1])){
            //filter teams with tie
            Team[] tiedTeams = Structs.filter(Team.class, activeTeams, t -> state.points(activeTeams[0]) == state.points(t));
            //sort
            Arrays.sort(tiedTeams, (s1, s2) -> (int)(
                    state.teams.get(s1).cores.first().entity().items.sum((item, amount) -> itemsValues[item.id] * amount) -
                    state.teams.get(s2).cores.first().entity().items.sum((item, amount) -> itemsValues[item.id] * amount)
            ));

            Call.sendChatMessage("Tie breaker!");
            Call.eliminateTeam(tiedTeams[0].ordinal());
        }else{
            Call.eliminateTeam(activeTeams[0].ordinal());
        }

        Events.fire(new RoundEvent());
    }

    public void calculatePoints(){
        for(Team team : Team.all){
            int points = 0;
            if(state.teams.isActive(team)){
                for(Tile eater : state.teams.get(team).eaters){
                    points += (int)eater.<ItemEater.ItemEaterEntity>entity().pointsEarned;
                }
            }
            state.points[team.ordinal()] = points;
        }
    }

    @Remote(called = Loc.both)
    public static void eliminateTeam(int team){
        Team t = Team.all[team];
        //We need to copy set because when Core is destroyed it wants to remove itself from the original set and will occur error
        ObjectSet<Tile> cores = new ObjectSet<>(state.teams.get(t).cores);
        for(Tile tile : cores){
            world.tile(tile.pos()).block().onDestroyed(tile);
            world.removeBlock(tile);
        }
        for(Player p : playerGroup.all()){
            if(p.getTeam() == t){
                p.kill();
            }
        }
    }

    private void checkGameOver(){
        if(!state.rules.attackMode && state.teams.get(defaultTeam).cores.size == 0 && !state.gameOver){
            state.gameOver = true;
            Events.fire(new GameOverEvent(waveTeam));
        }else if(state.rules.attackMode){
            Team alive = null;

            for(Team team : Team.all){
                if(state.teams.get(team).cores.size > 0){
                    if(alive != null){
                        return;
                    }
                    alive = team;
                }
            }

            if(alive != null && !state.gameOver){
                if(world.isZone() && alive == defaultTeam){
                    //in attack maps, a victorious game over is equivalent to a launch
                    Call.launchZone();
                }else{
                    Events.fire(new GameOverEvent(alive));
                }
                state.gameOver = true;
            }
        }
    }

    @Remote(called = Loc.both)
    public static void launchZone(){
        if(!headless){
            ui.hudfrag.showLaunch();
        }

        for(Tile tile : new ObjectSetIterator<>(state.teams.get(defaultTeam).cores)){
            Effects.effect(Fx.launch, tile);
        }

        Time.runTask(30f, () -> {
            for(Tile tile : new ObjectSetIterator<>(state.teams.get(defaultTeam).cores)){
                for(Item item : content.items()){
                    data.addItem(item, tile.entity.items.get(item));
                }
                world.removeBlock(tile);
            }
            state.launched = true;
            state.gameOver = true;
            //manually fire game over event now
            Events.fire(new GameOverEvent(defaultTeam));
        });
    }

    @Remote(called = Loc.both)
    public static void onGameOver(Team winner){
        state.stats.wavesLasted = state.wave;
        ui.restart.show(winner);
        netClient.setQuiet();
    }

    @Override
    public void update(){

        if(!state.is(State.menu)){

            if(!state.isPaused()){
                Time.update();

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    if(!state.rules.waitForWaveToEnd || unitGroups[waveTeam.ordinal()].size() == 0){
                        state.wavetime = Math.max(state.wavetime - Time.delta(), 0);
                    }
                }

                if(state.rules.resourcesWar && !state.rules.rushGame && !state.gameOver && !netServer.isWaitingForPlayers()){
                    state.eliminationtime = Math.max(state.eliminationtime - Time.delta(), 0);
                }

                //resources war main loop
                if(state.rules.resourcesWar && !state.gameOver && !netServer.isWaitingForPlayers()){
                    if(!Net.client()){
                        calculatePoints();

                        //regular mode condition checking
                        if(state.eliminationtime <=0 && !state.rules.rushGame){
                            eliminateWeakest();
                        }

                        //rush mode condition checking
                        if(state.rules.rushGame)
                            for(int i=0; i<state.points.length; i++)
                                if(state.points[i] >= state.pointsThreshold)
                                    eliminateWeakest();
                    }

                    //buffing
                    if(state.rules.buffing){
                        state.buffTime = Math.max(state.buffTime - Time.delta(), 0);
                        if(state.buffTime <= 0){
                            if(state.buffedItem == null){
                                state.buffedItem = content.items().random();
                                state.buffTime = state.rules.buffTime;
                            }else{
                                state.buffedItem = null;
                                state.buffTime = state.rules.buffSpacing;
                            }
                        }
                    }

                }

                if(!Net.client() && state.wavetime <= 0 && state.rules.waves){
                    runWave();
                }

                if(!headless){
                    Entities.update(effectGroup);
                    Entities.update(groundEffectGroup);
                }

                if(!state.isEditor()){
                    for(EntityGroup group : unitGroups){
                        Entities.update(group);
                    }

                    Entities.update(puddleGroup);
                    Entities.update(shieldGroup);
                    Entities.update(bulletGroup);
                    Entities.update(tileGroup);
                    Entities.update(fireGroup);
                }else{
                    for(EntityGroup<?> group : unitGroups){
                        group.updateEvents();
                        collisions.updatePhysics(group);
                    }
                }


                Entities.update(playerGroup);

                //effect group only contains item transfers in the headless version, update it!
                if(headless){
                    Entities.update(effectGroup);
                }

                if(!state.isEditor()){

                    for(EntityGroup group : unitGroups){
                        if(group.isEmpty()) continue;
                        collisions.collideGroups(bulletGroup, group);
                    }

                    collisions.collideGroups(bulletGroup, playerGroup);
                }

                world.pathfinder.update();
            }

            if(!Net.client() && !world.isInvalidMap() && !state.isEditor()){
                checkGameOver();
            }
        }
    }
}
