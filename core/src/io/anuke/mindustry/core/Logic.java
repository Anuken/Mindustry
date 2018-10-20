package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Teams;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityQuery;
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
        EntityQuery.init();
        EntityQuery.collisions().setCollider(tilesize, world::solid);
    }

    public void play(){
        state.set(State.playing);
        state.wavetime = wavespace * state.difficulty.timeScaling * 2;

        for(Tile tile : state.teams.get(defaultTeam).cores){
            if(world.getSector() != null){
                Array<ItemStack> items = world.getSector().startingItems;
                for(ItemStack stack : items){
                    tile.entity.items.add(stack.item, stack.amount);
                }
            }
        }

        Events.fire(new PlayEvent());
    }

    public void reset(){
        state.wave = 1;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.gameOver = false;
        state.teams = new Teams();

        Timers.clear();
        Entities.clear();
        TileEntity.sleepingEntities = 0;

        Events.fire(new ResetEvent());
    }

    public void runWave(){
        state.spawner.spawnEnemies();
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

    private void updateSectors(){
        if(world.getSector() == null) return;

        world.getSector().currentMission().update();

        //check unlocked sectors
        while(!world.getSector().complete && world.getSector().currentMission().isComplete()){
            world.getSector().currentMission().onComplete();
            world.getSector().completedMissions ++;

            state.mode = world.getSector().currentMission().getMode();
            world.getSector().currentMission().onBegin();
            world.sectors.save();
        }

        //check if all assigned missions are complete
        if(!world.getSector().complete && world.getSector().completedMissions >= world.getSector().missions.size){
            state.mode = GameMode.victory;

            world.sectors.completeSector(world.getSector().x, world.getSector().y);
            world.sectors.save();
            if(!headless){
                ui.missions.show(world.getSector());
            }

            Events.fire(new SectorCompleteEvent());
        }
    }

    @Override
    public void update(){
        if(threads.isEnabled() && !threads.isOnThread()) return;

        if(Vars.control != null){
            control.runUpdateLogic();
        }

        if(!state.is(State.menu)){

            if(!state.is(State.paused) || Net.active()){
                Timers.update();
            }

            updateSectors();

            if(!Net.client() && !world.isInvalidMap()){
                checkGameOver();
            }

            if(!state.is(State.paused) || Net.active()){

                if(!state.mode.disableWaveTimer && !state.mode.disableWaves){
                    state.wavetime -= Timers.delta();
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
                    EntityQuery.collideGroups(group, playerGroup);

                    for(EntityGroup other : unitGroups){
                        if(other.isEmpty()) continue;
                        EntityQuery.collideGroups(group, other);
                    }
                }

                EntityQuery.collideGroups(bulletGroup, playerGroup);
                EntityQuery.collideGroups(playerGroup, playerGroup);

                world.pathfinder.update();
            }
        }

        if(threads.isEnabled()){
            netServer.update();
        }
    }
}
