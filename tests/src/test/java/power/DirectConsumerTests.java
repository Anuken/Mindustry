package power;

import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerGenerator.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/** Tests for direct power consumers. */
public class DirectConsumerTests extends PowerTestFixture{


    //https://www.arhohuttunen.com/junit-5-parameterized-tests/
    //Option 1
    // Uses a method to provide parameters via stream arguments
    @ParameterizedTest
    @MethodSource("noPNoItemsParameters")
    void noPowerRequestedWithNoItemsParameterized(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction)
    {
        testUnitFactory(siliconAmount, leadAmount, producedPower,requestedPower, expectedSatisfaction);
    }

    /**
     * Testing extreme negative outliers
     *
     * @return
     */
    private static Stream<Arguments> noPNoItemsParameters()
    {
        return Stream.of(
                arguments(-9999, -10, -2f, -1f, 1f), //extreme negative outlier
                arguments(-1, -1, -0.8f, -0.8f, 1f),
                arguments(30, 0, 0.8f, 0.8f, 1f),
                arguments(60, 30, 0.16f, 0.16f, 1f),
                arguments(90, 60, 0.24f, 0.24f, 1f),
                arguments(120, 90, 0.32f, 0.32f, 1f),
                arguments(12000, 0, 2f, 1f, 1f) //extreme positive outlier

        );
    }

    //Option 2
    //https://stackoverflow.com/questions/61483452/parameterized-test-with-two-arguments-in-junit-5-jupiter
    // CsvSource aka comma seperated source
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


    /**
     *
     *  Testing No Power requests with insufficient items
     *
     *  - having trouble finding where the material amounts impact the expectedSatisfaction result from out tests
     *  Note: Setting requestedPower at 1 guarantees 1 tick will run at our produced power (0.5 means 2 ticks ,etc)
     *
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
    @MethodSource("noPowerRequested_InsufficientItems_Parameters")
    void noPowerRequestedWithInsufficientItemsParameterized(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction)
    {
        testUnitFactory(siliconAmount, leadAmount, producedPower,requestedPower, expectedSatisfaction);
    }
    private static Stream<Arguments> noPowerRequested_InsufficientItems_Parameters()
    {
        return Stream.of(
                arguments(30, 0, 0.08f, 0.08f, 1f), //first 2 tests meet basic test params already tested
                arguments(0, 30, 0.08f, 0.08f, 1f), //second given test
                arguments(15, 0, 0.8f, 0.8f, 1f),
                //0.25 is 1/4, therefore 4 ticks aka 0.1 * 4 = expected status result of 0.4
                arguments(30, 30, 0.1f, 0.25f, 0.4f), //a comparison test with "sufficient" items
                arguments(0, 0, 0.1f, 0.25f, 0.4f), //a comparison test with no items (should give same result)
                arguments(2, 0, 0.1f, 0.25f, 0.4f), //same params as previous 2, except now testing "insufficient" items
                arguments(0, 15, 0.1f, 0.8f, 0.125f), //(1/10 per tick)* (8/10 ticks) = 0.125 power?
                arguments(90, 0, 0.8f, 0.8f, 1f), //large silicon but no lead
                arguments(0, 90,  0.8f, 0.8f, 1f), //large lead but no silicon
                arguments(12000, 0,  0.5f, 1.0f, 0.5f), //SUPER large silicon but no lead
                arguments(0, 12000,  0.4f, 0.8f, 0.5f) //SUPER large silicon but no lead
        );
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
