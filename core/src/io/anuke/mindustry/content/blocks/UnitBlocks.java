package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.production.UnitFactory;

public class UnitBlocks {
    public static final Block

    flierFactory = new UnitFactory("flierfactory"){{
        type = UnitTypes.flier;
    }};
}
