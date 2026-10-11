import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Characterization tests for heat behavior. They only use content, logic sensors and IO, so they pass both before and after the heat refactor.
 * Heat levels are controlled by running a heat source at a tiny time scale, so it ramps by 0.1 per tick.
 */
public class HeatBehaviorTests{
    static final int cx = 10, cy = 10;
    static final float eps = 1e-3f, rampStep = 0.1f, rampTolerance = 0.01f;
    /** writeAll sizes of non-heat blocks on the unmodified code. Negative means "not recorded yet": the size is printed instead of asserted. */
    static final int kilnSize = 27, graphitePressSize = 21;

    static final Seq<Building> builds = new Seq<>();

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
        Time.setDeltaProvider(() -> 1f);
        clearArea();
    }

    static void clearArea(){
        builds.clear();
        for(int x = 0; x <= 24; x++){
            for(int y = 0; y <= 24; y++){
                state.world.tile(x, y).setAir();
            }
        }
    }

    static Building place(Block block, int x, int y, int rotation){
        Tile tile = state.world.tile(x, y);
        tile.setBlock(block, Team.sharded, rotation);
        builds.add(tile.build);
        return tile.build;
    }

    /** Places a 1x1 heat source touching the east edge of the target and facing it. Place sources before consumers so they update first. */
    static Building eastSource(Building target, boolean slow){
        Building src = place(Blocks.heatSource, target.tile.x + target.block.size / 2 + 1, target.tile.y, 2);
        if(slow){
            src.timeScale = 0.0001f;
            src.timeScaleDuration = 1e9f;
        }
        return src;
    }

    static void step(int ticks, Runnable beforeEach){
        for(Building build : builds){
            build.updateProximity();
        }

        for(int i = 0; i < ticks; i++){
            state.updateId++;
            logic.updateTime();
            if(beforeEach != null) beforeEach.run();
            for(Building build : builds){
                build.update();
            }
        }
    }

    static void clearOutputs(Building build){
        if(build.block instanceof GenericCrafter crafter){
            if(crafter.outputItems != null){
                for(ItemStack stack : crafter.outputItems) build.items.set(stack.item, 0);
            }
            if(crafter.outputLiquids != null){
                for(LiquidStack stack : crafter.outputLiquids) build.liquids.set(stack.liquid, 0f);
            }
        }
    }

    /** Fills power, item and liquid inputs so that heat is the only limiting factor. */
    static void supply(Building build){
        supply(build, 1f);
    }

    static void supply(Building build, float powerStatus){
        if(build.power != null) build.power.status = powerStatus;

        for(Consume cons : build.block.consumers){
            if(cons instanceof ConsumeItems items){
                for(ItemStack stack : items.items){
                    build.items.set(stack.item, Math.min(build.block.itemCapacity, stack.amount * 2));
                }
            }else if(cons instanceof ConsumeLiquid liquid){
                build.liquids.set(liquid.liquid, build.block.liquidCapacity);
            }else if(cons instanceof ConsumeLiquids liquids){
                for(LiquidStack stack : liquids.liquids){
                    build.liquids.set(stack.liquid, build.block.liquidCapacity);
                }
            }
        }

        clearOutputs(build);

        //a fully reloaded idle turret stops consuming, which would zero its efficiency
        if(build instanceof ReloadTurret.ReloadTurretBuild turret) turret.reloadCounter = 0f;
    }

    static void unsupply(Building build){
        if(build.power != null) build.power.status = 0f;
        if(build.items != null) build.items.clear();
        if(build.liquids != null) build.liquids.clear();
    }

    static float heat(Building build){
        return (float)build.sense(LogicProp.heat);
    }

    static int writtenSize(Building build){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        build.writeAll(new Writes(new DataOutputStream(out)));
        return out.size();
    }

    static int closest(float[] samples, float value){
        int best = 0;
        for(int i = 1; i < samples.length; i++){
            if(Math.abs(samples[i] - value) < Math.abs(samples[best] - value)) best = i;
        }
        return best;
    }

    /** Runs the target while the source ramps, recording sensed heat and efficiency every tick. Returns {heats, efficiencies}. */
    static float[][] ramp(Building target, float maxHeat){
        int ticks = (int)(maxHeat / rampStep);
        float[] heats = new float[ticks], efficiencies = new float[ticks];
        for(int i = 0; i < ticks; i++){
            step(1, () -> supply(target));
            heats[i] = heat(target);
            efficiencies[i] = target.efficiency;
        }
        return new float[][]{heats, efficiencies};
    }

    //region sources

    @Test
    void heatSourceReachesFullOutputInOneTick(){
        Building src = place(Blocks.heatSource, cx, cy, 0);
        assertEquals(0f, heat(src), eps);

        step(1, null);
        assertEquals(1000f, heat(src), eps);
    }

    @Test
    void electricHeaterRampsLinearly(){
        Building heater = place(Blocks.electricHeater, cx, cy, 0);

        step(5, () -> supply(heater));
        assertEquals(5 * 0.15f, heat(heater), eps);

        step(100, () -> supply(heater));
        assertEquals(3f, heat(heater), eps);
    }

    @Test
    void producerHeatFollowsEfficiency(){
        Building heater = place(Blocks.electricHeater, cx, cy, 0);

        step(60, () -> supply(heater, 0.5f));
        assertEquals(1.5f, heat(heater), eps);
    }

    @Test
    void producersReachOutputAndDecayToZero(){
        Block[] blocks = {Blocks.electricHeater, Blocks.slagHeater, Blocks.phaseHeater, Blocks.oxidationChamber, Blocks.heatReactor};
        float[] outputs = {3f, 8f, 15f, 5f, 10f};

        for(int i = 0; i < blocks.length; i++){
            clearArea();
            Building build = place(blocks[i], cx, cy, 0);
            String name = blocks[i].name;

            step(130, () -> supply(build));
            assertEquals(outputs[i], heat(build), eps, name + " full output");

            step(130, () -> unsupply(build));
            assertEquals(0f, heat(build), eps, name + " decay");
        }
    }

    @Test
    void neoplasiaReactorHeatFollowsEfficiency(){
        Building reactor = place(Blocks.neoplasiaReactor, cx, cy, 0);

        step(20, () -> supply(reactor));
        assertEquals(20 * 0.15f, heat(reactor), 0.01f);

        step(40, () -> unsupply(reactor));
        assertEquals(0f, heat(reactor), 0.01f);
    }

    //endregion
    //region facing and contact

    @Test
    void sourceFacingConsumerTransfersHeat(){
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);
        Building src = eastSource(consumer, false);
        builds.reverse();

        step(3, null);
        assertEquals(1000f, heat(src), eps);
        assertEquals(1000f, heat(consumer), eps);
    }

    @Test
    void sourceFacingAwayTransfersNothing(){
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);
        Building src = place(Blocks.heatSource, cx + 2, cy, 0);
        builds.reverse();

        step(3, null);
        assertEquals(1000f, heat(src), eps);
        assertEquals(0f, heat(consumer), eps);
    }

    @Test
    void fullContactTransfersAllHeat(){
        Building heater = place(Blocks.electricHeater, cx + 2, cy, 2);
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);

        step(40, () -> supply(heater));
        assertEquals(3f, heat(heater), eps);
        assertEquals(3f / 2f * 2f, heat(consumer), eps);
    }

    @Test
    void partialContactScalesHeat(){
        Building heater = place(Blocks.electricHeater, cx + 2, cy + 1, 2);
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);

        step(40, () -> supply(heater));
        assertEquals(3f, heat(heater), eps);
        assertEquals(3f / 2f * 1f, heat(consumer), eps);
    }

    //endregion
    //region conductors

    @Test
    void largeConductorChainPassesHeatThrough(){
        Building src = place(Blocks.heatSource, cx + 8, cy, 2);
        Building far = place(Blocks.heatRedirector, cx + 6, cy, 2);
        Building near = place(Blocks.heatRedirector, cx + 3, cy, 2);
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);

        step(5, null);
        assertEquals(1000f, heat(src), eps);
        assertEquals(1000f, heat(far), eps);
        assertEquals(1000f, heat(near), eps);
        assertEquals(1000f, heat(consumer), eps);
    }

    @Test
    void smallConductorPassesHeatThrough(){
        Building src = place(Blocks.heatSource, cx + 4, cy, 2);
        Building cond = place(Blocks.smallHeatRedirector, cx + 2, cy, 2);
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);

        step(5, null);
        assertEquals(1000f, heat(cond), eps);
        assertEquals(1000f, heat(consumer), eps);
    }

    @Test
    void conductorCycleDoesNotAmplify(){
        Building src = place(Blocks.heatSource, cx, cy + 2, 3);
        Building a = place(Blocks.heatRedirector, cx, cy, 0);
        Building b = place(Blocks.heatRedirector, cx + 3, cy, 2);

        step(30, null);
        assertTrue(heat(a) > 0f, "heat should still enter the loop");
        assertTrue(heat(a) <= 1000f + eps, "a = " + heat(a));
        assertTrue(heat(b) <= 1000f + eps, "b = " + heat(b));
    }

    @Test
    void disabledConductorOutputsNothing(){
        Building src = place(Blocks.heatSource, cx + 4, cy, 2);
        Building cond = place(Blocks.smallHeatRedirector, cx + 2, cy, 2);
        Building consumer = place(Blocks.carbideCrucible, cx, cy, 0);

        step(5, null);
        assertEquals(1000f, heat(consumer), eps);

        cond.enabled = false;
        step(3, null);
        assertEquals(0f, heat(cond), eps);
        assertEquals(0f, heat(consumer), eps);
    }

    @Test
    void routerSplitsAcrossThreeSides(){
        Building src = place(Blocks.heatSource, cx - 2, cy + 1, 0);
        Building router = place(Blocks.heatRouter, cx, cy, 0);
        Building north = place(Blocks.carbideCrucible, cx, cy + 3, 0);
        Building south = place(Blocks.carbideCrucible, cx, cy - 3, 0);
        Building east = place(Blocks.carbideCrucible, cx + 3, cy, 0);
        Building back = place(Blocks.carbideCrucible, cx - 3, cy - 1, 0);

        step(5, null);
        assertEquals(1000f, heat(router), eps);
        assertEquals(1000f / 3f, heat(north), eps);
        assertEquals(1000f / 3f, heat(south), eps);
        assertEquals(1000f / 3f, heat(east), eps);
        assertEquals(0f, heat(back), eps);
    }

    //endregion
    //region consumers

    @Test
    void heatCraftersNeedHeatToWork(){
        Block[] blocks = {Blocks.atmosphericConcentrator, Blocks.carbideCrucible, Blocks.surgeCrucible, Blocks.cyanogenSynthesizer, Blocks.phaseSynthesizer};

        for(Block block : blocks){
            clearArea();
            Building build = place(block, cx, cy, 0);
            supply(build);
            int total = build.items == null ? 0 : build.items.total();

            step(10, () -> build.power.status = 1f);

            assertEquals(BlockStatus.noInput, build.status(), block.name);
            assertEquals(0f, ((GenericCrafter.GenericCrafterBuild)build).progress, eps, block.name);
            assertEquals(0f, build.efficiency, eps, block.name);
            assertEquals(total, build.items == null ? 0 : build.items.total(), block.name);
            assertEquals(0f, heat(build), eps, block.name);
        }
    }

    @Test
    void heatCraftersScaleWithHeatAndCapAtMaxEfficiency(){
        Block[] blocks = {Blocks.atmosphericConcentrator, Blocks.carbideCrucible, Blocks.surgeCrucible, Blocks.cyanogenSynthesizer, Blocks.phaseSynthesizer};
        float[] requirements = {24f, 40f, 40f, 20f, 32f};

        for(int b = 0; b < blocks.length; b++){
            clearArea();
            float req = requirements[b];
            String name = blocks[b].name;

            //placed at the origin so the source can be created first; the real consumer takes its place in the update order
            Building consumer = place(blocks[b], cx, cy, 0);
            builds.clear();
            Building src = eastSource(consumer, true);
            builds.add(consumer);

            float[][] result = ramp(consumer, req * 3f);
            float[] heats = result[0], efficiencies = result[1];

            float maxSeen = 0f;
            for(int i = 0; i < heats.length; i++){
                float expected = Math.min(Math.min(heats[i] / req, 1f) + Math.max(heats[i] - req, 0f) / req, 1f);
                assertEquals(expected, efficiencies[i], rampTolerance, name + " at heat " + heats[i]);
                maxSeen = Math.max(maxSeen, efficiencies[i]);
            }

            assertEquals(0.5f, efficiencies[closest(heats, req / 2f)], rampTolerance, name + " half");
            assertEquals(1f, efficiencies[closest(heats, req)], rampTolerance, name + " requirement");
            assertEquals(1f, efficiencies[closest(heats, req * 2f)], rampTolerance, name + " double");
            assertTrue(maxSeen <= 1f + eps, name + " exceeded max efficiency: " + maxSeen);
        }
    }

    @Test
    void turretsWithoutHeatDoNotWork(){
        Block[] blocks = {Blocks.afflict, Blocks.malign};

        for(Block block : blocks){
            clearArea();
            Building turret = place(block, cx, cy, 0);

            step(10, () -> supply(turret));

            assertEquals(0f, turret.efficiency, eps, block.name);
            assertEquals(BlockStatus.noInput, turret.status(), block.name);
            assertFalse(turret.canConsume(), block.name);
            assertEquals(0f, heat(turret), eps, block.name);
        }
    }

    @Test
    void turretEfficiencyScalesWithHeatAndIsCapped(){
        Block[] blocks = {Blocks.afflict, Blocks.malign};
        float[] requirements = {20f, 144f};

        for(int b = 0; b < blocks.length; b++){
            clearArea();
            float req = requirements[b];
            String name = blocks[b].name;

            Building turret = place(blocks[b], cx, cy, 0);
            builds.clear();
            eastSource(turret, true);
            builds.add(turret);

            float[][] result = ramp(turret, req * 1.5f);
            float[] heats = result[0], efficiencies = result[1];

            float maxSeen = 0f;
            for(int i = 0; i < heats.length; i++){
                assertEquals(Math.min(heats[i] / req, 1f), efficiencies[i], rampTolerance, name + " at heat " + heats[i]);
                maxSeen = Math.max(maxSeen, efficiencies[i]);
            }

            assertEquals(0.5f, efficiencies[closest(heats, req / 2f)], rampTolerance, name + " half");
            assertEquals(1f, efficiencies[closest(heats, req * 1.4f)], rampTolerance, name + " capped");
            assertTrue(maxSeen <= 1f + eps, name + " exceeded max heat efficiency: " + maxSeen);
        }
    }

    @Test
    void turretWithoutHeatRequirementSensesNaN(){
        Building arc = place(Blocks.arc, cx, cy, 0);
        step(2, null);
        assertTrue(Double.isNaN(arc.sense(LogicProp.heat)));
    }

    @Test
    void fluxReactorEfficiencyFollowsHeat(){
        Building reactor = place(Blocks.fluxReactor, cx, cy, 0);
        builds.clear();
        eastSource(reactor, true);
        builds.add(reactor);

        float[][] result = ramp(reactor, 60f);
        float[] heats = result[0], efficiencies = result[1];

        for(int i = 0; i < heats.length; i++){
            assertEquals(Math.min(heats[i] / 150f, 1f), efficiencies[i], rampTolerance, "heat " + heats[i]);
        }
        assertTrue(efficiencies[closest(heats, 60f)] > efficiencies[closest(heats, 30f)]);
        assertEquals(60f, heat(reactor), 0.2f);
    }

    @Test
    void nuclearReactorEmitsToAdjacentConsumer(){
        Building reactor = place(Blocks.thoriumReactor, cx, cy, 0);
        Building consumer = place(Blocks.carbideCrucible, cx + 3, cy, 0);

        step(10, () -> reactor.items.set(Items.thorium, 30));

        //sensing the reactor itself reports its internal 0-1 heat, while neighbors receive heat * heatOutput (8)
        assertEquals(0.05f, heat(reactor), eps);
        assertEquals(0.05f * 8f, heat(consumer), eps);
    }

    //endregion
    //region non-heat blocks

    @Test
    void nonHeatBlocksKeepTheirSaveLayout(){
        Building kiln = place(Blocks.kiln, cx, cy, 0);
        Building press = place(Blocks.graphitePress, cx + 5, cy, 0);

        assertEquals(0, kiln.moduleBitmask() & (1 << 6));
        assertEquals(0, press.moduleBitmask() & (1 << 6));

        int kilnWritten = writtenSize(kiln), pressWritten = writtenSize(press);
        if(kilnSize >= 0) assertEquals(kilnSize, kilnWritten);
        if(graphitePressSize >= 0) assertEquals(graphitePressSize, pressWritten);
    }

    //endregion
}
