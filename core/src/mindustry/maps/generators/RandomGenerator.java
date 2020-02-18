package mindustry.maps.generators;

import arc.struct.*;
import mindustry.content.*;
import mindustry.maps.*;
import mindustry.world.*;

import static mindustry.Vars.world;

public abstract class RandomGenerator extends Generator{
    protected Block floor;
    protected Block block;
    protected Block ore;

    public RandomGenerator(int width, int height){
        super(width, height);
    }

    @Override
    public void generate(Tiles tiles){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile prev = tiles.getn(x, y);
                floor = prev.floor();
                block = prev.block();
                ore = prev.overlay();
                generate(x, y);
                prev.setFloor(floor.asFloor());
                prev.setBlock(block);
                prev.setOverlay(ore);
            }
        }

        decorate(tiles);

        world.setMap(new Map(new StringMap()));
    }

    public abstract void decorate(Tiles tiles);

    /**
     * Sets {@link #floor} and {@link #block} to the correct values as output.
     * Before this method is called, both are set to {@link Blocks#air} as defaults.
     */
    public abstract void generate(int x, int y);
}
