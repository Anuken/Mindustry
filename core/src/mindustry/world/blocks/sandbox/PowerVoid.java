package mindustry.world.blocks.sandbox;

import mindustry.world.*;
import mindustry.world.blocks.PowerBlock;
import mindustry.world.meta.BlockStat;

public class PowerVoid extends PowerBlock{

    public PowerVoid(String name){
        super(name);
        consumes.power(Float.MAX_VALUE);
        stopOnDisabled = true;
    }

    @Override
    public void init(){
        super.init();
        stats.remove(BlockStat.powerUse);
    }

    @Override
    public boolean shouldConsume(Tile tile){
        return tile.entity.enabled();
    }
}
