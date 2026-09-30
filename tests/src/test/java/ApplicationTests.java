import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.game.markers.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.mod.*;
import mindustry.mod.Mods.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.io.*;
import java.nio.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.*;

public class ApplicationTests{
    static Map testMap;
    static boolean initialized;
    static final Fi testDataFolder = new Fi("../../tests/build/test_data");

    @BeforeAll
    public static void launchApplication(){
        launchApplication(true);
    }

    public static void launchApplication(boolean clear){
        //only gets called once
        if(initialized) return;
        initialized = true;

        try{
            boolean[] begins = {false};
            Throwable[] exceptionThrown = {null};
            Log.useColors = false;

            ApplicationCore core = new ApplicationCore(){
                @Override
                public void setup(){
                    //clear older data
                    if(clear){
                        ApplicationTests.testDataFolder.deleteDirectory();
                    }

                    Core.settings.setDataDirectory(testDataFolder);
                    headless = true;
                    net = new Net(null);
                    tree = new FileTree();
                    Vars.init();
                    content.createBaseContent();
                    mods.loadScripts();
                    content.createModContent();

                    add(logic = new Logic());
                    add(netServer = new NetServer());

                    content.init();

                    mods.eachClass(Mod::init);

                    if(mods.hasContentErrors()){
                        for(LoadedMod mod : mods.list()){
                            if(mod.hasContentErrors()){
                                for(Content cont : mod.erroredContent){
                                    throw new RuntimeException("error in file: " + cont.minfo.sourceFile.path(), cont.minfo.baseError);
                                }
                            }
                        }
                    }

                }

                @Override
                public void init(){
                    super.init();
                    begins[0] = true;
                    testMap = maps.loadInternalMap("serpulo/groundZero");
                    Thread.currentThread().interrupt();
                }
            };

            new HeadlessApplication(core, throwable -> exceptionThrown[0] = throwable);

            while(!begins[0]){
                if(exceptionThrown[0] != null){
                    fail(exceptionThrown[0]);
                }
                Thread.sleep(10);
            }

            Block block = content.getByName(ContentType.block, "build2");
            assertEquals("build2", block == null ? null : block.name, "2x2 construct block doesn't exist?");
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
    "a",
    "asd asd asd asd asdagagasasjakbgeah;jwrej 23424234",
    "这个服务器可以用自己的语言说话",
    "\uD83D\uDEA3"
    })
    void writeStringTest(String string){
        ByteBuffer buffer = ByteBuffer.allocate(500);
        TypeIO.writeString(buffer, string);
        buffer.position(0);
        assertEquals(TypeIO.readString(buffer), string);

        ByteArrayOutputStream ba = new ByteArrayOutputStream();

        TypeIO.writeString(new Writes(new DataOutputStream(ba)), string);
        assertEquals(TypeIO.readString(new Reads(new DataInputStream(new ByteArrayInputStream(ba.toByteArray())))), string);

        SendChatMessageCallPacket pack = new SendChatMessageCallPacket();
        pack.message = string;

        buffer.position(0);
        pack.write(new Writes(new ByteBufferOutput(buffer)));
        int len = buffer.position();
        buffer.position(0);
        pack.message = "INVALID";
        pack.read(new Reads(new ByteBufferInput(buffer)), len);
        pack.handled();

        assertEquals(string, pack.message);

        buffer.position(0);
        Writes writes = new Writes(new ByteBufferOutput(buffer));
        TypeIO.writeString(writes, string);

        buffer.position(0);

        assertEquals(string, TypeIO.readString(new Reads(new ByteBufferInput(buffer))));

        buffer.position(0);
        ConnectPacket con = new ConnectPacket();
        con.name = string;
        con.uuid = "AAAAAAAA";
        con.usid = "AAAAAAAA";
        con.mods = new Seq<>();
        con.write(new Writes(new ByteBufferOutput(buffer)));

        con.name = "INVALID";
        buffer.position(0);
        con.read(new Reads(new ByteBufferInput(buffer)));

        assertEquals(string, con.name);
    }

