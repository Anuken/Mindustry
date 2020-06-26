package mindustry.world.blocks.sandbox;

import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

public class LiquidVoid extends Block{

    public LiquidVoid(String name){
        super(name);
        hasLiquids = true;
        solid = true;
        update = true;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("liquid");
    }

    public class LiquidVoidEntity extends Building{
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid, float amount){
            return true;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
        }
    }

}
