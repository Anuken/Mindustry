package io.anuke.mindustry.core;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.ApplicationListener;
import io.anuke.arc.Events;
import io.anuke.arc.entities.Entities;
import io.anuke.arc.entities.EntityGroup;
import io.anuke.arc.entities.EntityQuery;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Teams;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Recipe;
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
        Events.on(TileChangeEvent.class, event -> {
            if(event.tile.getTeam() == defaultTeam && Recipe.getByResult(event.tile.block()) != null){
                handleContent(Recipe.getByResult(event.tile.block()));
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
            control.unlocks.unlockContent(content);
        }
    }

    public void play(){
        state.set(State.playing);
        state.wavetime = wavespace * state.difficulty.timeScaling * 2;

        Events.fire(new PlayEvent());
    }

    public void reset(){
        state.wave = 1;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.gameOver = false;
        state.teams = new Teams();

        Time.clear();
        Entities.clear();
        TileEntity.sleepingEntities = 0;

        Events.fire(new ResetEvent());
    }

    public void runWave(){
        world.spawner.spawnEnemies();
        state.wave++;
        state.wavetime = wavespace * state.difficulty.timeScaling;

        Events.fire(new WaveEvent());
    }

    private void checkGameOver(){
        if(!state.mode.isPvp && state.teams.get(defaultTeam).cores.size == 0 && !state.gameOver){
            state.gameOver = true;
            Events.fire(new GameOverEvent(waveTeam));
        }else if(state.mode.isPvp){
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
    public static void onGameOver(Team winner){
        ui.restart.show(winner);
        netClient.setQuiet();
    }

    @Override
    public void update(){

        if(!state.is(State.menu)){

            if(!state.isPaused()){
                Time.update();

                if(!state.mode.disableWaveTimer && !state.mode.disableWaves && !state.gameOver){
                    state.wavetime -= Time.delta();
                }

                if(!Net.client() && state.wavetime <= 0 && !state.mode.disableWaves){
                    runWave();
                }

                if(!Entities.defaultGroup().isEmpty())
                    throw new RuntimeException("Do not add anything to the default group!");

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
