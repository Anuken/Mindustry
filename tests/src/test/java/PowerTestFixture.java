import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.power.Battery;
import io.anuke.mindustry.world.blocks.power.PowerGenerator;
import io.anuke.mindustry.world.modules.PowerModule;

import java.lang.reflect.Field;

/** This class provides objects commonly used by power related unit tests.
 *  For now, this is a helper with static methods, but this might change.
 * */
public class PowerTestFixture{

    protected static PowerGenerator createFakeProducerBlock(float producedPower){
        return new PowerGenerator("fakegen"){{
            powerProduction = producedPower;
        }};
    }

    protected static Battery createFakeBattery(float capacity, float ticksToFill){
        return new Battery("fakebattery"){{
            consumes.powerBuffered(capacity, ticksToFill);
        }};
    }

    protected static Block createFakeDirectConsumer(float powerPerTick, float minimumSatisfaction){
        return new Block("fakedirectconsumer"){{
            consumes.powerDirect(powerPerTick, minimumSatisfaction);
        }};
    }

    protected static Block createFakeBufferedConsumer(float capacity, float ticksToFill){
        return new Block("fakebufferedconsumer"){{
            consumes.powerBuffered(capacity, ticksToFill);
        }};
    }
    /**
     * Creates a fake tile on the given location using the given block.
     * @param x     The X coordinate.
     * @param y     The y coordinate.
     * @param block The block on the tile.
     * @return The created tile or null in case of exceptions.
     */
    protected static Tile createFakeTile(int x, int y, Block block){
        try{
            Tile tile = new Tile(x, y);

            Field field = Tile.class.getDeclaredField("wall");
            field.setAccessible(true);
            field.set(tile, block);

            field = Tile.class.getDeclaredField("floor");
            field.setAccessible(true);
            field.set(tile, Blocks.sand);

            tile.entity = block.newEntity();
            tile.entity.power = new PowerModule();
            return tile;
        }catch(Exception ex){
            return null;
        }
    }
}
