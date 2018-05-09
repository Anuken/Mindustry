package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.game.EnemySpawn;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.EventType.PlayEvent;
import io.anuke.mindustry.game.EventType.ResetEvent;
import io.anuke.mindustry.game.EventType.WaveEvent;
import io.anuke.mindustry.game.SpawnPoint;
import io.anuke.mindustry.game.WaveCreator;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;

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

        Timers.clear();
        Entities.clear();

        Events.fire(ResetEvent.class);
    }

    public void runWave(){

        if(state.lastUpdated < state.wave + 1){
            world.pathfinder().resetPaths();
            state.lastUpdated = state.wave + 1;
        }

        for(EnemySpawn spawn : spawns){
            Array<SpawnPoint> spawns = world.getSpawns();

            for(int lane = 0; lane < spawns.size; lane ++){
                int fl = lane;
                Tile tile = spawns.get(lane).start;
                int spawnamount = spawn.evaluate(state.wave, lane);

                for(int i = 0; i < spawnamount; i ++){
                    float range = 12f;

                    Timers.runTask(i*5f, () -> {

                        Enemy enemy = new Enemy(spawn.type);
                        enemy.set(tile.worldx() + Mathf.range(range), tile.worldy() + Mathf.range(range));
                        enemy.lane = fl;
                        enemy.tier = spawn.tier(state.wave, fl);
                        enemy.add();

                        Effects.effect(Fx.spawn, enemy);

                        state.enemies ++;
                    });
                }
            }
        }

        state.wave ++;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.extrawavetime = maxwavespace * state.difficulty.maxTimeScaling;

        Events.fire(WaveEvent.class);
    }

    @Override
    public void update(){

        if(!state.is(State.menu)){

            if(control != null) control.triggerInputUpdate();

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
                        if(!world.getMap().name.equals("tutorial")) state.wavetime -= Timers.delta();

                        if(state.lastUpdated < state.wave + 1 && state.wavetime < aheadPathfinding){ //start updating beforehand
                            world.pathfinder().resetPaths();
                            state.lastUpdated = state.wave + 1;
                        }
                    }else if(!world.getMap().name.equals("tutorial")){
                        state.extrawavetime -= Timers.delta();
                    }
                }

                if(!Net.client() && (state.wavetime <= 0 || state.extrawavetime <= 0)){
                    runWave();
                }

                Entities.update(Entities.defaultGroup());
                Entities.update(bulletGroup);
                Entities.update(enemyGroup);
                Entities.update(tileGroup);
                Entities.update(shieldGroup);
                Entities.update(playerGroup);

                Entities.collideGroups(bulletGroup, enemyGroup);
                Entities.collideGroups(bulletGroup, playerGroup);
            }
        }
    }
}
