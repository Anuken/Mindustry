package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;

public class Conduit extends LiquidBlock {

    public Conduit(String name) {
        super(name);
    }

    @Override
    public boolean canReplace(Block other) {
        return other instanceof Conduit && other != this;
    }
}
