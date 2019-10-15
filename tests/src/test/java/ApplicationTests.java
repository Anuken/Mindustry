import io.anuke.arc.ApplicationCore;
import io.anuke.arc.Core;
import io.anuke.arc.backends.headless.HeadlessApplication;
import io.anuke.arc.collection.*;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.entities.traits.BuilderTrait.BuildRequest;
import io.anuke.mindustry.entities.type.BaseUnit;
import io.anuke.mindustry.entities.type.base.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.BlockPart;
import org.junit.jupiter.api.*;

import static io.anuke.mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ApplicationTests{
    static Map testMap;
    static boolean initialized;

    @BeforeAll
    static void launchApplication(){
        //only gets called once
        if(initialized) return;
        initialized = true;

        try{
            boolean[] begins = {false};
            Throwable[] exceptionThrown = {null};
            Log.setUseColors(false);

            ApplicationCore core = new ApplicationCore(){
                @Override
                public void setup(){
                    headless = true;
                    net = new Net(null);
                    tree = new FileTree();
                    Vars.init();
                    content.createContent();

                    add(logic = new Logic());
                    add(netServer = new NetServer());

                    content.init();
                }

                @Override
                public void init(){
                    super.init();
                    begins[0] = true;
                    testMap = maps.loadInternalMap("groundZero");
                }
            };

            new HeadlessApplication(core, null, throwable -> exceptionThrown[0] = throwable);

            while(!begins[0]){
                if(exceptionThrown[0] != null){
                    fail(exceptionThrown[0]);
                }
                Thread.sleep(10);
            }
        }catch(Throwable r){
            fail(r);
        }
    }

    @BeforeEach
    void resetWorld(){
        Time.setDeltaProvider(() -> 1f);
        logic.reset();
        state.set(State.menu);
    }

    @Test
    void initialization(){
        assertNotNull(logic);
        assertNotNull(world);
        assertTrue(content.getContentMap().length > 0);
    }

    @Test
    void playMap(){
        world.loadMap(testMap);
    }

    @Test
    void spawnWaves(){
        world.loadMap(testMap);
        assertTrue(spawner.countSpawns() > 0, "No spawns present.");
        logic.runWave();
        //force trigger delayed spawns
        Time.setDeltaProvider(() -> 1000f);
        Time.update();
        Time.update();
        Time.setDeltaProvider(() -> 1f);
        unitGroups[waveTeam.ordinal()].updateEvents();
        assertFalse(unitGroups[waveTeam.ordinal()].isEmpty(), "No enemies spawned.");
    }

    @Test
    void createMap(){
        Tile[][] tiles = world.createTiles(8, 8);

        world.beginMapLoad();
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                tiles[x][y] = new Tile(x, y);
            }
        }
        world.endMapLoad();
    }

    @Test
    void multiblock(){
        createMap();
        int bx = 4;
        int by = 4;
        world.setBlock(world.tile(bx, by), Blocks.coreShard, Team.sharded);
        assertEquals(world.tile(bx, by).getTeam(), Team.sharded);
        for(int x = bx - 1; x <= bx + 1; x++){
            for(int y = by - 1; y <= by + 1; y++){
                if(x == bx && by == y){
                    assertEquals(world.tile(x, y).block(), Blocks.coreShard);
                }else{
                    assertTrue(world.tile(x, y).block() instanceof BlockPart && world.tile(x, y).link() == world.tile(bx, by));
                }
            }
        }
    }

    @Test
    void blockInventories(){
        multiblock();
        Tile tile = world.tile(4, 4);
        tile.entity.items.add(Items.coal, 5);
        tile.entity.items.add(Items.titanium, 50);
        assertEquals(tile.entity.items.total(), 55);
        tile.entity.items.remove(Items.phasefabric, 10);
        tile.entity.items.remove(Items.titanium, 10);
        assertEquals(tile.entity.items.total(), 45);
    }

    @Test
    void timers(){
        boolean[] ran = {false};
        Time.run(1.9999f, () -> ran[0] = true);

        Time.update();
        assertFalse(ran[0]);
        Time.update();
        assertTrue(ran[0]);
    }

    @Test
    void manyTimers(){
        int runs = 100000;
        int[] total = {0};
        for(int i = 0; i < runs; i++){
            Time.run(0.999f, () -> total[0]++);
        }
        assertEquals(0, total[0]);
        Time.update();
        assertEquals(runs, total[0]);
    }

    @Test
    void longTimers(){
        Time.setDeltaProvider(() -> Float.MAX_VALUE);
        Time.update();
        int steps = 100;
        float delay = 100000f;
        Time.setDeltaProvider(() -> delay / steps + 0.01f);
        int runs = 100000;
        int[] total = {0};
        for(int i = 0; i < runs; i++){
            Time.run(delay, () -> total[0]++);
        }
        assertEquals(0, total[0]);
        for(int i = 0; i < steps; i++){
            Time.update();
        }
        assertEquals(runs, total[0]);
    }

    @Test
    void save(){
        world.loadMap(testMap);
        assertTrue(state.teams.get(defaultTeam).cores.size > 0);
        SaveIO.saveToSlot(0);
    }

    @Test
    void load(){
        world.loadMap(testMap);
        Map map = world.getMap();

        SaveIO.saveToSlot(0);
        resetWorld();
        SaveIO.loadFromSlot(0);

        assertEquals(world.width(), map.width);
        assertEquals(world.height(), map.height);
        assertTrue(state.teams.get(defaultTeam).cores.size > 0);
    }

    @Test
    void loadOldSave(){
        resetWorld();
        SaveIO.load(Core.files.internal("build77.msav"));

        //just tests if the map was loaded properly and didn't crash, no validity checks currently
        assertEquals(276, world.width());
        assertEquals(10, world.height());
    }

    @Test
    void arrayIterators(){
        Array<String> arr = Array.with("a", "b" , "c", "d", "e", "f");
        Array<String> results = new Array<>();

        for(String s : arr);
        for(String s : results);

        Array.iteratorsAllocated = 0;

        //simulate non-enhanced for loops, which should be correct

        for(int i = 0; i < arr.size; i++){
            for(int j = 0; j < arr.size; j++){
                results.add(arr.get(i) + arr.get(j));
            }
        }

        int index = 0;

        //test nested for loops
        for(String s : arr){
            for(String s2 : arr){
                assertEquals(results.get(index++), s + s2);
            }
        }

        assertEquals(results.size, index);
        assertEquals(0, Array.iteratorsAllocated, "No new iterators must have been allocated.");
    }

    @Test
    void inventoryDeposit(){
        depositTest(Blocks.surgeSmelter, Items.copper);
        depositTest(Blocks.vault, Items.copper);
        depositTest(Blocks.thoriumReactor, Items.thorium);
    }

    @Test
    void edges(){
        Point2[] edges = Edges.getEdges(1);
        assertEquals(edges[0], new Point2(1, 0));
        assertEquals(edges[1], new Point2(0, 1));
        assertEquals(edges[2], new Point2(-1, 0));
        assertEquals(edges[3], new Point2(0, -1));

        Point2[] edges2 = Edges.getEdges(2);
        assertEquals(8, edges2.length);
    }

    @Test
    void buildingOverlap(){
        initBuilding();

        Phantom d1 = (Phantom)UnitTypes.phantom.create(Team.sharded);
        Phantom d2 = (Phantom)UnitTypes.phantom.create(Team.sharded);

        d1.set(10f, 20f);
        d2.set(10f, 20f);

        d1.addBuildRequest(new BuildRequest(0, 0, 0, Blocks.copperWallLarge));
        d2.addBuildRequest(new BuildRequest(1, 1, 0, Blocks.copperWallLarge));

        Time.setDeltaProvider(() -> 9999999f);
        d1.updateBuilding();
        d2.updateBuilding();

        assertEquals(Blocks.copperWallLarge, world.tile(0, 0).block());
        assertEquals(Blocks.air, world.tile(2, 2).block());
        assertTrue(world.tile(1, 1).block() instanceof BlockPart);
    }

    @Test
    void buildingDestruction(){
        initBuilding();

        Phantom d1 = (Phantom)UnitTypes.phantom.create(Team.sharded);
        Phantom d2 = (Phantom)UnitTypes.phantom.create(Team.sharded);

        d1.set(10f, 20f);
        d2.set(10f, 20f);

        d1.addBuildRequest(new BuildRequest(0, 0, 0, Blocks.copperWallLarge));
        d2.addBuildRequest(new BuildRequest(1, 1));

        Time.setDeltaProvider(() -> 3f);
        d1.updateBuilding();
        Time.setDeltaProvider(() -> 1f);
        d2.updateBuilding();

        assertEquals(content.getByName(ContentType.block, "build2"), world.tile(0, 0).block());

        Time.setDeltaProvider(() -> 9999f);

        d1.updateBuilding();
        d2.updateBuilding();

        assertEquals(Blocks.air, world.tile(0, 0).block());
        assertEquals(Blocks.air, world.tile(2, 2).block());
        assertEquals(Blocks.air, world.tile(1, 1).block());
    }

    @Test
    void allBlockTest(){
        Tile[][] tiles = world.createTiles(256*2 + 20, 10);

        world.beginMapLoad();
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                tiles[x][y] = new Tile(x, y, Blocks.stone.id, (byte)0, (byte)0);
            }
        }
        int i = 0;

        for(int x = 5; x < tiles.length && i < content.blocks().size; ){
            Block block = content.block(i++);
            if(block.buildVisibility.get()){
                x += block.size;
                tiles[x][5].setBlock(block);
                x += block.size;
            }
        }
        world.endMapLoad();

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                Tile tile = world.tile(x, y);
                if(tile.entity != null){
                    try{
                        tile.entity.update();
                    }catch(Throwable t){
                        fail("Failed to update block '" + tile.block() + "'.", t);
                    }
                    assertEquals(tile.block(), tile.entity.block);
                    assertEquals(tile.block().health, tile.entity.health);
                }
            }
        }
    }

    void initBuilding(){
        createMap();

        Tile core = world.tile(5, 5);
        world.setBlock(core, Blocks.coreShard, Team.sharded);
        for(Item item : content.items()){
            core.entity.items.set(item, 3000);
        }

        assertEquals(core, state.teams.get(Team.sharded).cores.first());
    }

    void depositTest(Block block, Item item){
        BaseUnit unit = UnitTypes.spirit.create(Team.derelict);
        Tile tile = new Tile(0, 0, Blocks.air.id, (byte)0, block.id);
        int capacity = tile.block().itemCapacity;

        assertNotNull(tile.entity, "Tile should have an entity, but does not: " + tile);

        int deposited = tile.block().acceptStack(item, capacity - 1, tile, unit);
        assertEquals(capacity - 1, deposited);

        tile.block().handleStack(item, capacity - 1, tile, unit);
        assertEquals(tile.entity.items.get(item), capacity - 1);

        int overflow = tile.block().acceptStack(item, 10, tile, unit);
        assertEquals(1, overflow);

        tile.block().handleStack(item, 1, tile, unit);
        assertEquals(capacity, tile.entity.items.get(item));
    }
}