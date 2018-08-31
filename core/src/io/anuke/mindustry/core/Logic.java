package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.game.EventType.GameOverEvent;
import io.anuke.mindustry.game.EventType.PlayEvent;
import io.anuke.mindustry.game.EventType.ResetEvent;
import io.anuke.mindustry.game.EventType.WaveEvent;
import io.anuke.mindustry.game.Teams;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
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

        for(Tile tile : state.teams.get(defaultTeam).cores){
            if(debug){
                for(Item item : Item.all()){
                    if(item.type == ItemType.material){
                        tile.entity.items.set(item, 1000);
                    }
                }
            }

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

    //this never triggers in PvP; only for checking sector game-overs
    private void checkGameOver(){
        if(state.teams.get(defaultTeam).cores.size == 0 && !state.gameOver){
            state.gameOver = true;
            Events.fire(new GameOverEvent());
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


                for(EntityGroup group : unitGroups){
                    Entities.update(group);
                }
                Entities.update(puddleGroup);
                Entities.update(tileGroup);
                Entities.update(bulletGroup);
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
