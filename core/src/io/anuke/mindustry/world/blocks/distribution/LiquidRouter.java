package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;

public class LiquidRouter extends LiquidBlock{

    public LiquidRouter(String name){
        super(name);
    }

    @Override
    public void update(Tile tile){

        if(tile.entity.liquids.total() > 0.01f){
            tryDumpLiquid(tile, tile.entity.liquids.current());
        }
    }

}
