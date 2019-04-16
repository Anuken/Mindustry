package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.world.blocks.PowerBlock;

public class PowerDistributor extends PowerBlock{

    public PowerDistributor(String name){
        super(name);
        consumesPower = false;
        outputsPower = true;
    }
}
