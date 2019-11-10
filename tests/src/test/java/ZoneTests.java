import io.anuke.arc.collection.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.io.SaveIO.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.storage.*;
import org.junit.jupiter.api.*;

import static io.anuke.mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class ZoneTests{

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
        Array<DynamicTest> out = new Array<>();
        if(world == null) world = new World();

        for(Zone zone : content.zones()){
            out.add(dynamicTest(zone.name, () -> {
                zone.generator.init(zone.loadout);
                logic.reset();
                try{
                    world.loadGenerator(zone.generator);
                }catch(SaveException e){
                    e.printStackTrace();
                    return;
                }
                zone.rules.get(state.rules);
                ObjectSet<Item> resources = new ObjectSet<>();
                boolean hasSpawnPoint = false;

                for(int x = 0; x < world.width(); x++){
                    for(int y = 0; y < world.height(); y++){
                        Tile tile = world.tile(x, y);
                        if(tile.drop() != null){
                            resources.add(tile.drop());
                        }
                        if(tile.block() instanceof CoreBlock && tile.getTeam() == defaultTeam){
                            hasSpawnPoint = true;
                        }
                    }
                }

                Array<SpawnGroup> spawns = state.rules.spawns;
                for(int i = 1; i <= 100; i++){
                    int total = 0;
                    for(SpawnGroup spawn : spawns){
                        total += spawn.getUnitsSpawned(i);
                    }

                    assertNotEquals(0, total, "Zone " + zone + " has no spawned enemies at wave " + i);
                }

                assertTrue(hasSpawnPoint, "Zone \"" + zone.name + "\" has no spawn points.");
                assertTrue(spawner.countSpawns() > 0 || (state.rules.attackMode && !state.teams.get(waveTeam).cores.isEmpty()), "Zone \"" + zone.name + "\" has no enemy spawn points: " + spawner.countSpawns());

                for(Item item : resources){
                    assertTrue(zone.resources.contains(item), "Zone \"" + zone.name + "\" is missing item in resource list: \"" + item.name + "\"");
                }

                for(Item item : zone.resources){
                    assertTrue(resources.contains(item), "Zone \"" + zone.name + "\" has unnecessary item in resource list: \"" + item.name + "\"");
                }
            }));
        }

        return out.toArray(DynamicTest.class);
    }
}
