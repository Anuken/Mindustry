package power;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.PowerGenerator;
import io.anuke.mindustry.world.blocks.power.PowerGraph;
import io.anuke.mindustry.world.blocks.units.UnitFactory;
import org.junit.jupiter.api.Test;
import sun.nio.cs.Surrogate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests for direct power consumers. */
public class DirectConsumerTests extends PowerTestFixture{

    @Test
    void noPowerRequestedWithNoItems(){
        testUnitFactory(0, 0, 0.08f, 0.08f, 0.0f);
    }

    @Test
    void noPowerRequestedWithInsufficientItems(){
        testUnitFactory(30, 0, 0.08f, 0.08f, 0.0f);
        testUnitFactory(0, 30, 0.08f, 0.08f, 0.0f);
    }

    @Test
    void powerRequestedWithSufficientItems(){
        testUnitFactory(30, 30, 0.08f, 0.08f, 1.0f);
    }

    void testUnitFactory(int siliconAmount, int leadAmount, float producedPower, float requestedPower, float expectedSatisfaction){
        Tile consumerTile = createFakeTile(0, 0, new UnitFactory("fakefactory"){{
            type = UnitTypes.spirit;
            produceTime = 60;
            consumes.powerDirect(requestedPower);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30));
        }});
        consumerTile.entity.items.add(Items.silicon, siliconAmount);
        consumerTile.entity.items.add(Items.lead, leadAmount);

        Tile producerTile = createFakeTile(2, 0, createFakeProducerBlock(producedPower));
        producerTile.<PowerGenerator.GeneratorEntity>entity().productionEfficiency = 0.5f; // 100%

        PowerGraph graph = new PowerGraph();
        graph.add(producerTile);
        graph.add(consumerTile);

        consumerTile.entity.update();
        graph.update();

        assertEquals(expectedSatisfaction, consumerTile.entity.power.satisfaction);
    }
}
