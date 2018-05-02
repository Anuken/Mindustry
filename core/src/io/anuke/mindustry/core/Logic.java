package io.anuke.mindustry.core;

import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.EventType.PlayEvent;
import io.anuke.mindustry.game.EventType.ResetEvent;
import io.anuke.mindustry.game.EventType.WaveEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.TeamInfo;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
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

    public Logic(){
        state = new GameState();
    }

    @Override
    public void init(){
        Entities.initPhysics();
        Entities.collisions().setCollider(tilesize, world::solid);
    }

    public void play(){
        state.wavetime = wavespace * state.difficulty.timeScaling * 2;

        if(state.mode.infiniteResources){
            state.inventory.fill();
        }else{
            state.inventory.clearItems();
        }

        Events.fire(PlayEvent.class);
    }

    public void reset(){
        state.wave = 1;
        state.extrawavetime = maxwavespace * state.difficulty.maxTimeScaling;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.enemies = 0;
        state.gameOver = false;
        state.teams = new TeamInfo();
        state.teams.add(Team.blue, true);
        state.teams.add(Team.red, false);

        Timers.clear();
        Entities.clear();
        TileEntity.sleepingEntities = 0;

        Events.fire(ResetEvent.class);
    }

    public void runWave(){

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

            boolean gameOver = true;

            for(TeamData data : state.teams.getTeams(true)){
                if(data.cores.size > 0){
                    gameOver = false;
                    break;
                }
            }

            if(gameOver && !state.gameOver){ //TODO better gameover state, victory state?
                state.gameOver = true;
                if(Net.server()) NetEvents.handleGameOver();
                Events.fire(GameOverEvent.class);
            }

            if(!state.is(State.paused) || Net.active()){

                if(!state.mode.disableWaveTimer){

                    if(state.enemies <= 0){
                        if(!world.getMap().name.equals("tutorial")) state.wavetime -= delta();
                    }else{
                        state.extrawavetime -= delta();
                    }
                }

                if(!Net.client() && (state.wavetime <= 0 || state.extrawavetime <= 0)){
                    runWave();
                }

                if(!Entities.defaultGroup().isEmpty()) throw new RuntimeException("Do not add anything to the default group!");

                Entities.update(bulletGroup);
                for(EntityGroup group : unitGroups){
                    Entities.update(group);
                }
                Entities.update(puddleGroup);
                Entities.update(tileGroup);
                Entities.update(airItemGroup);
                Entities.update(shieldGroup);
                Entities.update(playerGroup);

                for(EntityGroup group : unitGroups){
                    if(!group.isEmpty()){
                        Entities.collideGroups(bulletGroup, group);

                        for(EntityGroup other : unitGroups){
                            if(!other.isEmpty()){
                                Entities.collideGroups(group, other);
                            }
                        }
                    }
                }

                Entities.collideGroups(bulletGroup, playerGroup);
            }
        }
    }
}
