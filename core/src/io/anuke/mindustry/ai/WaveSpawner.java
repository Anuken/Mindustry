package io.anuke.mindustry.ai;

import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.IntArray;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.Tmp;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.Fx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.Effects;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.SpawnGroup;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Pos;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner{
    private static final float shockwaveBase = 380f, shockwaveRand = 0f, maxShockwaveDst = shockwaveBase + shockwaveRand;
    private Array<FlyerSpawn> flySpawns = new Array<>();
    private Array<GroundSpawn> groundSpawns = new Array<>();
    private IntArray loadedSpawns = new IntArray();
    private boolean spawning = false;

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    public void write(DataOutput stream) throws IOException{
        stream.writeInt(groundSpawns.size);
        for(GroundSpawn spawn : groundSpawns){
            stream.writeInt(Pos.get(spawn.x, spawn.y));
        }
    }

    public void read(DataInput stream) throws IOException{
        flySpawns.clear();
        groundSpawns.clear();
        loadedSpawns.clear();

        int amount = stream.readInt();

        for(int i = 0; i < amount; i++){
            loadedSpawns.add(stream.readInt());
        }
    }

    /**@return true if the player is near a ground spawn point.*/
    public boolean playerNear(){
        return groundSpawns.count(g -> Mathf.dst(g.x * tilesize, g.y * tilesize, player.x, player.y) < maxShockwaveDst) > 0;
    }

    public void spawnEnemies(){
        spawning = true;

        for(SpawnGroup group : state.rules.spawns){
            int spawned = group.getUnitsSpawned(state.wave);

            float spawnX, spawnY;
            float spread;

            if(group.type.isFlying){
                for(FlyerSpawn spawn : flySpawns){
                    float margin = 40f; //how far away from the edge flying units spawn
                    float trns = (world.width() + world.height()) * tilesize;
                    spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(spawn.angle, trns), -margin, world.width() * tilesize + margin);
                    spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(spawn.angle, trns), -margin, world.height() * tilesize + margin);
                    spread = margin / 1.5f;

                    for(int i = 0; i < spawned; i++){
                        BaseUnit unit = group.createUnit(waveTeam);
                        unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                        unit.add();
                    }
                }
            }else{
                for(GroundSpawn spawn : groundSpawns){
                    spawnX = spawn.x * tilesize;
                    spawnY = spawn.y * tilesize;
                    spread = tilesize*2;

                    for(int i = 0; i < spawned; i++){
                        Tmp.v1.rnd(spread);

                        BaseUnit unit = group.createUnit(waveTeam);
                        unit.set(spawnX + Tmp.v1.x, spawnY + Tmp.v1.y);

                        Time.run(Math.min(i*5, 60*2), () -> shockwave(unit));
                    }
                    Time.run(20f, () -> Effects.effect(Fx.spawnShockwave, spawn.x * tilesize, spawn.y * tilesize));
                    //would be interesting to see player structures survive this without hacks
                    Time.run(40f, () -> Damage.damage(waveTeam, spawn.x * tilesize, spawn.y * tilesize, shockwaveBase + Mathf.random(shockwaveRand), 99999999f, true));
                }
            }
        }

        Time.runTask(121f, () -> spawning = false);
    }

    public boolean isSpawning(){
        return spawning && !Net.client();
    }

    private void reset(){

        flySpawns.clear();
        groundSpawns.clear();

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                if(world.tile(x, y).block() == Blocks.spawn){
                    addSpawns(x, y);

                    //hide spawnpoints, they have served their purpose
                    world.tile(x, y).setBlock(Blocks.air);
                }
            }
        }

        for(int i = 0; i < loadedSpawns.size; i++){
            int pos = loadedSpawns.get(i);
            addSpawns(Pos.x(pos), Pos.y(pos));
        }

        loadedSpawns.clear();
    }

    private void addSpawns(int x, int y){
        GroundSpawn spawn = new GroundSpawn();
        spawn.x = x;
        spawn.y = y;
        groundSpawns.add(spawn);

        FlyerSpawn fspawn = new FlyerSpawn();
        fspawn.angle = Angles.angle(world.width()/2f, world.height()/2f, x, y);
        flySpawns.add(fspawn);
    }

    private void shockwave(BaseUnit unit){
        Effects.effect(Fx.unitSpawn, unit.x, unit.y, 0f, unit);
        Time.run(30f, () -> {
            unit.add();
            Effects.effect(Fx.spawn, unit);
        });
    }

    private class FlyerSpawn{
        float angle;
    }

    private class GroundSpawn{
        int x, y;
    }
}
