package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.units.RepairPoint;
import io.anuke.mindustry.world.blocks.types.units.ResupplyPoint;

public class UnitBlocks implements ContentList {
    public static Block resupplyPoint, repairPoint, droneFactory;

    @Override
    public void load() {
        /*
        droneFactory = new UnitFactory("dronefactory") {{
            type = UnitTypes.drone;
            produceTime = 200;
            size = 2;
            requirements = new ItemStack[]{
                    new ItemStack(Items.stone, 5)
            };
        }};*/

        resupplyPoint = new ResupplyPoint("resupplypoint") {{
            shadow = "shadow-round-1";
            itemCapacity = 30;
        }};

        repairPoint = new RepairPoint("repairpoint") {{
            shadow = "shadow-round-1";
            repairSpeed = 0.1f;
        }};
    }
}
