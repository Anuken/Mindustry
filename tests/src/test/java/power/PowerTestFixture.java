package power;

import io.anuke.arc.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.power.*;
import io.anuke.mindustry.world.modules.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.*;

import static io.anuke.mindustry.Vars.content;

/**
 * This class provides objects commonly used by power related unit tests.
 * For now, this is a helper with static methods, but this might change.
 * <p>
 * Note: All tests which subclass this will run with a fixed delta of 0.5!
 */
public class PowerTestFixture{

    @BeforeAll
    static void initializeDependencies(){
        Core.graphics = new FakeGraphics();
        Vars.content = new ContentLoader(){
            @Override
            public void handleMappableContent(MappableContent content){

            }
        };
        content.createContent();
        Log.setUseColors(false);
        Time.setDeltaProvider(() -> 0.5f);
    }

    protected static PowerGenerator createFakeProducerBlock(float producedPower){
        return new PowerGenerator("fakegen"){{
            powerProduction = producedPower;
        }};
    }

    protected static Battery createFakeBattery(float capacity){
        return new Battery("fakebattery"){{
            consumes.powerBuffered(capacity);
        }};
    }

    protected static Block createFakeDirectConsumer(float powerPerTick){
        return new PowerBlock("fakedirectconsumer"){{
            consumes.power(powerPerTick);
        }};
    }

    /**
     * Creates a fake tile on the given location using the given block.
     * @param x The X coordinate.
     * @param y The y coordinate.
     * @param block The block on the tile.
     * @return The created tile or null in case of exceptions.
     */
    protected static Tile createFakeTile(int x, int y, Block block){
        try{
            Tile tile = new Tile(x, y);

            //workaround since init() is not called for custom blocks
            if(block.consumes.all() == null){
                block.consumes.init();
            }

            // Using the Tile(int, int, byte, byte) constructor would require us to register any fake block or tile we create
            // Since this part shall not be part of the test and would require more work anyway, we manually set the block and floor
            // through reflections and then simulate part of what the changed() method does.

            Field field = Tile.class.getDeclaredField("block");
            field.setAccessible(true);
            field.set(tile, block);

            field = Tile.class.getDeclaredField("floor");
            field.setAccessible(true);
            field.set(tile, Blocks.sand);

            // Simulate the "changed" method. Calling it through reflections would require half the game to be initialized.
            tile.entity = block.newEntity().init(tile, false);
            tile.entity.cons = new ConsumeModule(tile.entity);
            if(block.hasItems) tile.entity.items = new ItemModule();
            if(block.hasLiquids) tile.entity.liquids = new LiquidModule();
            if(block.hasPower){
                tile.entity.power = new PowerModule();
                tile.entity.power.graph.add(tile);
            }

            // Assign incredibly high health so the block does not get destroyed on e.g. burning Blast Compound
            block.health = 100000;
            tile.entity.health = 100000.0f;

            return tile;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
