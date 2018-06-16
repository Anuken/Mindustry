package io.anuke.mindustry.world.blocks.distribution;

import io.anuke.mindustry.world.Block;

public class MassDriver extends Block {
    protected float range;

    public MassDriver(String name) {
        super(name);
        update = true;
        solid = true;
    }
}
