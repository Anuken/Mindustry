package io.anuke.mindustry.world.blocks;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.mindustry.world.blocks.types.distribution.PowerLaser;

public class DebugBlocks {
    public static final Block

    powerVoid = new PowerBlock("powervoid") {
        {
            powerCapacity = Float.MAX_VALUE;
        }
    },

    powerInfinite = new PowerLaser("powerinfinite") {
        {
            powerCapacity = 100f;
            laserDirections = 4;
        }

        @Override
        public void update(Tile tile){
            super.update(tile);
            tile.entity.power.amount = powerCapacity;
        }
    };
}
