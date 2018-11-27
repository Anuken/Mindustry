package power;

import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.mindustry.world.consumers.ConsumePower;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

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
        DynamicTest[] testDirectConsumption(){
            return new DynamicTest[]{
                // Note: Unfortunately, the display names are not yet output through gradle. See https://github.com/gradle/gradle/issues/5975
                // That's why we inject the description into the test method for now.
                dynamicTest("01", () -> test_directConsumptionCalculation(0.0f, 1.0f, 0.0f, "0.0 produced, 1.0 consumed (no power available)")),
                dynamicTest("02", () -> test_directConsumptionCalculation(0.0f, 0.0f, 0.0f, "0.0 produced, 0.0 consumed (no power anywhere)")),
                dynamicTest("03", () -> test_directConsumptionCalculation(1.0f, 0.0f, 0.0f, "1.0 produced, 0.0 consumed (no power requested)")),
                dynamicTest("04", () -> test_directConsumptionCalculation(1.0f, 1.0f, 1.0f, "1.0 produced, 1.0 consumed (stable consumption)")),
                dynamicTest("05", () -> test_directConsumptionCalculation(0.5f, 1.0f, 0.5f, "0.5 produced, 1.0 consumed (power shortage)")),
                dynamicTest("06", () -> test_directConsumptionCalculation(1.0f, 0.5f, 1.0f, "1.0 produced, 0.5 consumed (power excess)")),
                dynamicTest("07", () -> test_directConsumptionCalculation(0.09f, 0.09f - MathUtils.FLOAT_ROUNDING_ERROR / 10.0f, 1.0f, "floating point inaccuracy (stable consumption)"))
            };
        }
        void test_directConsumptionCalculation(float producedPower, float requiredPower, float expectedSatisfaction, String parameterDescription){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
            Tile directConsumerTile = createFakeTile(0, 1, createFakeDirectConsumer(requiredPower, 0.6f));

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile);
            powerGraph.add(directConsumerTile);

            assumeTrue(MathUtils.isEqual(producedPower, powerGraph.getPowerProduced()));
            assumeTrue(MathUtils.isEqual(requiredPower, powerGraph.getPowerNeeded()));

            // Update and check for the expected power satisfaction of the consumer
            powerGraph.update();
            assertEquals(expectedSatisfaction, directConsumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of direct consumer did not match");
        }

        /** Tests the satisfaction of a single buffered consumer after a single update of the power graph which contains a single producer. */
        @TestFactory
        DynamicTest[] testBufferedConsumption(){
            return new DynamicTest[]{
                // Note: powerPerTick may not be 0 in any of the test cases. This would equal a "ticksToFill" of infinite.
                dynamicTest("01", () -> test_bufferedConsumptionCalculation(0.0f, 0.0f, 0.1f, 0.0f, 0.0f, "Empty Buffer, No power anywhere")),
                dynamicTest("02", () -> test_bufferedConsumptionCalculation(0.0f, 1.0f, 0.1f, 0.0f, 0.0f, "Empty Buffer, No power provided")),
                dynamicTest("03", () -> test_bufferedConsumptionCalculation(1.0f, 0.0f, 0.1f, 0.0f, 0.0f, "Empty Buffer, No power requested")),
                dynamicTest("04", () -> test_bufferedConsumptionCalculation(1.0f, 1.0f, 1.0f, 0.0f, 1.0f, "Empty Buffer, Stable Power, One tick to fill")),
                dynamicTest("05", () -> test_bufferedConsumptionCalculation(1.0f, 1.0f, 0.1f, 0.0f, 0.1f, "Empty Buffer, Stable Power, multiple ticks to fill")),
                dynamicTest("06", () -> test_bufferedConsumptionCalculation(1.0f, 0.5f, 0.5f, 0.0f, 1.0f, "Empty Buffer, Power excess, one tick to fill")),
                dynamicTest("07", () -> test_bufferedConsumptionCalculation(1.0f, 0.5f, 0.1f, 0.0f, 0.2f, "Empty Buffer, Power excess, multiple ticks to fill")),
                dynamicTest("08", () -> test_bufferedConsumptionCalculation(0.5f, 1.0f, 1.0f, 0.0f, 0.5f, "Empty Buffer, Power shortage, one tick to fill")),
                dynamicTest("09", () -> test_bufferedConsumptionCalculation(0.5f, 1.0f, 0.1f, 0.0f, 0.1f, "Empty Buffer, Power shortage, multiple ticks to fill")),
                dynamicTest("10", () -> test_bufferedConsumptionCalculation(0.0f, 1.0f, 0.1f, 0.5f, 0.5f, "Unchanged buffer with no power produced")),
                dynamicTest("11", () -> test_bufferedConsumptionCalculation(1.0f, 1.0f, 0.1f, 1.0f, 1.0f, "Unchanged buffer when already full")),
                dynamicTest("12", () -> test_bufferedConsumptionCalculation(0.2f, 1.0f, 0.5f, 0.5f, 0.7f, "Half buffer, power shortage")),
                dynamicTest("13", () -> test_bufferedConsumptionCalculation(1.0f, 1.0f, 0.5f, 0.7f, 1.0f, "Buffer does not get exceeded")),
                dynamicTest("14", () -> test_bufferedConsumptionCalculation(1.0f, 1.0f, 0.5f, 0.5f, 1.0f, "Half buffer, filled with excess"))
            };
        }
        void test_bufferedConsumptionCalculation(float producedPower, float maxBuffer, float powerPerTick, float initialSatisfaction, float expectedSatisfaction, String parameterDescription){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
            Tile bufferedConsumerTile = createFakeTile(0, 1, createFakeBufferedConsumer(maxBuffer, maxBuffer > 0.0f ? maxBuffer/powerPerTick : 1.0f));
            bufferedConsumerTile.entity.power.satisfaction = initialSatisfaction;

            PowerGraph powerGraph = new PowerGraph();
            powerGraph.add(producerTile);
            powerGraph.add(bufferedConsumerTile);

            assumeTrue(MathUtils.isEqual(producedPower, powerGraph.getPowerProduced()));
            //assumeTrue(MathUtils.isEqual(Math.min(maxBuffer, powerPerTick), powerGraph.getPowerNeeded()));

            // Update and check for the expected power satisfaction of the consumer
            powerGraph.update();
            assertEquals(expectedSatisfaction, bufferedConsumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of buffered consumer did not match");
        }

        /** Tests the satisfaction of a single direct consumer after a single update of the power graph which contains a single producer and a single battery.
         *  The used battery is created with a maximum capacity of 100 and receives ten power per tick.
         */
        @TestFactory
        DynamicTest[] testDirectConsumptionWithBattery(){
            return new DynamicTest[]{
                dynamicTest("01", () -> test_directConsumptionWithBattery(10.0f, 0.0f, 0.0f, 10.0f, 0.0f, "Empty battery, no consumer")),
                dynamicTest("02", () -> test_directConsumptionWithBattery(10.0f, 0.0f, 90.0f, 100.0f, 0.0f, "Battery full after update, no consumer")),
                dynamicTest("03", () -> test_directConsumptionWithBattery(10.0f, 0.0f, 100.0f, 100.0f, 0.0f, "Full battery, no consumer")),
                dynamicTest("04", () -> test_directConsumptionWithBattery(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, "No producer, no consumer, empty battery")),
                dynamicTest("05", () -> test_directConsumptionWithBattery(0.0f, 0.0f, 100.0f, 100.0f, 0.0f, "No producer, no consumer, full battery")),
                dynamicTest("06", () -> test_directConsumptionWithBattery(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, "No producer, empty battery")),
                dynamicTest("07", () -> test_directConsumptionWithBattery(0.0f, 10.0f, 100.0f, 90.0f, 1.0f, "No producer, full battery")),
                dynamicTest("08", () -> test_directConsumptionWithBattery(0.0f, 10.0f, 5.0f, 0.0f, 0.5f, "No producer, low battery")),
                dynamicTest("09", () -> test_directConsumptionWithBattery(5.0f, 10.0f, 5.0f, 0.0f, 1.0f, "Producer + Battery = Consumed")),
            };
        }
        void test_directConsumptionWithBattery(float producedPower, float requestedPower, float initialBatteryCapacity, float expectedBatteryCapacity, float expectedSatisfaction, String parameterDescription){
            PowerGraph powerGraph = new PowerGraph();

            if(producedPower > 0.0f){
                Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(producedPower));
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
            assertEquals(expectedBatteryCapacity, batteryTile.entity.power.satisfaction * maxCapacity, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Expected battery capacity did not match");
            if(directConsumerTile != null){
                assertEquals(expectedSatisfaction, directConsumerTile.entity.power.satisfaction, MathUtils.FLOAT_ROUNDING_ERROR, parameterDescription + ": Satisfaction of direct consumer did not match");
            }
        }

        /** Makes sure a direct consumer stops working after power production is set to zero. */
        @Test
        void testDirectConsumptionStopsWithNoPower(){
            Tile producerTile = createFakeTile(0, 0, createFakeProducerBlock(10.0f));
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
