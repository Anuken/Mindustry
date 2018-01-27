package io.anuke.mindustry.core;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.Mathf;

import java.util.Arrays;

import static io.anuke.mindustry.Vars.*;

public class Logic extends Module {
    public final EntityGroup<Player> playerGroup = Entities.addGroup(Player.class).enableMapping();
    public final EntityGroup<Enemy> enemyGroup = Entities.addGroup(Enemy.class).enableMapping();
    public final EntityGroup<TileEntity> tileGroup = Entities.addGroup(TileEntity.class, false);
    public final EntityGroup<Bullet> bulletGroup = Entities.addGroup(Bullet.class);
    public final EntityGroup<Shield> shieldGroup = Entities.addGroup(Shield.class);

    public final int[] items = new int[Item.getAllItems().size];

    Array<EnemySpawn> spawns = WaveCreator.getSpawns();
    int wave = 1;
    int lastUpdated = -1;
    float wavetime;
    float extrawavetime;
    int enemies = 0;
    GameMode mode = GameMode.waves;
    Difficulty difficulty = Difficulty.normal;
    boolean friendlyFire;

    Tile core;
    Array<SpawnPoint> spawnpoints = new Array<>();

    @Override
    public void init(){
        Entities.initPhysics();
        Entities.collisions().setCollider(tilesize, world::solid);
    }

    public void play(){
        wavetime = wavespace * difficulty.timeScaling * 2;

        if(mode.infiniteResources){
            Arrays.fill(items, 999999999);
        }
    }

    public void reset(){
        lastUpdated = -1;
        wave = 1;
        extrawavetime = maxwavespace;
        wavetime = wavespace * difficulty.timeScaling;
        enemies = 0;
        Entities.clear();

        Arrays.fill(items, 0);
        spawnpoints.clear();

        for(Block block : Block.getAllBlocks()){
            block.onReset();
        }

        ui.hudfrag.updateItems();
        ui.hudfrag.updateWeapons();
    }

    public void runWave(){

        if(lastUpdated < wave + 1){
            world.pathfinder().resetPaths();
            lastUpdated = wave + 1;
        }

        for(EnemySpawn spawn : spawns){
            for(int lane = 0; lane < spawnpoints.size; lane ++){
                int fl = lane;
                Tile tile = spawnpoints.get(lane).start;
                int spawnamount = spawn.evaluate(wave, lane);

                for(int i = 0; i < spawnamount; i ++){
                    float range = 12f;

                    Timers.run(i*5f, () -> {

                        Enemy enemy = new Enemy(spawn.type);
                        enemy.set(tile.worldx() + Mathf.range(range), tile.worldy() + Mathf.range(range));
                        enemy.lane = fl;
                        enemy.tier = spawn.tier(wave, fl);
                        enemy.add();

                        Effects.effect(Fx.spawn, enemy);

                        enemies ++;
                    });
                }
            }
        }

        wave ++;

        wavetime = wavespace * difficulty.timeScaling;
        extrawavetime = maxwavespace;
    }

    public void coreDestroyed(){
        if(Net.active() && Net.server()) netServer.handleGameOver();
    }

    public void updateLogic(){
        if(!GameState.is(State.menu)){

            if(core.block() != ProductionBlocks.core && !ui.restart.isShown()){
                coreDestroyed();
            }

            if(!GameState.is(State.paused) || Net.active()){

                if(!mode.toggleWaves){

                    if(enemies <= 0){
                        wavetime -= delta();

                        if(lastUpdated < wave + 1 && wavetime < Vars.aheadPathfinding){ //start updating beforehand
                            world.pathfinder().resetPaths();
                            lastUpdated = wave + 1;
                        }
                    }else{
                        extrawavetime -= delta();
                    }
                }

                if(wavetime <= 0 || extrawavetime <= 0){
                    runWave();
                }

                Entities.update(Entities.defaultGroup());
                Entities.update(bulletGroup);
                Entities.update(enemyGroup);
                Entities.update(tileGroup);
                Entities.update(shieldGroup);
                Entities.update(playerGroup);

                Entities.collideGroups(enemyGroup, bulletGroup);
                Entities.collideGroups(playerGroup, bulletGroup);
            }
        }
    }
}
