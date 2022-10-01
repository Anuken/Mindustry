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
        Core.files = new MockFiles();
        Groups.init();

        boolean make = content == null;

        if(make){
            Vars.content = new ContentLoader(){
                @Override
                public void handleMappableContent(MappableContent content){

                }
            };
        }
        Vars.state = new GameState();
        Vars.tree = new FileTree();
        if(make){
            content.createBaseContent();
        }
        Log.useColors = false;
        Time.setDeltaProvider(() -> 0.5f);
    }

    protected static PowerGenerator createFakeProducerBlock(float producedPower){
        return new PowerGenerator("fakegen" + System.nanoTime()){{
            buildType = () -> new GeneratorBuild();
            powerProduction = producedPower;
        }};
    }

    protected static Battery createFakeBattery(float capacity){
        return new Battery("fakebattery" + System.nanoTime()){{
            buildType = () -> new BatteryBuild();
            consumePowerBuffered(capacity);
        }};
    }

    protected static Block createFakeDirectConsumer(float powerPerTick){
        return new PowerBlock("fakedirectconsumer" + System.nanoTime()){{
            buildType = Building::create;
            consumePower(powerPerTick);
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
            if(block.consumers.length == 0){
                block.init();
            }

            // Using the Tile(int, int, byte, byte) constructor would require us to register any fake block or tile we create
            // Since this part shall not be part of the test and would require more work anyway, we manually set the block and floor
            // through reflections and then simulate part of what the changed() method does.

            Reflect.set(Tile.class, tile, "block", block);
            Reflect.set(Tile.class, tile, "floor", Blocks.sand);

            // Simulate the "changed" method. Calling it through reflections would require half the game to be initialized.
            tile.build = block.newBuilding().init(tile, Team.sharded, false, 0);
            if(block.hasPower){
                new PowerGraph().add(tile.build);
            }

            // Assign incredibly high health so the block does not get destroyed on e.g. burning Blast Compound
            block.health = 100000;
            tile.build.health = 100000.0f;

            return tile;
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
