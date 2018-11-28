package power;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.BurnerGenerator;
import io.anuke.mindustry.world.blocks.power.ItemGenerator;
import io.anuke.mindustry.world.blocks.power.ItemLiquidGenerator;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * This class tests ItemLiquidGenerators. Currently, testing is only performed on the BurnerGenerator subclass,
 * which means only power calculations based on flammability are tested.
 * All tests are run with a fixed delta of 0.5 so delta considerations can be tested as well.
 * Additionally, each PowerGraph::update() call will have its own thread frame, i.e. the method will never be called twice within the same frame.
 * Both of these constraints are handled by FakeThreadHandler within PowerTestFixture.
 * Any power amount (produced, consumed, buffered) should be affected by FakeThreadHandler.fakeDelta but satisfaction should not!
 */
public class ItemLiquidGeneratorTests extends PowerTestFixture{

    private ItemLiquidGenerator generator;
    private Tile tile;
    private ItemGenerator.ItemGeneratorEntity entity;
    private final float fakeLiquidPowerMultiplier = 2.0f;
    private final float fakeItemDuration = 60f; // 60 ticks
    private final float maximumLiquidUsage = 0.5f;

    @BeforeEach
    public void createBurnerGenerator(){
        // Use a burner generator instead of a custom ItemLiquidGenerator subclass since we would implement abstract methods the same way.
        generator = new BurnerGenerator("fakegen"){{
            powerProduction = 0.1f;
            itemDuration = 60f;
            liquidPowerMultiplier = fakeLiquidPowerMultiplier;
            itemDuration = fakeItemDuration;
            maxLiquidGenerate = maximumLiquidUsage;
        }};

        tile = createFakeTile(0, 0, generator);
        entity = tile.entity();
    }

    /** Tests the consumption and efficiency when being supplied with liquids. */
    @TestFactory
    DynamicTest[] generatorWorksProperlyWithLiquidInput(){
        return new DynamicTest[]{
            dynamicTest("01", () -> simulateLiquidConsumption(Liquids.oil, 0.0f, "No liquids provided")),
            dynamicTest("02", () -> simulateLiquidConsumption(Liquids.oil, maximumLiquidUsage / 4.0f, "Low oil provided")),
            dynamicTest("03", () -> simulateLiquidConsumption(Liquids.oil, maximumLiquidUsage * 1.0f, "Sufficient oil provided")),
            dynamicTest("04", () -> simulateLiquidConsumption(Liquids.oil, maximumLiquidUsage * 2.0f, "Excess oil provided"))
            // Note: The generator will decline any other liquid since it's not flammable
        };
    }

    void simulateLiquidConsumption(Liquid liquid, float availableLiquidAmount, String parameterDescription){
        final float baseEfficiency = fakeLiquidPowerMultiplier * liquid.flammability;
        final float expectedEfficiency = Math.min(1.0f, availableLiquidAmount / maximumLiquidUsage) * baseEfficiency;
        final float expectedConsumptionPerTick = Math.min(maximumLiquidUsage, availableLiquidAmount);
        final float expectedRemainingLiquidAmount = Math.max(0.0f, availableLiquidAmount - expectedConsumptionPerTick * FakeThreadHandler.fakeDelta);

        assertTrue(generator.acceptLiquid(tile, null, liquid, availableLiquidAmount), parameterDescription + ": Liquids which will be declined by the generator don't need to be tested - The code won't be called for those cases.");

        // Reset liquids since BeforeEach will not be called between dynamic tests
        for(Liquid tmpLiquid : Vars.content.liquids()){
            entity.liquids.reset(tmpLiquid, 0.0f);
        }
        entity.liquids.add(liquid, availableLiquidAmount);
        entity.cons.update(tile.entity);
        assertTrue(entity.cons.valid());

        // Perform an update on the generator once - This should use up any resource up to the maximum liquid usage
        generator.update(tile);

        assertEquals(expectedRemainingLiquidAmount, entity.liquids.get(liquid), parameterDescription + ": Remaining liquid amount mismatch.");
        assertEquals(expectedEfficiency, entity.productionEfficiency, parameterDescription + ": Efficiency mismatch.");
    }

    /** Tests the consumption and efficiency when being supplied with items. */
    @TestFactory
    DynamicTest[] generatorWorksProperlyWithItemInput(){
        return new DynamicTest[]{
            dynamicTest("01", () -> simulateItemConsumption(Items.coal, 0, "No items provided")),
            dynamicTest("02", () -> simulateItemConsumption(Items.coal, 1, "Sufficient coal provided")),
            dynamicTest("03", () -> simulateItemConsumption(Items.coal, 10, "Excess coal provided")),
            dynamicTest("04", () -> simulateItemConsumption(Items.blastCompound, 1, "Blast compound provided")),
            //dynamicTest("03", () -> simulateItemConsumption(Items.plastanium, 1, "Plastanium provided")), // Not accepted by generator due to low flammability
            dynamicTest("05", () -> simulateItemConsumption(Items.biomatter, 1, "Biomatter provided")),
            dynamicTest("06", () -> simulateItemConsumption(Items.pyratite, 1, "Pyratite provided"))
        };
    }

    void simulateItemConsumption(Item item, int amount, String parameterDescription){
        final float expectedEfficiency = Math.min(1.0f, amount > 0 ? item.flammability : 0f);
        final float expectedRemainingItemAmount = Math.max(0, amount - 1);
        assertTrue(generator.acceptItem(item, tile, null), parameterDescription + ": Items which will be declined by the generator don't need to be tested - The code won't be called for those cases.");


        // Clean up manually since BeforeEach will not be called between dynamic tests
        entity.items.clear();
        entity.generateTime = 0.0f;
        entity.productionEfficiency = 0.0f;

        if(amount > 0){
            entity.items.add(item, amount);
        }
        entity.cons.update(tile.entity);
        assertTrue(entity.cons.valid());

        // Perform an update on the generator once - This should use up one or zero items - dependent on if the item is accepted and available or not.
        generator.update(tile);

        assertEquals(expectedRemainingItemAmount, entity.items.get(item), parameterDescription + ": Remaining item amount mismatch.");
        assertEquals(expectedEfficiency, entity.productionEfficiency, parameterDescription + ": Efficiency mismatch.");
    }

    /** Makes sure the efficiency stays equal during the item duration. */
    @Test
    void efficiencyRemainsConstantWithinItemDuration(){

        // Burn a single coal and test for the duration
        entity.items.add(Items.coal, 1);
        entity.cons.update(tile.entity);
        generator.update(tile);

        float expectedEfficiency = entity.productionEfficiency;

        float currentDuration = 0.0f;
        while((currentDuration += FakeThreadHandler.fakeDelta) <= fakeItemDuration){
            generator.update(tile);
            assertEquals(expectedEfficiency, entity.productionEfficiency, "Duration: " + String.valueOf(currentDuration));
        }
        generator.update(tile);
        assertEquals(0.0f, entity.productionEfficiency, "Duration: " + String.valueOf(currentDuration));
    }
}
