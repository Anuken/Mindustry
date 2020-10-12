package mindustry.world.blocks.sandbox;

import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

public class PowerVoid extends PowerBlock{

    public PowerVoid(String name){
        super(name);
        consumes.power(Float.MAX_VALUE);
    }

    @Override
    public void init(){
        super.init();
        stats.remove(BlockStat.powerUse);
    }
}
