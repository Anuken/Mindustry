package power;

import arc.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.power.PowerGenerator.*;
import mindustry.world.consumers.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Tests code related to the power system in general, but not specific blocks.
 * All tests are run with a fixed delta of 0.5 so delta considerations can be tested as well.
 * Additionally, each PowerGraph::update() call will have its own thread frame, i.e. the method will never be called twice within the same frame.
 * Both of these constraints are handled by FakeThreadHandler within PowerTestFixture.
 * Any power amount (produced, consumed, buffered) should be affected by Time.delta() but status should not!
 */
public class PowerTests extends PowerTestFixture{

    @BeforeAll
    static void init(){
        Core.graphics = new FakeGraphics();
        Vars.state = new GameState();
    }

    @Nested
    class PowerGraphTests{

        /**
         * Tests the status of a single consumer after a single update of the power graph which contains a single producer.
         * <p>
         * Assumption: When the consumer requests zero power, status does not change. Default is 0.0f.
         */
        @TestFactory
        DynamicTest[] directConsumerSatisfactionIsAsExpected(){
            return new DynamicTest[]{
            // Note: Unfortunately, the display names are not yet output through gradle. See https://github.com/gradle/gradle/issues/5975
            // That's why we inject the description into the test method for now.
            // Additional Note: If you don't see any labels in front of the values supplied as function parameters, use a better IDE like IntelliJ IDEA.
            dynamicTest("01", () -> simulateDirectConsumption(0.0f, 1.0f, 0.0f, "0.0 produced, 1.0 consumed (no power available)")),
            dynamicTest("02", () -> simulateDirectConsumption(0.0f, 0.0f, 0.0f, "0.0 produced, 0.0 consumed (no power anywhere)")),
            dynamicTest("03", () -> simulateDirectConsumption(1.0f, 0.0f, 1.0f, "1.0 produced, 0.0 consumed (no power requested)")),
            dynamicTest("04", () -> simulateDirectConsumption(1.0f, 1.0f, 1.0f, "1.0 produced, 1.0 consumed (stable consumption)")),
            dynamicTest("05", () -> simulateDirectConsumption(0.5f, 1.0f, 0.5f, "0.5 produced, 1.0 consumed (power shortage)")),
            dynamicTest("06", () -> simulateDirectConsumption(1.0f, 0.5f, 1.0f, "1.0 produced, 0.5 consumed (power excess)")),
            dynamicTest("07", () -> simulateDirectConsumption(0.09f, 0.09f - Mathf.FLOAT_ROUNDING_ERROR / 10.0f, 1.0f, "floating point inaccuracy (stable consumption)"))
            };
        }

        void simulateDirectConsumption(float producedPower, float requiredPower, float expectedSatisfaction, String parameterDescription){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
            producerTile.<GeneratorBuild>bc().productionEfficiency = 1f;
            Tile directConsumerTile = createFakeTile(0, 1, createFakeDirectConsumer(requiredPower));

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile.build);
            powerGraph.add(directConsumerTile.build);

            assertEquals(producedPower * Time.delta, powerGraph.getPowerProduced(), Mathf.FLOAT_ROUNDING_ERROR);
            assertEquals(requiredPower * Time.delta, powerGraph.getPowerNeeded(), Mathf.FLOAT_ROUNDING_ERROR);

            //Update and check for the expected power status of the consumer
            powerGraph.update();
            assertEquals(expectedSatisfaction, directConsumerTile.build.power.status, Mathf.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of direct consumer did not match");
        }

