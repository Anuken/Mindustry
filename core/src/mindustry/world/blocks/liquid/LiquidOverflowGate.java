package mindustry.world.blocks.liquid;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

//TODO implement later
public class LiquidOverflowGate extends LiquidBlock{
    public int topRegion;

    public LiquidOverflowGate(String name){
        super(name);
        rotate = true;
        topRegion = reg("-top");
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
        Draw.rect(name, tile.drawx(), tile.drawy());
        Draw.rect(reg(topRegion), tile.drawx(), tile.drawy(), tile.rotation() * 90);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-top")};
    }

    @Override
    public Tile getLiquidDestination(Tile tile, Tile source, Liquid liquid){
        int dir = source.relativeTo(tile.x, tile.y);
        dir = (dir + 4) % 4;
        Tile next = tile.getNearby(dir).link();
        if(!next.block().acceptLiquid(next, tile, liquid, 0.0001f) && !(next.block() instanceof LiquidOverflowGate || next.block() instanceof LiquidJunction)){
            return tile;
        }
        return next.block().getLiquidDestination(next, tile, liquid);
    }
}