    @Test
    void writeRules(){
        ByteBuffer buffer = ByteBuffer.allocate(1000);

        Rules rules = new Rules();
        rules.attackMode = true;
        rules.buildSpeedMultiplier = 99f;

        TypeIO.writeRules(new Writes(new ByteBufferOutput(buffer)), rules);
        buffer.position(0);
        Rules res = TypeIO.readRules(new Reads(new ByteBufferInput(buffer)));

        assertEquals(rules.buildSpeedMultiplier, res.buildSpeedMultiplier);
        assertEquals(rules.attackMode, res.attackMode);
    }

    @Test
    void writeRules2(){
        Rules rules = new Rules();
        rules.attackMode = true;
        rules.tags.put("blah", "bleh");
        rules.buildSpeedMultiplier = 99.1f;

        String str = JsonIO.write(rules);
        Rules res = JsonIO.read(Rules.class, str);

        assertEquals(rules.buildSpeedMultiplier, res.buildSpeedMultiplier);
        assertEquals(rules.attackMode, res.attackMode);
        assertEquals(rules.tags.get("blah"), res.tags.get("blah"));

        String str2 = JsonIO.write(new Rules(){{
            attackMode = true;
        }});
    }

    @Test
    void initialization(){
        assertNotNull(logic);
        assertNotNull(state);
        assertTrue(content.getContentMap().length > 0);
    }

    @Test
    void playMap() throws Throwable{
        state.loadMap(testMap);
    }

    @Test
    void spawnWaves() throws Throwable{
        state.loadMap(testMap);
        assertTrue(state.spawner.countSpawns() > 0, "No spawns present.");
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
        World tiles = state.resizeWorld(8, 8);

        state.beginMapLoad();
        tiles.fill();
        state.endMapLoad();
    }

    @Test
    void multiblock(){
        createMap();
        int bx = 4;
        int by = 4;
        state.world.tile(bx, by).setBlock(Blocks.coreShard, Team.sharded, 0);
        assertEquals(state.world.tile(bx, by).team(), Team.sharded);
        for(int x = bx - 1; x <= bx + 1; x++){
            for(int y = by - 1; y <= by + 1; y++){
                assertEquals(state.world.tile(x, y).block(), Blocks.coreShard);
                assertEquals(state.world.tile(x, y).build, state.world.tile(bx, by).build);
            }
        }
    }

    @Test
    void blockInventories(){
        multiblock();
        Tile tile = state.world.tile(4, 4);
        tile.build.items.add(Items.coal, 5);
        tile.build.items.add(Items.titanium, 50);
        assertEquals(tile.build.items.total(), 55);
        tile.build.items.remove(Items.phaseFabric, 10);
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
    void save() throws Throwable{
        state.loadMap(testMap);
        assertTrue(state.teams.playerCores().size > 0);
        SaveIO.save(saveDirectory.child("0.msav"));
    }

    @Test
    void saveLoad() throws Throwable{
        state.loadMap(testMap);
        Map map = state.map;

        float hp = 30f;

        Unit unit = UnitTypes.dagger.spawn(Team.sharded, 20f, 30f);
        unit.health = hp;

        SaveIO.save(saveDirectory.child("0.msav"));
        resetWorld();
        SaveIO.load(saveDirectory.child("0.msav"));

        Unit spawned = Groups.unit.find(u -> u.type == UnitTypes.dagger);
        assertNotNull(spawned, "Saved daggers must persist");
        assertEquals(hp, spawned.health, "Spawned dagger health must save.");

        assertEquals(state.world.width, map.width);
        assertEquals(state.world.height, map.height);
        assertTrue(state.teams.playerCores().size > 0);
    }

    void updateBlocks(int times){
        for(Tile tile : state.world){
            if(tile.build != null && tile.isCenter()){
                tile.build.updateProximity();
            }
        }

        for(int i = 0; i < times; i++){
            Time.update();
            for(Tile tile : state.world){
                if(tile.build != null && tile.isCenter()){
                    tile.build.update();
                }
            }
        }
    }

    @Test
    void liquidOutput() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;

        state.world.tile(0, 0).setBlock(Blocks.liquidSource, Team.sharded);
        state.world.tile(0, 0).build.configureAny(Liquids.water);

        state.world.tile(2, 1).setBlock(Blocks.liquidTank, Team.sharded);

        updateBlocks(10);

        assertTrue(state.world.tile(2, 1).build.liquids.currentAmount() >= 1);
        assertTrue(state.world.tile(2, 1).build.liquids.current() == Liquids.water);
    }

