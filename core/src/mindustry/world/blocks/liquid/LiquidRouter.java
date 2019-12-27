package mindustry.world.blocks.liquid;

import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.blocks.LiquidBlock;

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

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return tile.entity.liquids.get(liquid) + amount < liquidCapacity && (tile.entity.liquids.current() == liquid || tile.entity.liquids.get(tile.entity.liquids.current()) < 0.2f);
    }
}
