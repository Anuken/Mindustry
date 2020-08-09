import arc.*;
import arc.backend.headless.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.net.Net;
import mindustry.type.*;
import mindustry.world.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
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
                    world = new World(){
                        @Override
                        public float getDarkness(int x, int y){
                            //for world borders
                            return 0;
                        }
                    };
                    content.createBaseContent();

                    add(logic = new Logic());
                    add(netServer = new NetServer());

                    content.init();
                }

                @Override
                public void init(){
                    super.init();
                    begins[0] = true;
                    testMap = maps.loadInternalMap("groundZero");
                    Thread.currentThread().interrupt();
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
        Groups.unit.update();
        assertFalse(Groups.unit.isEmpty(), "No enemies spawned.");
    }

    @Test
    void createMap(){
        Tiles tiles = world.resize(8, 8);

        world.beginMapLoad();
        tiles.fill();
        world.endMapLoad();
    }

    @Test
    void multiblock(){
        createMap();
        int bx = 4;
        int by = 4;
        world.tile(bx, by).setBlock(Blocks.coreShard, Team.sharded, 0);
        assertEquals(world.tile(bx, by).team(), Team.sharded);
        for(int x = bx - 1; x <= bx + 1; x++){
            for(int y = by - 1; y <= by + 1; y++){
                assertEquals(world.tile(x, y).block(), Blocks.coreShard);
                assertEquals(world.tile(x, y).build, world.tile(bx, by).build);
            }
        }
    }

    @Test
    void blockInventories(){
        multiblock();
        Tile tile = world.tile(4, 4);
        tile.build.items.add(Items.coal, 5);
        tile.build.items.add(Items.titanium, 50);
        assertEquals(tile.build.items.total(), 55);
        tile.build.items.remove(Items.phasefabric, 10);
        tile.build.items.remove(Items.titanium, 10);
        assertEquals(tile.build.items.total(), 45);
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
        assertTrue(state.teams.playerCores().size > 0);
        SaveIO.save(saveDirectory.child("0.msav"));
    }

    @Test
    void load(){
        world.loadMap(testMap);
        Map map = state.map;

        SaveIO.save(saveDirectory.child("0.msav"));
        resetWorld();
        SaveIO.load(saveDirectory.child("0.msav"));

        assertEquals(world.width(), map.width);
        assertEquals(world.height(), map.height);
        assertTrue(state.teams.playerCores().size > 0);
    }

    void updateBlocks(int times){
        for(Tile tile : world.tiles){
            if(tile.build != null && tile.isCenter()){
                tile.build.updateProximity();
            }
        }

        for(int i = 0; i < times; i++){
            Time.update();
            for(Tile tile : world.tiles){
                if(tile.build != null && tile.isCenter()){
                    tile.build.update();
                }
            }
        }
    }

    @Test
    void liquidOutput(){
        world.loadMap(testMap);
        state.set(State.playing);

        world.tile(0, 0).setBlock(Blocks.liquidSource);
        world.tile(0, 0).build.configureAny(Liquids.water);

        world.tile(2, 1).setBlock(Blocks.liquidTank);

        updateBlocks(10);

        assertTrue(world.tile(2, 1).build.liquids.currentAmount() >= 1);
        assertTrue(world.tile(2, 1).build.liquids.current() == Liquids.water);
    }

    @Test
    void liquidJunctionOutput(){
        world.loadMap(testMap);
        state.set(State.playing);

        Tile source = world.rawTile(0, 0), tank = world.rawTile(1, 4), junction = world.rawTile(0, 1), conduit = world.rawTile(0, 2);

        source.setBlock(Blocks.liquidSource);
        source.build.configureAny(Liquids.water);

        junction.setBlock(Blocks.liquidJunction);

        conduit.setBlock(Blocks.conduit, Team.derelict, 1);

        tank.setBlock(Blocks.liquidTank);

        updateBlocks(10);

        assertTrue(tank.build.liquids.currentAmount() >= 1, "Liquid not moved through junction");
        assertTrue(tank.build.liquids.current() == Liquids.water, "Tank has no water");
    }

    @Test
    void blockOverlapRemoved(){
        world.loadMap(testMap);
        state.set(State.playing);

        //edge block
        world.tile(1, 1).setBlock(Blocks.coreShard);
        assertEquals(Blocks.coreShard, world.tile(0, 0).block());

        //this should overwrite the block
        world.tile(2, 2).setBlock(Blocks.coreShard);
        assertEquals(Blocks.air, world.tile(0, 0).block());
    }

    @Test
    void conveyorCrash(){
        world.loadMap(testMap);
        state.set(State.playing);

        world.tile(0, 0).setBlock(Blocks.conveyor);
        world.tile(0, 0).build.acceptStack(Items.copper, 1000, null);
    }

    @Test
    void indexingBasic(){
        resetWorld();
        SaveIO.load(Core.files.internal("77.msav"));

        //test basic method.
        Rand r = new Rand(0);
        Building[] res = {null};

        Cons<Building> assigner = t -> res[0] = t;

        int iterations = 100;

        r.setSeed(0);

        //warmup.
        for(int i = 0; i < iterations; i++){
            int x = r.random(0, world.width()), y = r.random(0, world.height());
            float range = r.random(tilesize * 30);

            indexer.eachBlock(Team.sharded, x * tilesize, y * tilesize, range, t -> true, assigner);
        }

        //TODO impl
        /*
        r.setSeed(0);

        for(int i = 0; i < iterations; i++){
            int x = r.random(0, world.width()), y = r.random(0, world.height());
            float range = r.random(tilesize * 30);

            indexer.eachBlock2(Team.sharded, x * tilesize, y * tilesize, range, t -> true, assigner);
        }*/

        //benchmark.

        r.setSeed(0);

        Time.mark();

        for(int i = 0; i < iterations; i++){
            int x = r.random(0, world.width()), y = r.random(0, world.height());
            float range = r.random(tilesize * 30);

            indexer.eachBlock(Team.sharded, x * tilesize, y * tilesize, range, t -> true, assigner);
        }

        Log.info("Time for basic indexing: @", Time.elapsed());

        r.setSeed(0);

        /*
        Time.mark();

        for(int i = 0; i < iterations; i++){
            int x = r.random(0, world.width()), y = r.random(0, world.height());
            float range = r.random(tilesize * 30);

            indexer.eachBlock2(Team.sharded, x * tilesize, y * tilesize, range, t -> true, assigner);
        }

        Log.info("Time for quad: {0}", Time.elapsed());
        */

    }

    @Test
    void conveyorBench(){
        int[] itemsa = {0};

        world.loadMap(testMap);
        state.set(State.playing);
        int length = 128;
        world.tile(0, 0).setBlock(Blocks.itemSource);
        world.tile(0, 0).build.configureAny(Items.copper);

        Seq<Building> entities = Seq.with(world.tile(0, 0).build);

        for(int i = 0; i < length; i++){
            world.tile(i + 1, 0).setBlock(Blocks.conveyor, Team.derelict, 0);
            entities.add(world.tile(i + 1, 0).build);
        }

        world.tile(length + 1, 0).setBlock(new Block("___"){{
            hasItems = true;
            destructible = true;
            entityType = () -> new Building(){
                @Override
                public void handleItem(Building source, Item item){
                    itemsa[0] ++;
                }

                @Override
                public boolean acceptItem(Building source, Item item){
                    return true;
                }
            };
        }});

        entities.each(Building::updateProximity);

        //warmup
        for(int i = 0; i < 100000; i++){
            entities.each(Building::update);
        }

        Time.mark();
        for(int i = 0; i < 200000; i++){
            entities.each(Building::update);
        }
        Log.info(Time.elapsed() + "ms to process " + itemsa[0] + " items");
        assertNotEquals(0, itemsa[0]);
    }

    @Test
    void load77Save(){
        resetWorld();
        SaveIO.load(Core.files.internal("77.msav"));

        //just tests if the map was loaded properly and didn't crash, no validity checks currently
        assertEquals(276, world.width());
        assertEquals(10, world.height());
    }

    @Test
    void load85Save(){
        resetWorld();
        SaveIO.load(Core.files.internal("85.msav"));

        assertEquals(250, world.width());
        assertEquals(300, world.height());
    }

    @Test
    void arrayIterators(){
        Seq<String> arr = Seq.with("a", "b" , "c", "d", "e", "f");
        Seq<String> results = new Seq<>();

        for(String s : arr);
        for(String s : results);

        Seq.iteratorsAllocated = 0;

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
        assertEquals(0, Seq.iteratorsAllocated, "No new iterators must have been allocated.");
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

        Builderc d1 = (Builderc)UnitTypes.poly.create(Team.sharded);
        Builderc d2 = (Builderc)UnitTypes.poly.create(Team.sharded);

        //infinite build range
        state.rules.editor = true;
        state.rules.infiniteResources = true;
        state.rules.buildSpeedMultiplier = 999999f;

        d1.set(0f, 0f);
        d2.set(20f, 20f);

        d1.addBuild(new BuildPlan(0, 0, 0, Blocks.copperWallLarge));
        d2.addBuild(new BuildPlan(1, 1, 0, Blocks.copperWallLarge));

        d1.update();
        d2.update();

        assertEquals(Blocks.copperWallLarge, world.tile(0, 0).block());
        assertEquals(Blocks.air, world.tile(2, 2).block());
        assertEquals(Blocks.copperWallLarge, world.tile(1, 1).block());
        assertEquals(world.tile(1, 1).build, world.tile(0, 0).build);
    }

    @Test
    void buildingDestruction(){
        initBuilding();

        Builderc d1 = (Builderc)UnitTypes.poly.create(Team.sharded);
        Builderc d2 = (Builderc)UnitTypes.poly.create(Team.sharded);

        d1.set(10f, 20f);
        d2.set(10f, 20f);

        d1.addBuild(new BuildPlan(0, 0, 0, Blocks.copperWallLarge));
        d2.addBuild(new BuildPlan(1, 1));

        Time.setDeltaProvider(() -> 3f);
        d1.update();
        Time.setDeltaProvider(() -> 1f);
        d2.update();

        assertEquals(content.getByName(ContentType.block, "build2"), world.tile(0, 0).block());

        Time.setDeltaProvider(() -> 9999f);

        d1.update();

        assertEquals(Blocks.copperWallLarge, world.tile(0, 0).block());
        assertEquals(Blocks.copperWallLarge, world.tile(1, 1).block());

        d2.clearBuilding();
        d2.addBuild(new BuildPlan(1, 1));
        d2.update();

        assertEquals(Blocks.air, world.tile(0, 0).block());
        assertEquals(Blocks.air, world.tile(2, 2).block());
        assertEquals(Blocks.air, world.tile(1, 1).block());
    }

    @Test
    void allBlockTest(){
        Tiles tiles = world.resize(256*3 + 20, 10);

        world.beginMapLoad();
        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                tiles.set(x, y, new Tile(x, y, Blocks.stone, Blocks.air, Blocks.air));
            }
        }
        int i = 0;

        for(int x = 5; x < tiles.width && i < content.blocks().size; ){
            Block block = content.block(i++);
            if(block.canBeBuilt()){
                x += block.size;
                tiles.get(x, 5).setBlock(block);
                x += block.size;
            }
        }
        world.endMapLoad();

        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                Tile tile = world.rawTile(x, y);
                if(tile.build != null){
                    try{
                        tile.build.update();
                    }catch(Throwable t){
                        fail("Failed to update block '" + tile.block() + "'.", t);
                    }
                    assertEquals(tile.block(), tile.build.block());
                    assertEquals(tile.block().health, tile.build.health());
                }
            }
        }
    }

    void initBuilding(){
        createMap();

        Tile core = world.tile(5, 5);
        core.setBlock(Blocks.coreShard, Team.sharded, 0);
        for(Item item : content.items()){
            core.build.items.set(item, 3000);
        }

        assertEquals(core.build, state.teams.get(Team.sharded).core());
    }

    void depositTest(Block block, Item item){
        Unit unit = UnitTypes.mono.create(Team.derelict);
        Tile tile = new Tile(0, 0, Blocks.air, Blocks.air, block);
        int capacity = tile.block().itemCapacity;

        assertNotNull(tile.build, "Tile should have an entity, but does not: " + tile);

        int deposited = tile.build.acceptStack(item, capacity - 1, unit);
        assertEquals(capacity - 1, deposited);

        tile.build.handleStack(item, capacity - 1, unit);
        assertEquals(tile.build.items.get(item), capacity - 1);

        int overflow = tile.build.acceptStack(item, 10, unit);
        assertEquals(1, overflow);

        tile.build.handleStack(item, 1, unit);
        assertEquals(capacity, tile.build.items.get(item));
    }
}
