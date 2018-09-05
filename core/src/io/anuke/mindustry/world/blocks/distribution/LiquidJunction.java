package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.LiquidBlock;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.graphics.Draw;

public class LiquidJunction extends LiquidBlock{

    public LiquidJunction(String name){
        super(name);
        hasLiquids = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove(BarType.liquid);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(BlockStat.liquidCapacity);
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(name(), tile.worldx(), tile.worldy());
    }

    @Override
    public TextureRegion[] getIcon(){
        return new TextureRegion[]{Draw.region(name)};
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        int dir = source.relativeTo(tile.x, tile.y);
        dir = (dir + 4) % 4;
        Tile to = tile.getNearby(dir).target();

        if(to.block().hasLiquids && to.block().acceptLiquid(to, tile, liquid, Math.min(to.block().liquidCapacity - to.entity.liquids.get(liquid) - 0.00001f, amount))){
            to.block().handleLiquid(to, tile, liquid, Math.min(to.block().liquidCapacity - to.entity.liquids.get(liquid) - 0.00001f, amount));
        }
    }

    @Override
    public boolean acceptLiquid(Tile dest, Tile source, Liquid liquid, float amount){
        int dir = source.relativeTo(dest.x, dest.y);
        dir = (dir + 4) % 4;
        Tile to = dest.getNearby(dir);
        if(to == null) return false;
        to = to.target();
        return to != null && to.entity != null && to.block().hasLiquids && to.block().acceptLiquid(to, dest, liquid, Math.min(to.block().liquidCapacity - to.entity.liquids.get(liquid) - 0.00001f, amount));
    }
}
