import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.util.Structs;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import org.junit.jupiter.api.*;

import static io.anuke.mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    DynamicTest[] testZoneResources(){
        Array<DynamicTest> out = new Array<>();

        for(Zone zone : content.zones()){
            out.add(dynamicTest(zone.name, () -> {
                zone.generator.init(zone.loadout);
                logic.reset();
                world.loadGenerator(zone.generator);
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

                assertTrue(hasSpawnPoint, "Zone \"" + zone.name + "\" has no spawn points.");
                assertTrue(world.spawner.countSpawns() > 0 || (zone.rules.get().attackMode && !state.teams.get(waveTeam).cores.isEmpty()), "Zone \"" + zone.name + "\" has no enemy spawn points: " + world.spawner.countSpawns());

                for(Item item : resources){
                    assertTrue(Structs.contains(zone.resources, item), "Zone \"" + zone.name + "\" is missing item in resource list: \"" + item.name + "\"");
                }

                for(Item item : zone.resources){
                    assertTrue(resources.contains(item), "Zone \"" + zone.name + "\" has unnecessary item in resoruce list: \"" + item.name + "\"");
                }
            }));
        }

        return out.toArray(DynamicTest.class);
    }
}
