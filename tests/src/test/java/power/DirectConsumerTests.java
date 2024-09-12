package power;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerGenerator.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for direct power consumers. */
public class DirectConsumerTests extends PowerTestFixture{


    /**
     * CsvSource parameterized tests, testing outliers
     */
    @ParameterizedTest
    @CsvSource({"0, 0, 0.08f, 0.08f, 1f", "0, 0, 0.08f, 0.08f, 1f", "0, 0, 0.08f, 0.08f, 1f"})
    public void noPowerRequestedWithNoItemsParameterized2(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction)
    {
        testUnitFactory(siliconAmount, leadAmount, producedPower,requestedPower, expectedSatisfaction);
    }

    @Test
    void noPowerRequestedWithNoItems(){
        testUnitFactory(0, 0, 0.08f, 0.08f, 1f);
    }

    /** Testing No Power requests with various sufficient items.
     *  Test Order: Sufficient Items, test with no items (should give same result), except now testing "insufficient" items, another insufficient test,
     *  large silicon but no lead, large lead but no silicon, SUPER large silicon but no lead. SUPER large silicon but no lead
     * @param siliconAmount
     * Item needed for power request
     * @param leadAmount
     * Item needed for power request
     * @param producedPower
     *  The amount of power produced per tick in case of an efficiency of 1.0, which represents 100%.
     *  Higher produced power means higher power module status
     * @param requestedPower
     * The amount of power which is required each tick for 100% efficiency. How much power we consume per tick.
     * Lower request power means reduced power module status
     * @param expectedSatisfaction
     * Expected result from input. This will be compared to power module status to see if we meet test results.
     */
    @ParameterizedTest
    @CsvSource({"30, 30, 0.1f, 0.25f, 0.4f", "0, 0, 0.1f, 0.25f, 0.4f", "2, 0, 0.1f, 0.25f, 0.4f", "0, 15, 0.1f, 0.8f, 0.125f",
    "90, 0, 0.8f, 0.8f, 1f", "0, 90,  0.8f, 0.8f, 1f", "12000, 0,  0.5f, 1.0f, 0.5f", "0, 12000,  0.4f, 0.8f, 0.5f"})
    void noPowerRequestedSufficiencyParameterized(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction)
    {
        testUnitFactory(siliconAmount, leadAmount, producedPower,requestedPower, expectedSatisfaction);
    }

    @Test
    void noPowerRequestedWithInsufficientItems(){
        testUnitFactory(30, 0, 0.08f, 0.08f, 1f);
        testUnitFactory(0, 30, 0.08f, 0.08f, 1f);
    }

    @Test
    void powerRequestedWithSufficientItems(){
        testUnitFactory(30, 30, 0.08f, 0.08f, 1.0f);
    }

    void testUnitFactory(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction){
        Tile ct = createFakeTile(0, 0, new GenericCrafter("fakefactory"){{
            hasPower = true;
            hasItems = true;
            consumePower(requestedPower);
            consumeItems(new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30));
        }});
        ct.block().init();
        ct.build.items.add(Items.silicon, siliconAmount);
        ct.build.items.add(Items.lead, leadAmount);

        Tile producerTile = createFakeTile(2, 0, createFakeProducerBlock(producedPower));
        //production efficiently: The efficiency of the producer. An efficiency of 1.0 means 100%
        ((GeneratorBuild)producerTile.build).productionEfficiency = 1f;

        PowerGraph graph = new PowerGraph();
        graph.add(producerTile.build);
        graph.add(ct.build);

        ct.build.update();
        graph.update();

        assertEquals(expectedSatisfaction, ct.build.power.status);
    }
}
