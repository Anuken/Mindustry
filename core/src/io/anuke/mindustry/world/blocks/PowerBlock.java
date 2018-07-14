package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockGroup;

public abstract class PowerBlock extends Block{

    public PowerBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        group = BlockGroup.power;
    }
}
