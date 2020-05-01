package mindustry.world.blocks.power;

import mindustry.world.Block;
import mindustry.world.meta.BlockGroup;

public abstract class PowerBlock extends Block{

    public PowerBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        group = BlockGroup.power;
    }
}
