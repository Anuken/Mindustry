package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.units.RepairPoint;
import io.anuke.mindustry.world.blocks.types.units.ResupplyPoint;
import io.anuke.mindustry.world.blocks.types.units.UnitFactory;

public class UnitBlocks {
    public static final Block

    droneFactory = new UnitFactory("dronefactory"){{
        type = UnitTypes.drone;
        produceTime = 200;
        size = 2;
        requirements = new ItemStack[]{
            new ItemStack(Items.stone, 5)
        };
    }},

    vtolFactory = new UnitFactory("vtolfactory"){{
        type = UnitTypes.vtol;
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
    }},

    resupplyPoint = new ResupplyPoint("resupplypoint"){{
        shadow = "shadow-round-1";
        itemCapacity = 30;
    }},

    repairPoint = new RepairPoint("repairpoint"){{
        shadow = "shadow-round-1";
        repairSpeed = 0.1f;
    }};
}