        /**
         * Tests the status of a single direct consumer after a single update of the power graph which contains a single producer and a single battery.
         * The used battery is created with a maximum capacity of 100 and receives ten power per tick.
         */
        @TestFactory
        DynamicTest[] batteryCapacityIsAsExpected(){
            return new DynamicTest[]{
            // Note: expectedBatteryCapacity is currently adjusted to a delta of 0.5! (FakeThreadHandler sets it to that)
            dynamicTest("01", () -> simulateDirectConsumptionWithBattery(10.0f, 0.0f, 0.0f, 5.0f, 0.0f, "Empty battery, no consumer")),
            dynamicTest("02", () -> simulateDirectConsumptionWithBattery(10.0f, 0.0f, 94.999f, 99.999f, 0.0f, "Battery almost full after update, no consumer")),
            dynamicTest("03", () -> simulateDirectConsumptionWithBattery(10.0f, 0.0f, 100.0f, 100.0f, 0.0f, "Full battery, no consumer")),
            dynamicTest("04", () -> simulateDirectConsumptionWithBattery(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, "No producer, no consumer, empty battery")),
            dynamicTest("05", () -> simulateDirectConsumptionWithBattery(0.0f, 0.0f, 100.0f, 100.0f, 0.0f, "No producer, no consumer, full battery")),
            dynamicTest("06", () -> simulateDirectConsumptionWithBattery(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, "No producer, empty battery")),
            dynamicTest("07", () -> simulateDirectConsumptionWithBattery(0.0f, 10.0f, 100.0f, 95.0f, 1.0f, "No producer, full battery")),
            dynamicTest("08", () -> simulateDirectConsumptionWithBattery(0.0f, 10.0f, 2.5f, 0.0f, 0.5f, "No producer, low battery")),
            dynamicTest("09", () -> simulateDirectConsumptionWithBattery(5.0f, 10.0f, 5.0f, 2.5f, 1.0f, "Producer + Battery = Consumed")),
            };
        }

        void simulateDirectConsumptionWithBattery(float producedPower, float requestedPower, float initialBatteryCapacity, float expectedBatteryCapacity, float expectedSatisfaction, String parameterDescription){
            PowerGraph powerGraph = new PowerGraph();

            if(producedPower > 0.0f){
                Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
                producerTile.<GeneratorBuild>bc().productionEfficiency = 1f;
                powerGraph.add(producerTile.build);
            }
            Tile directConsumerTile = null;
            if(requestedPower > 0.0f){
                directConsumerTile = createFakeTile(0, 1, createFakeDirectConsumer(requestedPower));
                powerGraph.add(directConsumerTile.build);
            }
            float maxCapacity = 100f;
            Tile batteryTile = createFakeTile(0, 2, createFakeBattery(maxCapacity));
            batteryTile.build.power.status = initialBatteryCapacity / maxCapacity;

            powerGraph.add(batteryTile.build);

            powerGraph.update();
            assertEquals(expectedBatteryCapacity / maxCapacity, batteryTile.build.power.status, Mathf.FLOAT_ROUNDING_ERROR, parameterDescription + ": Expected battery status did not match");
            if(directConsumerTile != null){
                assertEquals(expectedSatisfaction, directConsumerTile.build.power.status, Mathf.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of direct consumer did not match");
            }
        }

        /** Makes sure a direct consumer stops working after power production is set to zero. */
        @Test
        void directConsumptionStopsWithNoPower(){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(10.0f));
            producerTile.<GeneratorBuild>bc().productionEfficiency = 1.0f;
            Tile consumerTile = createFakeTile(0, 1, createFakeDirectConsumer(5.0f));

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile.build);
            powerGraph.add(consumerTile.build);
            powerGraph.update();

            assertEquals(1.0f, consumerTile.build.power.status, Mathf.FLOAT_ROUNDING_ERROR);

            powerGraph.remove(producerTile.build);
            powerGraph.add(consumerTile.build);
            powerGraph.update();

            assertEquals(0.0f, consumerTile.build.power.status, Mathf.FLOAT_ROUNDING_ERROR);
            if(consumerTile.block().consumes.hasPower()){
                ConsumePower consumePower = consumerTile.block().consumes.getPower();
                assertFalse(consumePower.valid(consumerTile.bc()));
            }
        }
    }
}
