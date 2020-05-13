package mindustry.world.blocks.liquid;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

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
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name)};
    }

    public class LiquidJunctionEntity extends TileEntity{
        @Override
        public void draw(){
            Draw.rect(region, x, y);
        }

        @Override
        public Tilec getLiquidDestination(Tilec source, Liquid liquid){
            int dir = source.relativeTo(tile.x, tile.y);
            dir = (dir + 4) % 4;
            Tilec next = nearby(dir);
            if(next == null || (!next.acceptLiquid(this, liquid, 0f) && !(next.block() instanceof LiquidJunction))){
                return this;
            }
            return next.getLiquidDestination(this, liquid);
        }
    }


}
