package mindustry.world.blocks.sandbox;

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

    @Override
    public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
        return true;
    }

    @Override
    public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){}

}
