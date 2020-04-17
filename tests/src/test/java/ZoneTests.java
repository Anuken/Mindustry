import arc.struct.*;
import arc.util.*;
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

        //TODO fix
        if(true) return new DynamicTest[0];
        //fail("Zone validity tests need to be refactored!");

        for(SectorPreset zone : content.zones()){
            out.add(dynamicTest(zone.name, () -> {
                logic.reset();
                try{
                    //world.loadGenerator(zone.generator);
                }catch(SaveException e){
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

                Array<SpawnGroup> spawns = state.rules.spawns;
                for(int i = 1; i <= 100; i++){
                    int total = 0;
                    for(SpawnGroup spawn : spawns){
                        total += spawn.getUnitsSpawned(i);
                    }

                    assertNotEquals(0, total, "Zone " + zone + " has no spawned enemies at wave " + i);
                }

                assertTrue(hasSpawnPoint, "Zone \"" + zone.name + "\" has no spawn points.");
                assertTrue(spawner.countSpawns() > 0 || (state.rules.attackMode && state.teams.get(state.rules.waveTeam).hasCore()), "Zone \"" + zone.name + "\" has no enemy spawn points: " + spawner.countSpawns());

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
