package io.anuke.mindustry.ai;

import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.Squad;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.SpawnGroup;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner{
    private Array<SpawnGroup> groups;
    private Array<FlyerSpawn> flySpawns = new Array<>();
    private Array<GroundSpawn> groundSpawns = new Array<>();

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    public void spawnEnemies(){

        for(SpawnGroup group : groups){
            int spawned = group.getUnitsSpawned(state.wave);

            float spawnX, spawnY;
            float spread;

            if(group.type.isFlying){
                for(FlyerSpawn spawn : flySpawns){
                    Squad squad = new Squad();
                    float margin = 40f; //how far away from the edge flying units spawn
                    float trns = (world.width() + world.height()) * tilesize;
                    spawnX = Mathf.clamp(world.width() * tilesize / 2f + Angles.trnsx(spawn.angle, trns), -margin, world.width() * tilesize + margin);
                    spawnY = Mathf.clamp(world.height() * tilesize / 2f + Angles.trnsy(spawn.angle, trns), -margin, world.height() * tilesize + margin);
                    spread = margin / 1.5f;

                    for(int i = 0; i < spawned; i++){
                        BaseUnit unit = group.createUnit(waveTeam);
                        unit.setWave();
                        unit.setSquad(squad);
                        unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                        unit.add();
                    }
                }
            }else{
                for(GroundSpawn spawn : groundSpawns){
                    Squad squad = new Squad();
                    spawnX = spawn.x * tilesize;
                    spawnY = spawn.y * tilesize;
                    spread = tilesize;

                    for(int i = 0; i < spawned; i++){
                        BaseUnit unit = group.createUnit(waveTeam);
                        unit.setWave();
                        unit.setSquad(squad);
                        unit.set(spawnX + Mathf.range(spread), spawnY + Mathf.range(spread));
                        unit.add();
                    }
                }
            }
        }
    }

    private void reset(){
        flySpawns.clear();
        groundSpawns.clear();
        groups = state.rules.spawns;

        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                if(world.tile(x, y).block() == Blocks.spawn){
                    GroundSpawn spawn = new GroundSpawn();
                    spawn.x = x;
                    spawn.y = y;
                    groundSpawns.add(spawn);

                    FlyerSpawn fspawn = new FlyerSpawn();
                    fspawn.angle = Angles.angle(world.width()/2f, world.height()/2f, x, y);
                    flySpawns.add(fspawn);
                }
            }
        }
    }

    private class FlyerSpawn{
        float angle;
    }

    private class GroundSpawn{
        int x, y;
    }
}
