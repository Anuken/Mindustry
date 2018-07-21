package io.anuke.mindustry.core;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
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
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityPhysics;
import io.anuke.ucore.modules.Module;

import static io.anuke.mindustry.Vars.*;

/**
 * Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 * <p>
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic extends Module{
    public boolean doUpdate = true;

    public Logic(){
        state = new GameState();
    }

    @Override
    public void init(){
        EntityPhysics.initPhysics();
        EntityPhysics.collisions().setCollider(tilesize, world::solid);
    }

    public void play(){
        state.set(State.playing);
        state.wavetime = wavespace * state.difficulty.timeScaling * 2;

        //fill inventory with items for debugging

        for(TeamData team : state.teams.getTeams()){
            for(Tile tile : team.cores){
                if(debug){
                    for(Item item : Item.all()){
                        if(item.type == ItemType.material){
                            tile.entity.items.set(item, 1000);
                        }
                    }
                }else{
                    tile.entity.items.add(Items.tungsten, 50);
                    tile.entity.items.add(Items.lead, 20);
                }
            }
        }


        Events.fire(PlayEvent.class);
    }

    public void reset(){
        state.wave = 1;
        state.wavetime = wavespace * state.difficulty.timeScaling;
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
        state.spawner.spawnEnemies();
        state.wave++;
        state.wavetime = wavespace * state.difficulty.timeScaling;

        Events.fire(WaveEvent.class);
    }

    private void checkGameOver(){
        boolean gameOver = true;

        for(TeamData data : state.teams.getTeams(true)){
            if(data.cores.size > 0){
                gameOver = false;
                break;
            }
        }

        if(gameOver && !state.gameOver){
            state.gameOver = true;
            Events.fire(GameOverEvent.class);
        }
    }

    @Override
    public void update(){
        if(threads.isEnabled() && !threads.isOnThread()) return;

        if(Vars.control != null){
            control.runUpdateLogic();
        }

        if(!state.is(State.menu)){

            if(control != null) control.triggerUpdateInput();

            if(!state.is(State.paused) || Net.active()){
                Timers.update();
            }

            if(!world.isInvalidMap()){
                checkGameOver();
            }

            if(!state.is(State.paused) || Net.active()){

                if(!state.mode.disableWaveTimer){
                    state.wavetime -= Timers.delta();
                }

                if(!Net.client() && state.wavetime <= 0){
                    runWave();
                }

                if(!Entities.defaultGroup().isEmpty())
                    throw new RuntimeException("Do not add anything to the default group!");

                Entities.update(bulletGroup);
                for(EntityGroup group : unitGroups){
                    Entities.update(group);
                }
                Entities.update(puddleGroup);
                Entities.update(tileGroup);
                Entities.update(fireGroup);
                Entities.update(playerGroup);
                Entities.update(itemGroup);

                //effect group only contains item drops in the headless version, update it!
                if(headless){
                    Entities.update(effectGroup);
                }

                for(EntityGroup group : unitGroups){
                    if(!group.isEmpty()){
                        EntityPhysics.collideGroups(bulletGroup, group);
                    }
                }

                EntityPhysics.collideGroups(bulletGroup, playerGroup);
                EntityPhysics.collideGroups(itemGroup, playerGroup);

                world.pathfinder().update();
            }
        }
    }
}
