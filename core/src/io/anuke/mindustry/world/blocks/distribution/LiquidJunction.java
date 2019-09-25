package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.meta.BlockStat;

public class LiquidJunction extends LiquidBlock{

    public LiquidJunction(String name){
        super(name);
        hasLiquids = true;
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
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        int dir = source.relativeTo(tile.x, tile.y);
        dir = (dir + 4) % 4;
        Tile to = tile.getNearby(dir).link();

        if(to.block().hasLiquids && to.block().acceptLiquid(to, tile, liquid, amount)){
            to.block().handleLiquid(to, tile, liquid, amount);
        }
    }

    @Override
    public boolean acceptLiquid(Tile dest, Tile source, Liquid liquid, float amount){
        int dir = source.relativeTo(dest.x, dest.y);
        dir = (dir + 4) % 4;
        Tile to = dest.getNearby(dir);
        if(to == null) return false;
        to = to.link();
        return to != null && to.entity != null && to.block().hasLiquids && to.block().acceptLiquid(to, dest, liquid, amount);
    }
}
