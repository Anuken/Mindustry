package io.anuke.mindustry.world.blocks.units;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.util.EnumSet;

public class CommandCenter extends Block{

    public CommandCenter(String name){
        super(name);

        flags = EnumSet.of(BlockFlag.comandCenter);
        destructible = true;
        solid = true;
        configurable = true;
    }


}
