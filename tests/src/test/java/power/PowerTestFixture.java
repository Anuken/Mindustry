package power;

import arc.*;
import arc.mock.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;

/**
 * This class provides objects commonly used by power related unit tests.
 * For now, this is a helper with static methods, but this might change.
 * <p>
 * Note: All tests which subclass this will run with a fixed delta of 0.5!
 */
public class PowerTestFixture{

    @BeforeAll
    static void initializeDependencies(){
        headless = true;
        Core.graphics = new FakeGraphics();
        Core.files = new MockFiles();
        Vars.state = new GameState();
        Vars.tree = new FileTree();
        Vars.content = new ContentLoader(){
            @Override
            public void handleMappableContent(MappableContent content){

            }
        };
        content.createBaseContent();
        Log.setUseColors(false);
        Time.setDeltaProvider(() -> 0.5f);
    }

    protected static PowerGenerator createFakeProducerBlock(float producedPower){
        return new PowerGenerator("fakegen"){{
            entityType = () -> new GeneratorEntity();
            powerProduction = producedPower;
        }};
    }

    protected static Battery createFakeBattery(float capacity){
        return new Battery("fakebattery"){{
            entityType = () -> new BatteryEntity();
            consumes.powerBuffered(capacity);
        }};
    }

    protected static Block createFakeDirectConsumer(float powerPerTick){
        return new PowerBlock("fakedirectconsumer"){{
            entityType = Building::create;
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

            Reflect.set(Tile.class, tile, "block", block);
            Reflect.set(Tile.class, tile, "floor", Blocks.sand);

            // Simulate the "changed" method. Calling it through reflections would require half the game to be initialized.
            tile.build = block.newEntity().init(tile, Team.sharded, false, 0);
            if(block.hasPower){
                tile.build.power.graph = new PowerGraph(){
                    //assume there's always something consuming power
                    @Override
                    public float getUsageFraction(){
                        return 1f;
                    }
                };
                tile.build.power.graph.add(tile.build);
            }

            // Assign incredibly high health so the block does not get destroyed on e.g. burning Blast Compound
            block.health = 100000;
            tile.build.health(100000.0f);

            return tile;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
