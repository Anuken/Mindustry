import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class HeatModuleTests{
    static final int cx = 10, cy = 10;
    static final float eps = 1e-3f;

    @BeforeAll
    public static void launchApplication(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void loadWorld() throws Throwable{
        logic.reset();
        state.set(State.menu);
        GameState.loadMap(ApplicationTests.testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        arc.util.Time.setDeltaProvider(() -> 1f);
        HeatBehaviorTests.clearArea();
    }

    static Building place(Block block, int x, int y, int rotation){
        return HeatBehaviorTests.place(block, x, y, rotation);
    }

    static void step(int ticks, Runnable beforeEach){
        HeatBehaviorTests.step(ticks, beforeEach);
    }

    static float heat(Building build){
        return HeatBehaviorTests.heat(build);
    }

    static byte[] save(Building build){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        build.writeAll(new Writes(new DataOutputStream(out)));
        return out.toByteArray();
    }

    static void load(Building build, byte[] data){
        build.readAll(new Reads(new DataInputStream(new ByteArrayInputStream(data))), build.version());
    }

    static GenericCrafter customCrafter(String name, int size){
        GenericCrafter block = new GenericCrafter(name + System.nanoTime());
        block.size = size;
        return block;
    }

    static GenericCrafter init(GenericCrafter block){
        block.init();
        return block;
    }

    static Building fixedSource(Building target, float heat){
        GenericCrafter block = customCrafter("fixed-heat", 1);
        block.heatOutput = heat;
        block.heatWarmupRate = 1000f;
        block.rotate = true;
        block.itemCapacity = 0;
        init(block);
        return place(block, target.tile.x + target.block.size / 2 + 1, target.tile.y, 2);
    }

    static VariableReactor.VariableReactorBuild fluxReactor(float heat){
        Building reactor = place(Blocks.fluxReactor, cx, cy, 0);
        HeatBehaviorTests.builds.clear();
        if(heat > 0f) fixedSource(reactor, heat);
        HeatBehaviorTests.builds.add(reactor);
        return (VariableReactor.VariableReactorBuild)reactor;
    }

    static void runFlux(VariableReactor.VariableReactorBuild reactor, float coolant, int ticks){
        step(ticks, () -> reactor.liquids.set(Liquids.cyanogen, coolant));
    }

    //region module presence

    @Test
    void heatBlocksHaveModules(){
        Block[] heatBlocks = {Blocks.heatSource, Blocks.electricHeater, Blocks.neoplasiaReactor, Blocks.thoriumReactor, Blocks.carbideCrucible, Blocks.afflict, Blocks.fluxReactor, Blocks.heatRedirector, Blocks.heatRouter};
        Block[] otherBlocks = {Blocks.kiln, Blocks.graphitePress, Blocks.arc, Blocks.duo};

        for(Block block : heatBlocks){
            assertTrue(block.hasHeat, block.name);
            assertNotNull(place(block, cx, cy, 0).heatModule, block.name);
            HeatBehaviorTests.clearArea();
        }

        for(Block block : otherBlocks){
            assertFalse(block.hasHeat, block.name);
            assertNull(place(block, cx, cy, 0).heatModule, block.name);
            HeatBehaviorTests.clearArea();
        }
    }

    @Test
    void emittersAndConsumersSetFlags(){
        assertTrue(Blocks.electricHeater.heatOutput > 0f);
        assertNull(Blocks.electricHeater.consHeat);
        assertNotNull(Blocks.carbideCrucible.consHeat);
        assertEquals(40f, Blocks.carbideCrucible.consHeat.requirement, eps);
        assertEquals(0f, Blocks.carbideCrucible.heatOutput, eps);

        GenericCrafter consumer = customCrafter("heat-consumer", 2);
        consumer.consumeHeat(5f);
        init(consumer);
        assertTrue(consumer.hasHeat);

        GenericCrafter emitter = customCrafter("heat-emitter", 2);
        emitter.heatOutput = 4f;
        init(emitter);
        assertTrue(emitter.hasHeat);
    }

    @Test
    void moduleBitIsSetOnlyForHeatBlocks(){
        int bit = 1 << 6;
        assertEquals(bit, place(Blocks.electricHeater, cx, cy, 0).moduleBitmask() & bit);
        assertEquals(bit, place(Blocks.carbideCrucible, cx + 5, cy, 0).moduleBitmask() & bit);
        assertEquals(bit, place(Blocks.heatRedirector, cx, cy + 5, 0).moduleBitmask() & bit);
        assertEquals(0, place(Blocks.kiln, cx + 5, cy + 5, 0).moduleBitmask() & bit);
    }

    //endregion
    //region saving

    @Test
    void moduleWritesOnlyOutput(){
        HeatModule module = new HeatModule();
        module.output = 7f;
        module.input = 3f;
        module.sides[1] = 2f;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        module.write(new Writes(new DataOutputStream(out)));
        assertEquals(4, out.size());

        HeatModule read = new HeatModule();
        read.read(new Reads(new DataInputStream(new ByteArrayInputStream(out.toByteArray()))));
        assertEquals(7f, read.output, eps);
        assertEquals(0f, read.input, eps);
        assertEquals(0f, read.sides[1], eps);
    }

    @Test
    void heaterOutputSurvivesSaveRoundTrip(){
        Building heater = place(Blocks.electricHeater, cx, cy, 0);
        step(40, () -> HeatBehaviorTests.supply(heater));
        assertEquals(3f, heater.heatModule.output, eps);

        byte[] data = save(heater);

        Building fresh = place(Blocks.electricHeater, cx + 6, cy, 0);
        assertEquals(0f, fresh.heatModule.output, eps);
        load(fresh, data);
        assertEquals(3f, fresh.heatModule.output, eps);
    }

    @Test
    void conductorOutputSurvivesSaveRoundTrip(){
        Building src = place(Blocks.heatSource, cx + 2, cy, 2);
        Building cond = place(Blocks.smallHeatRedirector, cx, cy, 2);
        step(5, null);
        assertEquals(1000f, cond.heatModule.output, eps);

        byte[] data = save(cond);

        Building fresh = place(Blocks.smallHeatRedirector, cx + 8, cy + 8, 2);
        load(fresh, data);
        assertEquals(1000f, fresh.heatModule.output, eps);
    }

    @Test
    void inputAndSidesAreRecomputedAfterLoading(){
        Building src = place(Blocks.heatSource, cx + 2, cy, 2);
        Building consumer = place(Blocks.carbideCrucible, cx, cy - 1, 0);
        step(3, null);
        assertEquals(1000f, consumer.heatModule.input, eps);
        assertEquals(1000f, consumer.heatModule.sides[0], eps);

        byte[] data = save(consumer);
        consumer.heatModule.input = 0f;
        java.util.Arrays.fill(consumer.heatModule.sides, 0f);
        load(consumer, data);
        assertEquals(0f, consumer.heatModule.input, eps);
        assertEquals(0f, consumer.heatModule.sides[0], eps);

        step(1, null);
        assertEquals(1000f, consumer.heatModule.input, eps);
        assertEquals(1000f, consumer.heatModule.sides[0], eps);
    }

    //endregion
    //region custom blocks

    @Test
    void heatTransformerOutputFollowsInputEfficiency(){
        GenericCrafter transformer = customCrafter("heat-transformer", 2);
        transformer.heatOutput = 6f;
        transformer.rotate = true;
        transformer.consumeHeat(12f).maxEfficiency(1f);
        init(transformer);

        Building target = place(transformer, cx, cy, 0);
        HeatBehaviorTests.builds.clear();
        Building src = HeatBehaviorTests.eastSource(target, false);
        HeatBehaviorTests.builds.add(target);

        step(60, null);
        assertEquals(1000f, target.heatModule.input, eps);
        assertEquals(1f, target.efficiency, eps);
        assertEquals(6f, target.heatModule.output, eps);
        assertEquals(1000f, heat(target), eps, "a consumer senses its input");

        src.enabled = false;
        step(60, null);
        assertEquals(0f, target.heatModule.input, eps);
        assertEquals(0f, target.heatModule.output, eps);
    }

    @Test
    void transformerFeedsDownstreamConsumer(){
        GenericCrafter transformer = customCrafter("heat-transformer", 2);
        transformer.heatOutput = 6f;
        transformer.rotate = true;
        transformer.consumeHeat(12f).maxEfficiency(1f);
        init(transformer);

        Building target = place(transformer, cx, cy, 2);
        HeatBehaviorTests.builds.clear();
        HeatBehaviorTests.eastSource(target, false);
        HeatBehaviorTests.builds.add(target);

        //transformer faces west, so the consumer sits on its west edge
        Building consumer = place(Blocks.carbideCrucible, cx - 2, cy, 0);

        step(60, null);
        assertEquals(6f, target.heatModule.output, eps);
        assertEquals(6f, consumer.heatModule.input, eps);
    }

    @Test
    void overheatScalesEfficiencyUpToMaximum(){
        GenericCrafter crafter = customCrafter("overheat-crafter", 2);
        crafter.consumeHeat(10f).overheatScale(0.5f).maxEfficiency(2f);
        init(crafter);

        Building target = place(crafter, cx, cy, 0);
        HeatBehaviorTests.builds.clear();
        HeatBehaviorTests.eastSource(target, true);
        HeatBehaviorTests.builds.add(target);

        float[][] result = HeatBehaviorTests.ramp(target, 50f);
        float[] heats = result[0], efficiencies = result[1];

        for(int i = 0; i < heats.length; i++){
            float expected = Math.min(Math.min(heats[i] / 10f, 1f) + Math.max(heats[i] - 10f, 0f) / 10f * 0.5f, 2f);
            assertEquals(expected, efficiencies[i], 0.02f, "heat " + heats[i]);
        }

        assertEquals(1.5f, efficiencies[HeatBehaviorTests.closest(heats, 20f)], 0.01f);
        assertEquals(2f, efficiencies[HeatBehaviorTests.closest(heats, 45f)], 0.01f);
    }

    @Test
    void heatRequirementIsBypassedWhenCheating(){
        GenericCrafter crafter = customCrafter("cheat-crafter", 2);
        crafter.consumeHeat(10f).maxEfficiency(1f);
        init(crafter);

        Building target = place(crafter, cx, cy, 0);
        step(2, null);
        assertEquals(0f, target.efficiency, eps);

        Team.sharded.rules().cheat = true;
        try{
            step(2, null);
            assertEquals(1f, target.efficiency, eps);
        }finally{
            Team.sharded.rules().cheat = false;
        }
    }

    @Test
    void heatConsumingDrillWorksThroughConsumeHeat(){
        Drill drill = new Drill("heat-drill" + System.nanoTime());
        drill.size = 2;
        drill.tier = 2;
        drill.drillTime = 300f;
        drill.consumeHeat(10f).maxEfficiency(1f);
        drill.init();

        for(int dx = 0; dx < 2; dx++){
            for(int dy = 0; dy < 2; dy++){
                state.world.tile(cx + dx, cy + dy).setOverlay(Blocks.oreCopper);
            }
        }

        Building target = place(drill, cx, cy, 0);
        step(5, null);
        assertEquals(0f, target.efficiency, eps);
        assertEquals(BlockStatus.noInput, target.status());

        HeatBehaviorTests.builds.clear();
        HeatBehaviorTests.eastSource(target, false);
        HeatBehaviorTests.builds.add(target);
        step(5, null);
        assertEquals(1f, target.efficiency, eps);
    }

    @Test
    void optionalHeatBoostDoesNotGateEfficiency(){
        GenericCrafter crafter = customCrafter("boost-crafter", 2);
        crafter.consumeHeat(10f).boost();
        init(crafter);

        Building target = place(crafter, cx, cy, 0);
        step(3, null);
        assertEquals(1f, target.efficiency, eps);
        assertEquals(0f, target.optionalEfficiency, eps);

        HeatBehaviorTests.builds.clear();
        HeatBehaviorTests.eastSource(target, false);
        HeatBehaviorTests.builds.add(target);
        step(3, null);
        assertEquals(1f, target.efficiency, eps);
        assertEquals(1f, target.optionalEfficiency, eps);
    }

    //endregion
    //region flux reactor

    @Test
    void fluxReactorIsStableWithCoolantMatchingPartialHeat(){
        float need = 9f / 60f, fraction = 75f / 150f;
        VariableReactor.VariableReactorBuild reactor = fluxReactor(75f);

        runFlux(reactor, need * fraction * 1.05f, 120);
        assertEquals(75f, reactor.heatModule.input, eps);
        assertEquals(0f, reactor.instability, eps);
    }

    @Test
    void fluxReactorBecomesUnstableWithCoolantBelowPartialHeat(){
        float need = 9f / 60f, fraction = 75f / 150f;
        VariableReactor.VariableReactorBuild reactor = fluxReactor(75f);

        runFlux(reactor, need * fraction * 0.8f, 120);
        assertTrue(reactor.instability > 0.02f, "instability " + reactor.instability);
    }

    @Test
    void fluxReactorIsStableAboveMaxHeatWithFullCoolant(){
        float need = 9f / 60f;
        VariableReactor.VariableReactorBuild reactor = fluxReactor(400f);

        runFlux(reactor, need * 1.05f, 120);
        assertEquals(400f, reactor.heatModule.input, eps);
        assertEquals(0f, reactor.instability, eps);
    }

    @Test
    void fluxReactorBecomesUnstableWithoutCoolant(){
        VariableReactor.VariableReactorBuild reactor = fluxReactor(75f);

        runFlux(reactor, 0f, 60);
        assertTrue(reactor.instability > 0.1f, "instability " + reactor.instability);
    }

    @Test
    void fluxReactorWithoutHeatDoesNotDrainCoolant(){
        VariableReactor.VariableReactorBuild reactor = fluxReactor(0f);
        reactor.liquids.set(Liquids.cyanogen, 10f);

        step(60, null);
        assertEquals(10f, reactor.liquids.get(Liquids.cyanogen), eps);
        assertEquals(0f, reactor.instability, eps);
    }

    //endregion
}
