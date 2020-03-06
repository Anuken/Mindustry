package mindustry.world.blocks.sandbox;

import mindustry.world.blocks.power.PowerBlock;
import mindustry.world.meta.BlockStat;

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
