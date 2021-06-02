package mindustry.world.blocks.power;

import mindustry.world.*;
import mindustry.world.meta.*;

public class PowerBlock extends Block{

    public PowerBlock(String name){
        super(name);
        update = true;
        solid = true;
        hasPower = true;
        group = BlockGroup.power;
    }
}
