package mindustry.world.blocks.liquid;

import arc.graphics.g2d.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

//TODO should leak!
public class LiquidJunction extends LiquidBlock{

    public LiquidJunction(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.liquidCapacity);
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region};
    }

    public class LiquidJunctionBuild extends Building{
        @Override
        public void draw(){
            Draw.rect(region, x, y);
        }

        @Override
        public Building getLiquidDestination(Building source, Liquid liquid){
            if(!enabled) return this;

            int dir = source.relativeTo(tile.x, tile.y);
            dir = (dir + 4) % 4;
            Building next = nearby(dir);
            if(next == null || (!next.acceptLiquid(this, liquid) && !(next.block instanceof LiquidJunction))){
                return this;
            }
            return next.getLiquidDestination(this, liquid);
        }
    }


}
