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

        if(tile.entity.getLiquids().total() > 0.01f){
            tryDumpLiquid(tile, tile.entity.getLiquids().current());
        }
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return tile.entity.getLiquids().get(liquid) + amount < liquidCapacity && (tile.entity.getLiquids().current() == liquid || tile.entity.getLiquids().get(tile.entity.getLiquids().current()) < 0.2f);
    }
}
