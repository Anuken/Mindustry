package io.anuke.mindustry.core;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.units.BaseUnit;
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

/**Logic module.
 * Handles all logic for entities and waves.
 * Handles game state events.
 * Does not store any game state itself.
 *
 * This class should <i>not</i> call any outside methods to change state of modules, but instead fire events.
 */
public class Logic extends Module {
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
        state.wavetime = wavespace * state.difficulty.timeScaling * 2;

        //fill inventory with items for debugging
        for(TeamData team : state.teams.getTeams()) {
            for (Tile tile : team.cores) {
                for (Item item : Item.all()) {
                    if (item.type == ItemType.material) {
                        tile.entity.items.addItem(item, 1000);
                    }
                }
            }
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

        //TODO spawn enemies properly
        for(int i = 0; i < 10; i ++){
            BaseUnit unit = UnitTypes.vtol.create(Team.red);
            Vector2 offset = new Vector2().setToRandomDirection().scl(world.width()/2f*tilesize).add(world.width()/2f*tilesize, world.height()/2f*tilesize);
            unit.inventory.addAmmo(AmmoTypes.bulletIron);
            unit.setWave();
            unit.set(offset.x, offset.y);
            unit.add();
        }

        state.wave ++;
        state.wavetime = wavespace * state.difficulty.timeScaling;
        state.extrawavetime = maxwavespace * state.difficulty.maxTimeScaling;

        Events.fire(WaveEvent.class);
    }

    @Override
    public void update(){
        if(!doUpdate) return;

        if(!state.is(State.menu)){

            if(control != null) control.triggerUpdateInput();

            if(!state.is(State.paused) || Net.active()){
                Timers.update();
            }

            /*
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
            }*/

            if(!state.is(State.paused) || Net.active()){

                if(!state.mode.disableWaveTimer){

                    if(state.enemies <= 0){
                        if(!world.getMap().name.equals("tutorial")) state.wavetime -= Timers.delta();
                    }else{
                        state.extrawavetime -= Timers.delta();
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
                Entities.update(fireGroup);
                Entities.update(shieldGroup);
                Entities.update(playerGroup);
                Entities.update(itemGroup);

                for(EntityGroup group : unitGroups){
                    if(!group.isEmpty()){
                        EntityPhysics.collideGroups(bulletGroup, group);

                        for(EntityGroup other : unitGroups){
                            if(!other.isEmpty()){
                                EntityPhysics.collideGroups(group, other);
                            }
                        }
                    }
                }

                EntityPhysics.collideGroups(bulletGroup, playerGroup);
                EntityPhysics.collideGroups(itemGroup, playerGroup);

                world.pathfinder().update();
            }
        }
    }
}
