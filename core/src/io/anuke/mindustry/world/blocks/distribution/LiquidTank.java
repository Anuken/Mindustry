package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;

public class LiquidTank extends LiquidRouter{

    public LiquidTank(String name){
        super(name);
    }

    @Override
    public boolean canDumpLiquid(Tile tile, Tile to, Liquid liquid){
        return super.canDumpLiquid(tile, to, liquid) && !(to.block() instanceof LiquidTank);
    }
}
