package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.DropPoint;
import io.anuke.mindustry.world.blocks.units.RepairPoint;
import io.anuke.mindustry.world.blocks.units.ResupplyPoint;
import io.anuke.mindustry.world.blocks.units.UnitFactory;

public class UnitBlocks extends BlockList implements ContentList {
    public static Block resupplyPoint, repairPoint, droneFactory, dropPoint;

    @Override
    public void load() {
        droneFactory = new UnitFactory("dronefactory") {{
            type = UnitTypes.drone;
            produceTime = 300;
            size = 2;
            requirements = new ItemStack[]{
                new ItemStack(Items.iron, 20)
            };
        }};

        resupplyPoint = new ResupplyPoint("resupplypoint") {{
            shadow = "shadow-round-1";
            itemCapacity = 30;
        }};

        dropPoint = new DropPoint("droppoint") {{
            shadow = "shadow-round-1";
            itemCapacity = 40;
        }};

        repairPoint = new RepairPoint("repairpoint") {{
            shadow = "shadow-round-1";
            repairSpeed = 0.1f;
        }};
    }
}
