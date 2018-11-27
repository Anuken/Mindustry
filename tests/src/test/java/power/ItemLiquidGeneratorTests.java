package power;

import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.BurnerGenerator;
import io.anuke.mindustry.world.blocks.power.ItemGenerator;
import io.anuke.mindustry.world.blocks.power.ItemLiquidGenerator;
import io.anuke.mindustry.world.blocks.power.PowerGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** This class tests the abstract ItemLiquidGenerator class and maybe some of its dependencies. */
public class ItemLiquidGeneratorTests extends PowerTestFixture{

    private ItemLiquidGenerator generator;
    private Tile tile;
    private ItemGenerator.ItemGeneratorEntity entity;
    private final float fakeLiquidPowerMultiplier = 2.0f;
    private final float fakeMaxLiquidGenerate = 0.5f;

    @BeforeEach
    public void createBurnerGenerator(){
        // Use a burner generator instead of a custom ItemLiquidGenerator subclass since we would implement abstract methods the same way.
        generator = new BurnerGenerator("fakegen"){{
            powerProduction = 0.1f;
            itemDuration = 60f;
            liquidPowerMultiplier = fakeLiquidPowerMultiplier;
            maxLiquidGenerate = fakeMaxLiquidGenerate;
        }};

        tile = createFakeTile(0, 0, generator);
        entity = tile.entity();
    }

    @Test
    void testLiquidConsumption(){
        final float providedUsage = 0.1f;
        final float expectedEfficiency = providedUsage / fakeMaxLiquidGenerate * fakeLiquidPowerMultiplier * Liquids.oil.flammability;

        entity.liquids.add(Liquids.oil, providedUsage);
        entity.cons.update(tile.entity);
        assumeTrue(entity.cons.valid());

        // Perform an update on the generator once - This should use up all oil and produce a fraction of what's possible
        generator.update(tile);

        assertEquals(0.0f, entity.liquids.get(Liquids.oil));
        assertEquals(expectedEfficiency, entity.productionEfficiency);
    }
}
