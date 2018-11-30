package power;

import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.blocks.PowerBlocks;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.BurnerGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sets expectations to specific production blocks using specific inputs. */

public class PowerBalancingTests extends PowerTestFixture{
// Last updated to values of: v63

    /**
     * Tests the produced power of a power block with items or liquids.
     * @apiNote Tests only a single tick with a fixed delta and interpolates that to match one second.
     * @param powerBlock The block to be tested.
     * @param inputItem The item to be supplied (may be null).
     * @param inputLiquid The liquid to be supplied (may be null).
     * @param expectedPowerPerSecond The amount of power which should be produced per second.
     */
    public void testPowerGenerator(Block powerBlock, Item inputItem, Liquid inputLiquid, float expectedPowerPerSecond){
        Tile fakeTile = createFakeTile(0, 0, powerBlock);
        if(inputItem != null){
            fakeTile.entity.items.add(inputItem, 1);
        }
        if(inputLiquid != null){
            fakeTile.entity.liquids.add(inputLiquid, 1);
        }
        fakeTile.entity.cons.update(fakeTile.entity);
        powerBlock.update(fakeTile);

        assertEquals(expectedPowerPerSecond, fakeTile.entity.power.graph.getPowerProduced() * 60f, MathUtils.FLOAT_ROUNDING_ERROR * 10.0f);
    }

    @Test
    public void testCombustionWithCoal(){
        testPowerGenerator(PowerBlocks.combustionGenerator, Items.coal, null, 2.7f); // 100% flammability
    }

    @Test
    public void testCombustionWithOil(){
        testPowerGenerator(PowerBlocks.combustionGenerator, null, Liquids.oil, 2.7f * 1.2f); // 120% flammability
    }

    @Test
    public void testCombustionWithBlastCompound(){
        testPowerGenerator(PowerBlocks.combustionGenerator, Items.blastCompound, null, 2.7f * 0.4f); // 40% flammability
    }

    @Test
    public void testTurbineWithCoal(){
        testPowerGenerator(PowerBlocks.turbineGenerator, Items.coal, Liquids.water, 8.4f); // 100% flammability
    }

    @Test
    public void testTurbineWithBiomatter(){
        testPowerGenerator(PowerBlocks.turbineGenerator, Items.biomatter, Liquids.water, 8.4f * 0.8f); // 100% flammability
    }

    @Test
    public void testThermalWithLava(){
        testPowerGenerator(PowerBlocks.thermalGenerator, null, Liquids.lava, 36f); // 100% flammability
    }
}
