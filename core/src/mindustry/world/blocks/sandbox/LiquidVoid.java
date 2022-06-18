package mindustry.world.blocks.sandbox;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class LiquidVoid extends Block{

    public LiquidVoid(String name){
        super(name);
        hasLiquids = true;
        solid = true;
        update = true;
        group = BlockGroup.liquids;
        envEnabled = Env.any;
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");
    }

    public class LiquidVoidBuild extends Building{
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return enabled;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            liquids.handleFlow(liquid, amount);
        }
    }

}
