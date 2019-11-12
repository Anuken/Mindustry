package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;

public class LiquidJunction extends LiquidBlock{

    public LiquidJunction(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(BlockStat.liquidCapacity);
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("liquid");
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(name, tile.worldx(), tile.worldy());
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name)};
    }

    @Override
    public Tile getLiquidDestination(Tile tile, Tile source){
        int dir = source.relativeTo(tile.x, tile.y);
        dir = (dir + 4) % 4;
        Tile next = tile.getNearby(dir).link();
        return next.block().getLiquidDestination(next, tile);
    }
}
