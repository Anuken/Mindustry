package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.EnemySpawn;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.EventType.PlayEvent;
import io.anuke.mindustry.game.EventType.ResetEvent;
import io.anuke.mindustry.game.EventType.WaveEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.WaveCreator;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;

import static io.anuke.mindustry.Vars.*;

/**Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 *
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic extends Module {
    private final Array<EnemySpawn> spawns = WaveCreator.getSpawns();

    @Override
    public void init(){
        Entities.initPhysics();
        Entities.collisions().setCollider(tilesize, world::solid);
    }

    public void play(){
        state.wavetime = wavespace * state.difficulty.timeScaling * 2;

        if(state.mode.infiniteResources){
            state.inventory.fill();
        }

        Events.fire(PlayEvent.class);
    }

    public void reset(){
        state.wave = 1;
        state.extrawavetime = maxwavespace * state.difficulty.maxTimeScaling;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.enemies = 0;
        state.lastUpdated = -1;
        state.gameOver = false;
        state.inventory.clearItems();
        state.allyTeams.clear();
        state.enemyTeams.clear();
        state.enemyTeams.add(Team.red);
        state.team = Team.none;

        Timers.clear();
        Entities.clear();

        Events.fire(ResetEvent.class);
    }

    public void runWave(){

        if(state.lastUpdated < state.wave + 1){
            world.pathfinder().resetPaths();
            state.lastUpdated = state.wave + 1;
        }

        //TODO spawn enemies

        state.wave ++;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.extrawavetime = maxwavespace * state.difficulty.maxTimeScaling;

        Events.fire(WaveEvent.class);
    }

    @Override
    public void update(){

        if(!state.is(State.menu)){

            if(!state.is(State.paused) || Net.active()){
                Timers.update();
            }

            if(!Net.client())
                world.pathfinder().update();

            if(world.getCore() != null && world.getCore().block() != ProductionBlocks.core && !state.gameOver){
                state.gameOver = true;
                if(Net.server()) NetEvents.handleGameOver();
                Events.fire(GameOverEvent.class);
            }

            if(!state.is(State.paused) || Net.active()){

                if(!state.mode.disableWaveTimer){

                    if(state.enemies <= 0){
                        if(!world.getMap().name.equals("tutorial")) state.wavetime -= delta();

                        if(state.lastUpdated < state.wave + 1 && state.wavetime < aheadPathfinding){ //start updating beforehand
                            world.pathfinder().resetPaths();
                            state.lastUpdated = state.wave + 1;
                        }
                    }else{
                        state.extrawavetime -= delta();
                    }
                }

                if(!Net.client() && (state.wavetime <= 0 || state.extrawavetime <= 0)){
                    runWave();
                }

                Entities.update(Entities.defaultGroup());
                Entities.update(bulletGroup);
                for(EntityGroup group : unitGroups){
                    if(!group.isEmpty()) Entities.update(group);
                }
                Entities.update(tileGroup);
                Entities.update(shieldGroup);
                Entities.update(playerGroup);

                for(EntityGroup group : unitGroups){
                    if(!group.isEmpty()) Entities.collideGroups(bulletGroup, group);
                }

                Entities.collideGroups(bulletGroup, playerGroup);
            }
        }
    }
}
