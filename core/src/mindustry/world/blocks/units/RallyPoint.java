package mindustry.world.blocks.units;

import arc.struct.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class RallyPoint extends Block{

    public RallyPoint(String name){
        super(name);
        update = solid = true;
        flags = EnumSet.of(BlockFlag.rally);
    }
}
