package power;

import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.PowerGenerator;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.mindustry.world.consumers.ConsumePower;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * Tests code related to the power system in general, but not specific blocks.
 * All tests are run with a fixed delta of 0.5 so delta considerations can be tested as well.
 * Additionally, each PowerGraph::update() call will have its own thread frame, i.e. the method will never be called twice within the same frame.
 * Both of these constraints are handled by FakeThreadHandler within PowerTestFixture.
 * Any power amount (produced, consumed, buffered) should be affected by FakeThreadHandler.fakeDelta but satisfaction should not!
 */
public class PowerTests extends PowerTestFixture{

    @BeforeEach
    void initTest(){
    }

    @Nested
    class PowerGraphTests{

        /** Tests the satisfaction of a single consumer after a single update of the power graph which contains a single producer.
         *
         *  Assumption: When the consumer requests zero power, satisfaction does not change. Default is 0.0f.
         */
        @TestFactory
        DynamicTest[] directConsumerSatisfactionIsAsExpected(){
            return new DynamicTest[]{
                // Note: Unfortunately, the display names are not yet output through gradle. See https://github.com/gradle/gradle/issues/5975
                // That's why we inject the description into the test method for now.
                // Additional Note: If you don't see any labels in front of the values supplied as function parameters, use a better IDE like IntelliJ IDEA.
                dynamicTest("01", () -> simulateDirectConsumption(0.0f, 1.0f, 0.0f, "0.0 produced, 1.0 consumed (no power available)")),
                dynamicTest("02", () -> simulateDirectConsumption(0.0f, 0.0f, 0.0f, "0.0 produced, 0.0 consumed (no power anywhere)")),
                dynamicTest("03", () -> simulateDirectConsumption(1.0f, 0.0f, 0.0f, "1.0 produced, 0.0 consumed (no power requested)")),
                dynamicTest("04", () -> simulateDirectConsumption(1.0f, 1.0f, 1.0f, "1.0 produced, 1.0 consumed (stable consumption)")),
                dynamicTest("05", () -> simulateDirectConsumption(0.5f, 1.0f, 0.5f, "0.5 produced, 1.0 consumed (power shortage)")),
                dynamicTest("06", () -> simulateDirectConsumption(1.0f, 0.5f, 1.0f, "1.0 produced, 0.5 consumed (power excess)")),
                dynamicTest("07", () -> simulateDirectConsumption(0.09f, 0.09f - MathUtils.FLOAT_ROUNDING_ERROR / 10.0f, 1.0f, "floating point inaccuracy (stable consumption)"))
            };
        }
        void simulateDirectConsumption(float producedPower, float requiredPower, float expectedSatisfaction, String parameterDescription){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
            producerTile.<PowerGenerator.GeneratorEntity>entity().productionEfficiency = 1.0f;
            Tile directConsumerTile = createFakeTile(0, 1, createFakeDirectConsumer(requiredPower, 0.6f));

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile);
            powerGraph.add(directConsumerTile);

            assertEquals(producedPower * FakeThreadHandler.fakeDelta, powerGraph.getPowerProduced(), MathUtils.FLOAT_ROUNDING_ERROR);
            assertEquals(requiredPower * FakeThreadHandler.fakeDelta, powerGraph.getPowerNeeded(), MathUtils.FLOAT_ROUNDING_ERROR);

            // Update and check for the expected power satisfaction of the consumer
            powerGraph.update();
            assertEquals(expectedSatisfaction, directConsumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of direct consumer did not match");
        }

