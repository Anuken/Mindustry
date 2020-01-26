package mindustry.maps.generators;

import arc.struct.StringMap;
import mindustry.content.Blocks;
import mindustry.maps.Map;
import mindustry.world.Block;
import mindustry.world.Tile;

import static mindustry.Vars.world;

public abstract class RandomGenerator extends Generator{
    protected Block floor;
    protected Block block;
    protected Block ore;

    public RandomGenerator(int width, int height){
        super(width, height);
    }

    @Override
    public void generate(Tile[][] tiles){
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                floor = Blocks.air;
                block = Blocks.air;
                ore = Blocks.air;
                generate(x, y);
                tiles[x][y] = new Tile(x, y, floor.id, ore.id, block.id);
            }
        }

        decorate(tiles);

        world.setMap(new Map(new StringMap()));
    }

    public abstract void decorate(Tile[][] tiles);

    /**
     * Sets {@link #floor} and {@link #block} to the correct values as output.
     * Before this method is called, both are set to {@link Blocks#air} as defaults.
     */
    public abstract void generate(int x, int y);
}
