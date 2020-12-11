package mindustry.world.blocks.liquid;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

public class LiquidRouter extends LiquidBlock{

    public LiquidRouter(String name){
        super(name);

        noUpdateDisabled = true;
    }

    public class LiquidRouterBuild extends LiquidBuild{
        @Override
        public void updateTile(){
            if(liquids.total() > 0.01f){
                dumpLiquid(liquids.current());
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
    }

    @Override
    public boolean canReplace(Block other){
        if(other.alwaysReplace) return true;
        return (other != this) && this.group != BlockGroup.none && other.group == this.group && other != this && other instanceof Conduit;
    }
}
