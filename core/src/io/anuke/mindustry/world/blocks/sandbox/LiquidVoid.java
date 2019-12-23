package io.anuke.mindustry.world.blocks.sandbox;

import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.Block;

public class LiquidVoid extends Block{

    public LiquidVoid(String name){
        super(name);
        hasLiquids = true;
        solid = true;
        update = true;
    }

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return true;
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){}

}
