package power;

import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.power.ConsumeGenerator.*;
import mindustry.world.consumers.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.DynamicTest.*;

/**
 * This class tests generators which can process items, liquids or both.
 * All tests are run with a fixed delta of 0.5 so delta considerations can be tested as well.
 * Any expected power amount (produced, consumed, buffered) should be affected by fakeDelta but status should not!
 */
public class ConsumeGeneratorTests extends PowerTestFixture{
    private ConsumeGenerator generator;
    private Tile tile;
    private ConsumeGeneratorBuild build;
    private final float fakeItemDuration = 60f; //ticks
    private final float maximumLiquidUsage = 0.5f;

    public void createGenerator(InputType inputType){
        Vars.state = new GameState();
        Vars.state.rules = new Rules();
        generator = new ConsumeGenerator("fakegen" + System.nanoTime()){{
            powerProduction = 0.1f;
            itemDuration = fakeItemDuration;
            buildType = ConsumeGeneratorBuild::new;

            if(inputType != InputType.liquids){
                consume(new ConsumeItemFlammable());
            }

            if(inputType != InputType.items){
                consume(new ConsumeLiquidFlammable(maximumLiquidUsage));
            }
        }};

        generator.init();
        tile = createFakeTile(0, 0, generator);
        build = (ConsumeGeneratorBuild)tile.build;
    }

    /** Tests the consumption and efficiency when being supplied with liquids. */
    @TestFactory
    DynamicTest[] generatorWorksProperlyWithLiquidInput(){

        // Execute all tests for the case where only liquids are accepted and for the case where liquids and items are accepted (but supply only liquids)
        InputType[] inputTypesToBeTested = new InputType[]{
        InputType.liquids,
        //InputType.any
        };

        ArrayList<DynamicTest> tests = new ArrayList<>();
        for(InputType inputType : inputTypesToBeTested){
            tests.add(dynamicTest("01", () -> simulateLiquidConsumption(inputType, Liquids.oil, 0.0f, "No liquids provided")));
            tests.add(dynamicTest("02", () -> simulateLiquidConsumption(inputType, Liquids.oil, maximumLiquidUsage / 4.0f, "Low oil provided")));
            tests.add(dynamicTest("03", () -> simulateLiquidConsumption(inputType, Liquids.oil, maximumLiquidUsage * 1.0f, "Sufficient oil provided")));
            tests.add(dynamicTest("04", () -> simulateLiquidConsumption(inputType, Liquids.oil, maximumLiquidUsage * 2.0f, "Excess oil provided")));
            // Note: The generator will decline any other liquid since it's not flammable
        }
        DynamicTest[] testArray = new DynamicTest[tests.size()];
        testArray = tests.toArray(testArray);
        return testArray;
    }

    void simulateLiquidConsumption(InputType inputType, Liquid liquid, float availableLiquidAmount, String parameterDescription){
        final float baseEfficiency = liquid.flammability;
        final float expectedEfficiency = Math.min(1.0f, availableLiquidAmount / maximumLiquidUsage) * baseEfficiency;
        final float expectedConsumptionPerTick = Math.min(maximumLiquidUsage, availableLiquidAmount);
        final float expectedRemainingLiquidAmount = Math.max(0.0f, availableLiquidAmount - expectedConsumptionPerTick * Time.delta);

        createGenerator(inputType);
        assertTrue(build.acceptLiquid(null, liquid), inputType + " | " + parameterDescription + ": Liquids which will be declined by the generator don't need to be tested - The code won't be called for those cases.");

        build.liquids.add(liquid, availableLiquidAmount);
        build.updateConsumption();

        // Perform an update on the generator once - This should use up any resource up to the maximum liquid usage
        build.updateTile();

        assertEquals(expectedRemainingLiquidAmount, build.liquids.get(liquid), inputType + " | " + parameterDescription + ": Remaining liquid amount mismatch.");
        assertEquals(expectedEfficiency, build.productionEfficiency, inputType + " | " + parameterDescription + ": Efficiency mismatch.");
    }

    /** Tests the consumption and efficiency when being supplied with items. */
    @TestFactory
    DynamicTest[] generatorWorksProperlyWithItemInput(){

        // Execute all tests for the case where only items are accepted and for the case where liquids and items are accepted (but supply only items)
        InputType[] inputTypesToBeTested = new InputType[]{
        InputType.items,
        //InputType.any
        };

        ArrayList<DynamicTest> tests = new ArrayList<>();
        for(InputType inputType : inputTypesToBeTested){
            tests.add(dynamicTest("01", () -> simulateItemConsumption(inputType, Items.coal, 0, "No items provided")));
            tests.add(dynamicTest("02", () -> simulateItemConsumption(inputType, Items.coal, 1, "Sufficient coal provided")));
            tests.add(dynamicTest("03", () -> simulateItemConsumption(inputType, Items.coal, 10, "Excess coal provided")));
            tests.add(dynamicTest("04", () -> simulateItemConsumption(inputType, Items.blastCompound, 1, "Blast compound provided")));
            tests.add(dynamicTest("05", () -> simulateItemConsumption(inputType, Items.sporePod, 1, "Biomatter provided")));
            tests.add(dynamicTest("06", () -> simulateItemConsumption(inputType, Items.pyratite, 1, "Pyratite provided")));
        }
        DynamicTest[] testArray = new DynamicTest[tests.size()];
        testArray = tests.toArray(testArray);
        return testArray;
    }

    void simulateItemConsumption(InputType inputType, Item item, int amount, String parameterDescription){
        float expectedEfficiency = amount > 0 ? item.flammability : 0f;
        int expectedRemainingItemAmount = Math.max(0, amount - 1);

        createGenerator(inputType);
        assertTrue(build.acceptItem(null, item), inputType + " | " + parameterDescription + ": Items which will be declined by the generator don't need to be tested - The code won't be called for those cases.");

        if(amount > 0){
            build.items.add(item, amount);
        }

        // Perform an update on the generator once - This should use up one or zero items - dependent on if the item is accepted and available or not.
        build.update();

        assertEquals(expectedRemainingItemAmount, build.items.get(item), inputType + " | " + parameterDescription + ": Remaining item amount mismatch.");
        assertEquals(expectedEfficiency, build.productionEfficiency, inputType + " | " + parameterDescription + ": Efficiency mismatch.");

    }

    /** Makes sure the efficiency stays equal during the item duration. */
    @Test
    void efficiencyRemainsConstantWithinItemDuration_ItemsOnly(){
        testItemDuration(InputType.items);
    }

    /** Makes sure the efficiency stays equal during the item duration. */
    @Test
    void efficiencyRemainsConstantWithinItemDuration_ItemsAndLiquids(){
        //testItemDuration(InputType.any);
    }

    void testItemDuration(InputType inputType){
        createGenerator(inputType);

        // Burn a single coal and test for the duration
        build.items.add(Items.coal, 1);
        build.update();

        float expectedEfficiency = build.productionEfficiency;

        float currentDuration = 0.0f;
        while((currentDuration += Time.delta) <= fakeItemDuration){
            build.update();
            assertEquals(expectedEfficiency, build.productionEfficiency, "Duration: " + currentDuration);
        }
        build.update();
        assertEquals(0.0f, build.productionEfficiency, "Duration: " + currentDuration);
    }

    enum InputType{
        items,
        liquids
    }
}
