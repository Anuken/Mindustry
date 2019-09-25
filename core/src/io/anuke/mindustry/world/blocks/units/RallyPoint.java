package io.anuke.mindustry.world.blocks.units;

import io.anuke.arc.collection.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.meta.*;

public class RallyPoint extends Block{

    public RallyPoint(String name){
        super(name);
        update = solid = true;
        flags = EnumSet.of(BlockFlag.rally);
    }
}
