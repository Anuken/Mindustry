package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.production.UnitFactory;

public class UnitBlocks {
    public static final Block

    flierFactory = new UnitFactory("flierfactory"){{
        type = UnitTypes.flier;
        produceTime = 200;
        size = 2;
        requirements = new ItemStack[]{
            new ItemStack(Items.stone, 5)
        };
    }},

    walkerFactory = new UnitFactory("walkerfactory"){{
        type = UnitTypes.scout;
        produceTime = 20;
        size = 2;
        requirements = new ItemStack[]{
            new ItemStack(Items.stone, 1)
        };
    }};
}
