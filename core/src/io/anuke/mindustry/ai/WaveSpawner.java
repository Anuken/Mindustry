package io.anuke.mindustry.ai;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.AmmoTypes;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.entities.units.Squad;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class WaveSpawner {
    private static final int quadsize = 15;

    private Array<FlyerSpawn> flySpawns = new Array<>();
    private Array<GroundSpawn> groundSpawns = new Array<>();

    public WaveSpawner(){
        Events.on(WorldLoadEvent.class, this::reset);
    }

    public void spawnEnemies(){
        int spawned = 10;
        int groundGroups = Math.min(1 + state.wave / 20, 4);
        int flyGroups = Math.min(1 + state.wave / 20, 4);

        //add extra groups if necessary
        for (int i = 0; i < groundGroups - groundSpawns.size; i++) {
            GroundSpawn spawn = new GroundSpawn();
        }

        for (int i = 0; i < flyGroups - flySpawns.size; i++) {
            FlyerSpawn spawn = new FlyerSpawn();
            spawn.angle = Mathf.random(360f);

            flySpawns.add(spawn);
        }

        for(GroundSpawn spawn : groundSpawns){

        }

        for(FlyerSpawn spawn : flySpawns){
            Squad squad = new Squad();
            float addition = 40f;
            float spread = addition / 1.5f;

            float baseX = world.width() *tilesize/2f + Mathf.sqrwavex(spawn.angle) * (world.width()/2f*tilesize + addition),
                    baseY = world.height() * tilesize/2f + Mathf.sqrwavey(spawn.angle) * (world.height()/2f*tilesize + addition);

            for(int i = 0; i < spawned; i ++){
                BaseUnit unit = UnitTypes.vtol.create(Team.red);
                unit.inventory.addAmmo(AmmoTypes.bulletIron);
                unit.setWave();
                unit.setSquad(squad);
                unit.set(baseX + Mathf.range(spread), baseY + Mathf.range(spread));
                unit.add();
            }
        }
    }

    public void calculateSpawn(){

        for(int x = 0; x < world.width(); x += quadsize){
            for(int y = 0; y < world.height(); y += quadsize){
                //TODO quadrant operations, etc
            }
        }
    }

    private void reset(){
        flySpawns.clear();
        groundSpawns.clear();
    }

    private class FlyerSpawn{
        float angle;

        FlyerSpawn(){

        }
    }

    private class GroundSpawn{

        GroundSpawn(){

        }
    }
}
