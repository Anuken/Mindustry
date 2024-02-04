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

    private static Stream<Arguments> noPNoItemsParameters()
    {
        return Stream.of(
                arguments(0, 0, 0.08f, 0.08f, 1f),
                arguments(0, 0, 0.08f, 0.08f, 1f),
                arguments(0, 0, 0.08f, 0.08f, 1f)
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
        ((GeneratorBuild)producerTile.build).productionEfficiency = 1f;

        PowerGraph graph = new PowerGraph();
        graph.add(producerTile.build);
        graph.add(ct.build);

        ct.build.update();
        graph.update();

        assertEquals(expectedSatisfaction, ct.build.power.status);
    }
}
