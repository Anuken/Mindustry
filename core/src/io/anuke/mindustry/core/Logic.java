package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Events;
import io.anuke.arc.collection.ObjectSet.ObjectSetIterator;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.Entities;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.EntityQuery;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;

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
        });
    }

    @Override
    public void init(){
        EntityQuery.init();
        EntityQuery.collisions().setCollider(tilesize, (x, y) -> {
            Tile tile = world.tile(x, y);
            return tile != null && tile.solid();
        });
    }

    /**Handles the event of content being used by either the player or some block.*/
    public void handleContent(UnlockableContent content){
        if(!headless){
            data.unlockContent(content);
        }
    }

    public void play(){
        state.set(State.playing);
        state.wavetime = state.rules.waveSpacing * 2; //grace period of 2x wave time before game starts

        Events.fire(new PlayEvent());
    }

    public void reset(){
        state.wave = 1;
        state.wavetime = state.rules.waveSpacing;
        state.gameOver = state.launched = false;
        state.teams = new Teams();
        state.rules = new Rules();
        state.stats = new Stats();

        Time.clear();
        Entities.clear();
        TileEntity.sleepingEntities = 0;

        Events.fire(new ResetEvent());
    }

    public void runWave(){
        world.spawner.spawnEnemies();
        state.wave++;
        state.wavetime = world.isZone() && world.getZone().isBossWave(state.wave) ? state.rules.waveSpacing * bossWaveMultiplier :
                         world.isZone() && world.getZone().isLaunchWave(state.wave) ? state.rules.waveSpacing * launchWaveMultiplier : state.rules.waveSpacing;

        Events.fire(new WaveEvent());
    }

    private void checkGameOver(){
        if(!state.rules.pvp && state.teams.get(defaultTeam).cores.size == 0 && !state.gameOver){
            state.gameOver = true;
            Events.fire(new GameOverEvent(waveTeam));
        }else if(state.rules.pvp){
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
                state.gameOver = true;
                Events.fire(new GameOverEvent(alive));
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
                    data.addItem(item, tile.entity.items.get(item) / playerGroup.size());
                }
                world.removeBlock(tile);
            }
            state.launched = true;
        });
    }

    @Remote(called = Loc.both)
    public static void onGameOver(Team winner){
        ui.restart.show(winner);
        netClient.setQuiet();
    }

    @Override
    public void update(){

        if(!state.is(State.menu)){

            if(!state.isPaused()){
                Time.update();

                if(state.rules.waves && state.rules.waveTimer && !state.gameOver){
                    state.wavetime = Math.max(state.wavetime - Time.delta(), 0);
                }

                if(!Net.client() && state.wavetime <= 0 && state.rules.waves){
                    runWave();
                }

                if(!Entities.defaultGroup().isEmpty()){
                    throw new IllegalArgumentException("Do not add anything to the default group!");
                }

                if(!headless){
                    Entities.update(effectGroup);
                    Entities.update(groundEffectGroup);
                }

                for(EntityGroup group : unitGroups){
                    Entities.update(group);
                }

                Entities.update(puddleGroup);
                Entities.update(shieldGroup);
                Entities.update(bulletGroup);
                Entities.update(tileGroup);
                Entities.update(fireGroup);
                Entities.update(playerGroup);

                //effect group only contains item transfers in the headless version, update it!
                if(headless){
                    Entities.update(effectGroup);
                }

                for(EntityGroup group : unitGroups){
                    if(group.isEmpty()) continue;

                    EntityQuery.collideGroups(bulletGroup, group);
                }

                EntityQuery.collideGroups(bulletGroup, playerGroup);
                EntityQuery.collideGroups(playerGroup, playerGroup);

                world.pathfinder.update();
            }

            if(!Net.client() && !world.isInvalidMap()){
                checkGameOver();
            }
        }
    }
}
