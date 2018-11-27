package power;

import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.ItemLiquidGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** This class tests the abstract ItemLiquidGenerator class and maybe some of its dependencies. */
public class ItemLiquidGeneratorTests extends PowerTestFixture{

    private ItemLiquidGenerator sut; // system under test (https://en.wikipedia.org/wiki/System_under_test)
    private Tile tile;

    @BeforeEach
    public void createItemLiquidGenerator(){
        sut = new ItemLiquidGenerator("fakegen"){
            @Override
            protected float getLiquidEfficiency(Liquid liquid){
                return liquid.flammability;
            }

            @Override
            protected float getItemEfficiency(Item item){
                return item.flammability;
            }
        };
        tile = createFakeTile(0, 0, sut);
    }

    @Test
    void detectCrashes(){
        sut.update(tile);
    }
}
