package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.mindustry.world.blocks.types.power.PowerDistributor;

public class DebugBlocks {
    public static final Block

    powerVoid = new PowerBlock("powervoid") {
        {
            powerCapacity = Float.MAX_VALUE;
        }
    },

    powerInfinite = new PowerDistributor("powerinfinite") {
        {
            powerCapacity = 10000f;
        }

        @Override
        public void update(Tile tile){
            super.update(tile);
            tile.entity.power.amount = powerCapacity;
        }
    };
}