    @Test
    void liquidJunctionOutput() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;

        Tile source = state.world.rawTile(0, 0), tank = state.world.rawTile(1, 4), junction = state.world.rawTile(0, 1), conduit = state.world.rawTile(0, 2);

        source.setBlock(Blocks.liquidSource, Team.sharded);
        source.build.configureAny(Liquids.water);

        junction.setBlock(Blocks.liquidJunction, Team.sharded);

        conduit.setBlock(Blocks.conduit, Team.sharded, 1);

        tank.setBlock(Blocks.liquidTank, Team.sharded);

        updateBlocks(10);

        assertTrue(tank.build.liquids.currentAmount() >= 1, "Liquid not moved through junction");
        assertTrue(tank.build.liquids.current() == Liquids.water, "Tank has no water");
    }

    @Test
    void liquidRouterOutputAll() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        Tile source = state.world.rawTile(4,0), router = state.world.rawTile(4, 2), conduitUp1 = state.world.rawTile(4,1),
        conduitLeft = state.world.rawTile(3,2), conduitUp2 = state.world.rawTile(4, 3), conduitRight = state.world.rawTile(5, 2),
        leftTank = state.world.rawTile(1, 2), topTank = state.world.rawTile(4,5), rightTank = state.world.rawTile(7, 2);

        source.setBlock(Blocks.liquidSource, Team.sharded);
        source.build.configureAny(Liquids.water);
        conduitUp1.setBlock(Blocks.conduit, Team.sharded, 1);
        router.setBlock(Blocks.liquidRouter, Team.sharded);
        conduitLeft.setBlock(Blocks.conduit, Team.sharded,2);
        conduitUp2.setBlock(Blocks.conduit, Team.sharded, 1);
        conduitRight.setBlock(Blocks.conduit, Team.sharded, 0);
        leftTank.setBlock(Blocks.liquidTank, Team.sharded);
        topTank.setBlock(Blocks.liquidTank, Team.sharded);
        rightTank.setBlock(Blocks.liquidTank, Team.sharded);

        updateBlocks(200);
        assertTrue(rightTank.build.liquids.currentAmount() > 0, "Liquid router did not distribute to rightTank");
        assertTrue(topTank.build.liquids.currentAmount() > 0, "Liquid router did not distribute to topTank");
        assertTrue(leftTank.build.liquids.currentAmount() > 0, "Liquid router did not distribute to rightTank");
    }

    @Test
    void sorterOutputCorrect() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        Tile source1 = state.world.rawTile(4, 0), source2 = state.world.rawTile(6, 0), s1conveyor = state.world.rawTile(4, 1),
        s2conveyor = state.world.rawTile(6, 1), s1s2conveyor = state.world.rawTile(5, 1), sorter = state.world.rawTile(5, 2),
        leftconveyor = state.world.rawTile(4, 2), rightconveyor = state.world.rawTile(6, 2), sortedconveyor = state.world.rawTile(5, 3),
        leftVault = state.world.rawTile(2, 2), rightVault = state.world.rawTile(8, 2), topVault = state.world.rawTile(5, 5);

        source1.setBlock(Blocks.itemSource, Team.sharded);
        source1.build.configureAny(Items.coal);
        source2.setBlock(Blocks.itemSource, Team.sharded);
        source2.build.configureAny(Items.copper);
        s1conveyor.setBlock(Blocks.conveyor, Team.sharded, 0);
        s2conveyor.setBlock(Blocks.conveyor, Team.sharded, 2);
        s1s2conveyor.setBlock(Blocks.conveyor, Team.sharded, 1);
        sorter.setBlock(Blocks.sorter, Team.sharded);
        sorter.build.configureAny(Items.copper);
        leftconveyor.setBlock(Blocks.conveyor, Team.sharded, 2);
        rightconveyor.setBlock(Blocks.conveyor, Team.sharded, 0);
        sortedconveyor.setBlock(Blocks.conveyor, Team.sharded, 1);
        leftVault.setBlock(Blocks.vault, Team.sharded);
        rightVault.setBlock(Blocks.vault, Team.sharded);
        topVault.setBlock(Blocks.vault, Team.sharded);

        updateBlocks(200);
        assertEquals(Items.coal, rightVault.build.items.first());
        assertEquals(Items.copper, topVault.build.items.first());
        assertEquals(Items.coal, leftVault.build.items.first());

    }

    @Test
    void routerOutputAll() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        Tile source1 = state.world.rawTile(5, 0),  conveyor = state.world.rawTile(5, 1),
        router = state.world.rawTile(5, 2), leftconveyor = state.world.rawTile(4, 2), rightconveyor = state.world.rawTile(6, 2),
        middleconveyor = state.world.rawTile(5, 3), leftVault = state.world.rawTile(2, 2),
        rightVault = state.world.rawTile(8, 2), topVault = state.world.rawTile(5, 5);

        source1.setBlock(Blocks.itemSource, Team.sharded);
        source1.build.configureAny(Items.coal);
        conveyor.setBlock(Blocks.conveyor, Team.sharded, 1);
        router.setBlock(Blocks.router, Team.sharded);
        router.build.configureAny(Items.coal);
        leftconveyor.setBlock(Blocks.conveyor, Team.sharded, 2);
        rightconveyor.setBlock(Blocks.conveyor, Team.sharded, 0);
        middleconveyor.setBlock(Blocks.conveyor, Team.sharded, 1);
        leftVault.setBlock(Blocks.vault, Team.sharded);
        rightVault.setBlock(Blocks.vault, Team.sharded);
        topVault.setBlock(Blocks.vault, Team.sharded);

        updateBlocks(200);
        assertEquals(Items.coal, rightVault.build.items.first());
        assertEquals(Items.coal, topVault.build.items.first());
        assertEquals(Items.coal, leftVault.build.items.first());
    }

    @Test
    void junctionOutputCorrect() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        Tile source1 = state.world.rawTile(5,0),source2 = state.world.rawTile(7, 2),  conveyor1 = state.world.rawTile(5, 1),
        conveyor2 = state.world.rawTile(6,2), junction = state.world.rawTile(5, 2), conveyor3 = state.world.rawTile(5,3),
        conveyor4 = state.world.rawTile(4,2), vault2 = state.world.rawTile(3, 1), vault1 = state.world.rawTile(5,5);
        source1.setBlock(Blocks.itemSource, Team.sharded);
        source1.build.configureAny(Items.coal);
        source2.setBlock(Blocks.itemSource, Team.sharded);
        source2.build.configureAny(Items.copper);
        conveyor1.setBlock(Blocks.conveyor, Team.sharded, 1);
        conveyor2.setBlock(Blocks.conveyor, Team.sharded, 2);
        conveyor3.setBlock(Blocks.conveyor, Team.sharded, 1);
        conveyor4.setBlock(Blocks.conveyor, Team.sharded, 2);
        junction.setBlock(Blocks.junction, Team.sharded);

        vault1.setBlock(Blocks.vault, Team.sharded);
        vault2.setBlock(Blocks.vault, Team.sharded);

        updateBlocks(200);
        assertEquals(Items.coal, vault1.build.items.first());
        assertEquals(Items.copper, vault2.build.items.first());
    }

    @Test
    void blockOverlapRemoved() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);

        //edge block
        state.world.tile(1, 1).setBlock(Blocks.coreShard);
        assertEquals(Blocks.coreShard, state.world.tile(0, 0).block());

        //this should overwrite the block
        state.world.tile(2, 2).setBlock(Blocks.coreShard);
        assertEquals(Blocks.air, state.world.tile(0, 0).block());
    }

    @Test
    void conveyorCrash() throws Throwable{
        state.loadMap(testMap);
        state.set(State.playing);

        state.world.tile(0, 0).setBlock(Blocks.conveyor);
        state.world.tile(0, 0).build.acceptStack(Items.copper, 1000, null);
    }

    @Test
    void conveyorBench() throws Throwable{
        int[] itemsa = {0};

        state.loadMap(testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        int length = 128;
        state.world.tile(0, 0).setBlock(Blocks.itemSource, Team.sharded);
        state.world.tile(0, 0).build.configureAny(Items.copper);

        Seq<Building> entities = Seq.with(state.world.tile(0, 0).build);

        for(int i = 0; i < length; i++){
            state.world.tile(i + 1, 0).setBlock(Blocks.conveyor, Team.sharded, 0);
            entities.add(state.world.tile(i + 1, 0).build);
        }

        state.world.tile(length + 1, 0).setBlock(new Block("___"){{
            hasItems = true;
            destructible = true;
            buildType = () -> new Building(){
                @Override
                public void handleItem(Building source, Item item){
                    itemsa[0] ++;
                }

                @Override
                public boolean acceptItem(Building source, Item item){
                    return true;
                }
            };
        }}, Team.sharded);

        entities.each(Building::updateProximity);

        final int iterations = 100_000;

        //warmup
        for(int i = 0; i < iterations; i++){
            entities.each(Building::update);
        }

        Time.mark();
        for(int i = 0; i < iterations*2; i++){
            entities.each(Building::update);
        }
        Log.info(Time.elapsed() + "ms to process " + itemsa[0] + " items");
        assertNotEquals(0, itemsa[0]);
    }

    @Test
    void load77Save() throws Throwable{
        resetWorld();
        SaveIO.load(Core.files.internal("77.msav"));

        //just tests if the map was loaded properly and didn't crash, no validity checks currently
        assertEquals(276, state.world.width);
        assertEquals(10, state.world.height);
    }

    @Test
    void load85Save() throws Throwable{
        resetWorld();
        SaveIO.load(Core.files.internal("85.msav"));

        assertEquals(250, state.world.width);
        assertEquals(300, state.world.height);
    }

    @Test
    void load108Save() throws Throwable{
        resetWorld();
        SaveIO.load(Core.files.internal("108.msav"));

        assertEquals(256, state.world.width);
        assertEquals(256, state.world.height);
    }

    @Test
    void load114Save() throws Throwable{
        resetWorld();
        SaveIO.load(Core.files.internal("114.msav"));

        assertEquals(500, state.world.width);
        assertEquals(500, state.world.height);
    }

    @Test
    void load152BESave() throws Throwable{
        resetWorld();
        SaveIO.load(Core.files.internal("152_be.msav"));

        assertEquals(414, state.world.width);
        assertEquals(414, state.world.height);
    }

    @Test
    void load152Save() throws Throwable{
        resetWorld();
        SaveIO.load(Core.files.internal("152.msav"));

        assertTrue(Groups.unit.contains(u -> u.type == UnitTypes.scepter));

        assertEquals(2000, state.world.width);
        assertEquals(195, state.world.height);
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

        Unit d1 = UnitTypes.poly.create(Team.sharded);
        Unit d2 = UnitTypes.poly.create(Team.sharded);

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

        assertEquals(Blocks.copperWallLarge, state.world.tile(0, 0).block());
        assertEquals(Blocks.air, state.world.tile(2, 2).block());
        assertEquals(Blocks.copperWallLarge, state.world.tile(1, 1).block());
        assertEquals(state.world.tile(1, 1).build, state.world.tile(0, 0).build);
    }

    @Test
    void buildingDestruction(){
        initBuilding();

        Builderc d1 = UnitTypes.poly.create(Team.sharded);
        Builderc d2 = UnitTypes.poly.create(Team.sharded);

        d1.set(10f, 20f);
        d2.set(10f, 20f);

        d1.addBuild(new BuildPlan(0, 0, 0, Blocks.copperWallLarge));
        d2.addBuild(new BuildPlan(1, 1));

        Time.setDeltaProvider(() -> 3f);
        d1.update();
        Time.setDeltaProvider(() -> 1f);
        d2.update();

        assertEquals(content.getByName(ContentType.block, "build2"), state.world.tile(0, 0).block());

        Time.setDeltaProvider(() -> 9999f);

        //prevents range issues
        state.rules.infiniteResources = true;

        d1.update();

        assertEquals(Blocks.copperWallLarge, state.world.tile(0, 0).block());
        assertEquals(Blocks.copperWallLarge, state.world.tile(1, 1).block());

        d2.clearBuilding();
        d2.addBuild(new BuildPlan(1, 1));

        for(int i = 0; i < 3; i++){
            d2.update();
        }

        assertEquals(Blocks.air, state.world.tile(0, 0).block());
        assertEquals(Blocks.air, state.world.tile(2, 2).block());
        assertEquals(Blocks.air, state.world.tile(1, 1).block());
    }

    @Test
    void allBlockTest() throws Throwable{
        World tiles = state.resizeWorld(80, 80);

        state.beginMapLoad();
        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                tiles.set(x, y, new Tile(x, y, Blocks.stone, Blocks.air, Blocks.air));
            }
        }
        int maxHeight = 0;
        state.rules.canGameOver = false;
        state.rules.borderDarkness = false;

        for(int x = 0, y = 0, i = 0; i < content.blocks().size; i ++){
            Block block = content.block(i);
            if(block.canBeBuilt()){
                int offset = Math.max(block.size % 2 == 0 ? block.size/2 - 1 : block.size/2, 0);

                if(x + block.size + 1 >= state.world.width){
                    y += maxHeight;
                    maxHeight = 0;
                    x = 0;
                }

                tiles.tile(x + offset, y + offset).setBlock(block);
                x += block.size;
                maxHeight = Math.max(maxHeight, block.size);
            }
        }
        state.endMapLoad();

        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                Tile tile = state.world.rawTile(x, y);
                if(tile.build != null){
                    try{
                        tile.build.update();
                    }catch(Throwable t){
                        fail("Failed to update block '" + tile.block() + "'.", t);
                    }
                    assertEquals(tile.block(), tile.build.block);
                    assertEquals(tile.block().health, tile.build.health());
                }
            }
        }
    }

    void checkPayloads(){
        for(int x = 0; x < state.world.width; x++){
            for(int y = 0; y < state.world.height; y++){
                Tile tile = state.world.rawTile(x, y);
                if(tile.build != null && tile.isCenter() && !(tile.block() instanceof CoreBlock)){
                    try{
                        tile.build.update();
                    }catch(Throwable t){
                        fail("Failed to update block in payload: '" + ((BuildPayload)tile.build.getPayload()).block() + "'", t);
                    }
                    assertEquals(tile.block(), tile.build.block);
                    assertEquals(tile.block().health, tile.build.health());
                }
            }
        }
    }

    @Test
    void allPayloadBlockTest() throws Throwable{
        int ts = 20;
        World tiles = state.resizeWorld(ts * 3, ts * 3);

        state.beginMapLoad();
        for(int x = 0; x < tiles.width; x++){
            for(int y = 0; y < tiles.height; y++){
                tiles.set(x, y, new Tile(x, y, Blocks.stone, Blocks.air, Blocks.air));
            }
        }

        tiles.getn(tiles.width - 2, tiles.height - 2).setBlock(Blocks.coreShard, Team.sharded);

        Seq<Block> blocks = content.blocks().select(b -> b.canBeBuilt());
        for(int i = 0; i < blocks.size; i++){
            int x = (i % ts) * 3 + 1;
            int y = (i / ts) * 3 + 1;
            Tile tile = tiles.tile(x, y);
            tile.setBlock(Blocks.payloadConveyor, Team.sharded);
            Building build = tile.build;
            build.handlePayload(build, new BuildPayload(blocks.get(i), Team.sharded));
        }
        state.endMapLoad();

        checkPayloads();

        SaveIO.write(saveDirectory.child("payloads.msav"));
        logic.reset();
        SaveIO.load(saveDirectory.child("payloads.msav"));

        checkPayloads();
    }

    @TestFactory
    DynamicTest[] testSectorValidity(){
        Seq<DynamicTest> out = new Seq<>();
        if(state.world == null) state.world = new World();

        for(SectorPreset sector : content.sectors()){

            out.add(dynamicTest(sector.name, () -> {
                Time.setDeltaProvider(() -> 1f);

                logic.reset();
                //pathfinder pollutes queue with garbage, causing OOM
                Reflect.<TaskQueue>get(HeadlessApplication.class, Core.app, "runnables").clear();
                state.rules.sector = sector.sector;
                state.loadGenerator(sector.generator.map.width, sector.generator.map.height, tiles -> sector.generator.generate(tiles, new WorldParams()));
                sector.rules.get(state.rules);
                ObjectSet<Item> resources = new ObjectSet<>();
                boolean hasSpawnPoint = false;

                assertFalse(state.rules.infiniteResources || Team.sharded.rules().infiniteResources, "Sector " + sector.name + " must not have infinite resources.");
                assertFalse(state.rules.allowEditRules, "Sector " + sector.name + " must not have rule editing enabled.");
                assertFalse(state.rules.allowEditWorldProcessors, "Sector " + sector.name + " must not have world processor editing enabled.");
                assertEquals(Team.sharded, state.rules.defaultTeam, "Sector " + sector.name + " must have the Sharded player team.");
                assertEquals(Vars.state.getPlanet() == Planets.serpulo ? Team.crux : Team.malis, state.rules.waveTeam, "Sector " + sector.name + " must have the correct enemy team.");

                Seq<TimerObjective> timers = state.rules.objectives.all.select(m -> m instanceof TimerObjective && !m.hidden && ((TimerObjective)m).text != null &&
                !((TimerObjective)m).text.isEmpty() && !((TimerObjective)m).text.contains("@")).as();

                if(!timers.isEmpty()){
                    fail("Sector " + sector.name + " has unlocalized objectives: " + timers.toString(", ", t -> "'" + t.text + "'"));
                }

                //TODO: some Erekir sectors (origin, caldera) modify the cap, why?
                //assertEquals(0, state.rules.unitCap, "Sector " + sector.name + " must not modify the unit cap.");

                for(Tile tile : state.world){
                    if(tile.drop() != null){
                        resources.add(tile.drop());
                    }
                    if(tile.block() instanceof CoreBlock && tile.team() == state.rules.defaultTeam){
                        hasSpawnPoint = true;
                    }
                }

                if(state.rules.waves){
                    Seq<SpawnGroup> spawns = state.rules.spawns;

                    int bossWave = 0;
                    if(state.rules.winWave > 0){
                        bossWave = state.rules.winWave;
                    }else{
                        outer:
                        for(int i = 1; i <= 1000; i++){
                            for(SpawnGroup spawn : spawns){
                                if(spawn.effect == StatusEffects.boss && spawn.getSpawned(i) > 0){
                                    bossWave = i;
                                    break outer;
                                }
                            }
                        }
                    }

                    if(state.rules.attackMode){
                        bossWave = 100;
                    }else{
                        assertNotEquals(0, bossWave, "Sector " + sector.name + " doesn't have a boss/end wave.");
                    }

                    if(state.rules.winWave > 0) bossWave = state.rules.winWave - 1;

                    for(int i = 1; i <= bossWave; i++){
                        int total = 0;
                        for(SpawnGroup spawn : spawns){
                            total += spawn.getSpawned(i - 1);
                        }

                        assertNotEquals(0, total, "Sector " + sector + " has no spawned enemies at wave " + i);
                    }
                }

                assertFalse(state.indexer.isBlockPresent(Blocks.powerSource), "Sector '" + sector + "' must not have power sources.");
                assertFalse(state.indexer.isBlockPresent(Blocks.powerVoid), "Sector '" + sector + "' must not have power voids.");
                assertFalse(state.indexer.isBlockPresent(Blocks.itemSource), "Sector '" + sector + "' must not have item sources.");
                assertFalse(state.indexer.isBlockPresent(Blocks.liquidSource), "Sector '" + sector + "' must not have liquid sources.");

                assertEquals(1, Team.sharded.cores().size, "Sector must have one core: " + sector + " (" + Team.sharded.cores() + ")");

                assertTrue(hasSpawnPoint, "Sector \"" + sector.name + "\" has no spawn points.");
                assertTrue(state.spawner.countSpawns() > 0 || (state.rules.attackMode && state.rules.waveTeam.data().hasCore()), "Sector \"" + sector.name + "\" has no enemy spawn points: " + state.spawner.countSpawns());
            }));
        }

        return out.toArray(DynamicTest.class);
    }

    void initBuilding(){
        createMap();

        Tile core = state.world.tile(5, 5);
        core.setBlock(Blocks.coreShard, Team.sharded, 0);
        for(Item item : content.items()){
            core.build.items.set(item, 3000);
        }

        assertEquals(core.build, Team.sharded.data().core());
    }

    void depositTest(Block block, Item item){
        Unit unit = UnitTypes.mono.create(Team.sharded);
        Tile tile = new Tile(0, 0, Blocks.air, Blocks.air, block);
        tile.setTeam(Team.sharded);
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
