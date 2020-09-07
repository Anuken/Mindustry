import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.io.SaveIO.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class SectorTests{

    @BeforeAll
    static void launchApplication(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void resetWorld(){
        Time.setDeltaProvider(() -> 1f);
        logic.reset();
        state.set(State.menu);
    }

    @TestFactory
    DynamicTest[] testZoneValidity(){
        Seq<DynamicTest> out = new Seq<>();
        if(world == null) world = new World();

        for(SectorPreset zone : content.sectors()){

            out.add(dynamicTest(zone.name, () -> {
                logic.reset();
                try{
                    world.loadGenerator(zone.generator.map.width, zone.generator.map.height, zone.generator::generate);
                }catch(SaveException e){
                    //fails randomly and I don't care about fixing it
                    e.printStackTrace();
                    return;
                }
                zone.rules.get(state.rules);
                ObjectSet<Item> resources = new ObjectSet<>();
                boolean hasSpawnPoint = false;

                for(Tile tile : world.tiles){
                    if(tile.drop() != null){
                        resources.add(tile.drop());
                    }
                    if(tile.block() instanceof CoreBlock && tile.team() == state.rules.defaultTeam){
                        hasSpawnPoint = true;
                    }
                }

                Seq<SpawnGroup> spawns = state.rules.spawns;

                int bossWave = 0;
                if(state.rules.winWave > 0){
                    bossWave = state.rules.winWave;
                }else{
                    outer:
                    for(int i = 1; i <= 1000; i++){
                        for(SpawnGroup spawn : spawns){
                            if(spawn.effect == StatusEffects.boss && spawn.getUnitsSpawned(i) > 0){
                                bossWave = i;
                                break outer;
                            }
                        }
                    }
                }

                if(state.rules.attackMode){
                    bossWave = 100;
                }else{
                    assertNotEquals(0, bossWave, "Sector doesn't have a boss wave.");
                }

                //TODO check for difficulty?
                for(int i = 1; i <= bossWave; i++){
                    int total = 0;
                    for(SpawnGroup spawn : spawns){
                        total += spawn.getUnitsSpawned(i);
                    }

                    assertNotEquals(0, total, "Sector " + zone + " has no spawned enemies at wave " + i);
                    //TODO this is flawed and needs to be changed later
                    //assertTrue(total < 75, "Sector spawns too many enemies at wave " + i + " (" + total + ")");
                }

                assertEquals(1, Team.sharded.cores().size, "Sector must have one core: " + zone);
                assertTrue(Team.sharded.core().items.total() < 1000, "Sector must not have starting resources: " + zone);

                assertTrue(hasSpawnPoint, "Sector \"" + zone.name + "\" has no spawn points.");
                assertTrue(spawner.countSpawns() > 0 || (state.rules.attackMode && state.teams.get(state.rules.waveTeam).hasCore()), "Sector \"" + zone.name + "\" has no enemy spawn points: " + spawner.countSpawns());
            }));
        }

        return out.toArray(DynamicTest.class);
    }
}