        /** Tests the satisfaction of a single buffered consumer after a single update of the power graph which contains a single producer. */
        @TestFactory
        DynamicTest[] bufferedConsumerSatisfactionIsAsExpected(){
            return new DynamicTest[]{
                // Note: powerPerTick may not be 0 in any of the test cases. This would equal a "ticksToFill" of infinite.
                // Note: Due to a fixed delta of 0.5, only half of what is defined here will in fact be produced/consumed. Keep this in mind when defining expectedSatisfaction!
                dynamicTest("01", () -> simulateBufferedConsumption(0.0f, 0.0f, 0.1f, 0.0f, 0.0f, "Empty Buffer, No power anywhere")),
                dynamicTest("02", () -> simulateBufferedConsumption(0.0f, 1.0f, 0.1f, 0.0f, 0.0f, "Empty Buffer, No power provided")),
                dynamicTest("03", () -> simulateBufferedConsumption(1.0f, 0.0f, 0.1f, 0.0f, 0.0f, "Empty Buffer, No power requested")),
                dynamicTest("04", () -> simulateBufferedConsumption(1.0f, 1.0f, 1.0f, 0.0f, 0.5f, "Empty Buffer, Stable Power, One tick to fill")),
                dynamicTest("05", () -> simulateBufferedConsumption(2.0f, 1.0f, 2.0f, 0.0f, 1.0f, "Empty Buffer, Stable Power, One delta to fill")),
                dynamicTest("06", () -> simulateBufferedConsumption(1.0f, 1.0f, 0.1f, 0.0f, 0.05f, "Empty Buffer, Stable Power, multiple ticks to fill")),
                dynamicTest("07", () -> simulateBufferedConsumption(1.2f, 0.5f, 1.0f, 0.0f, 1.0f, "Empty Buffer, Power excess, one delta to fill")),
                dynamicTest("08", () -> simulateBufferedConsumption(1.0f, 0.5f, 0.1f, 0.0f, 0.1f, "Empty Buffer, Power excess, multiple ticks to fill")),
                dynamicTest("09", () -> simulateBufferedConsumption(1.0f, 1.0f, 2.0f, 0.0f, 0.5f, "Empty Buffer, Power shortage, one delta to fill")),
                dynamicTest("10", () -> simulateBufferedConsumption(0.5f, 1.0f, 0.1f, 0.0f, 0.05f, "Empty Buffer, Power shortage, multiple ticks to fill")),
                dynamicTest("11", () -> simulateBufferedConsumption(0.0f, 1.0f, 0.1f, 0.5f, 0.5f, "Unchanged buffer with no power produced")),
                dynamicTest("12", () -> simulateBufferedConsumption(1.0f, 1.0f, 0.1f, 1.0f, 1.0f, "Unchanged buffer when already full")),
                dynamicTest("13", () -> simulateBufferedConsumption(0.2f, 1.0f, 0.5f, 0.5f, 0.6f, "Half buffer, power shortage")),
                dynamicTest("14", () -> simulateBufferedConsumption(1.0f, 1.0f, 0.5f, 0.9f, 1.0f, "Buffer does not get exceeded")),
                dynamicTest("15", () -> simulateBufferedConsumption(2.0f, 1.0f, 1.0f, 0.5f, 1.0f, "Half buffer, filled with excess"))
            };
        }
        void simulateBufferedConsumption(float producedPower, float maxBuffer, float powerConsumedPerTick, float initialSatisfaction, float expectedSatisfaction, String parameterDescription){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
            producerTile.<PowerGenerator.GeneratorEntity>entity().productionEfficiency = 1.0f;
            Tile bufferedConsumerTile = createFakeTile(0, 1, createFakeBufferedConsumer(maxBuffer, maxBuffer > 0.0f ? maxBuffer/powerConsumedPerTick : 1.0f));
            bufferedConsumerTile.entity.power.satisfaction = initialSatisfaction;

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile);
            powerGraph.add(bufferedConsumerTile);

            assertEquals(producedPower * FakeThreadHandler.fakeDelta, powerGraph.getPowerProduced(), MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Produced power did not match");
            assertEquals(Math.min(maxBuffer, powerConsumedPerTick * FakeThreadHandler.fakeDelta), powerGraph.getPowerNeeded(), MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": ConsumedPower did not match");

            // Update and check for the expected power satisfaction of the consumer
            powerGraph.update();
            assertEquals(expectedSatisfaction, bufferedConsumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of buffered consumer did not match");
        }

        /** Tests the satisfaction of a single direct consumer after a single update of the power graph which contains a single producer and a single battery.
         *  The used battery is created with a maximum capacity of 100 and receives ten power per tick.
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
                dynamicTest("09", () -> simulateDirectConsumptionWithBattery(5.0f, 10.0f, 5.0f, 0.0f, 1.0f, "Producer + Battery = Consumed")),
            };
        }
        void simulateDirectConsumptionWithBattery(float producedPower, float requestedPower, float initialBatteryCapacity, float expectedBatteryCapacity, float expectedSatisfaction, String parameterDescription){
            PowerGraph powerGraph = new PowerGraph();

            if(producedPower > 0.0f){
                Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
                producerTile.<PowerGenerator.GeneratorEntity>entity().productionEfficiency = 1.0f;
                powerGraph.add(producerTile);
            }
            Tile directConsumerTile = null;
            if(requestedPower > 0.0f){
                directConsumerTile = createFakeTile(0, 1, createFakeDirectConsumer(requestedPower, 0.6f));
                powerGraph.add(directConsumerTile);
            }
            float maxCapacity = 100f;
            Tile batteryTile = createFakeTile(0, 2, createFakeBattery(maxCapacity, 10 ));
            batteryTile.entity.power.satisfaction = initialBatteryCapacity / maxCapacity;

            powerGraph.add(batteryTile);

            powerGraph.update();
            assertEquals(expectedBatteryCapacity / maxCapacity, batteryTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Expected battery satisfaction did not match");
            if(directConsumerTile != null){
                assertEquals(expectedSatisfaction, directConsumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of direct consumer did not match");
            }
        }

        /** Makes sure a direct consumer stops working after power production is set to zero. */
        @Test
        void directConsumptionStopsWithNoPower(){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(10.0f));
            producerTile.<PowerGenerator.GeneratorEntity>entity().productionEfficiency = 1.0f;
            Tile consumerTile = createFakeTile(0, 1, createFakeDirectConsumer(5.0f, 0.6f));

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile);
            powerGraph.add(consumerTile);
            powerGraph.update();

            assertEquals(1.0f, consumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR);

            powerGraph.remove(producerTile);
            powerGraph.add(consumerTile);
            powerGraph.update();

            assertEquals(0.0f, consumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR);
            if(consumerTile.block().consumes.has(ConsumePower.class)){
                ConsumePower consumePower = consumerTile.block().consumes.get(ConsumePower.class);
                assertFalse(consumePower.valid(consumerTile.block(), consumerTile.entity()));
            }
        }
    }
}
