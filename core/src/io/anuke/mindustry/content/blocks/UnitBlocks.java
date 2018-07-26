package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.*;

public class UnitBlocks extends BlockList implements ContentList{
    public static Block resupplyPoint, repairPoint, droneFactory, fabricatorFactory, interceptorFactory, dropPoint,
            reconstructor, overdriveProjector, shieldProjector, commandCenter;

    @Override
    public void load(){
        droneFactory = new UnitFactory("drone-factory"){{
            type = UnitTypes.drone;
            produceTime = 800;
            size = 2;
            consumes.power(0.08f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30)});
        }};

        fabricatorFactory = new UnitFactory("fabricator-factory"){{
            type = UnitTypes.fabricator;
            produceTime = 1600;
            size = 2;
            consumes.power(0.2f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 70), new ItemStack(Items.lead, 80), new ItemStack(Items.titanium, 80)});
        }};

        interceptorFactory = new UnitFactory("interceptor-factory"){{
            type = UnitTypes.interceptor;
            produceTime = 1300;
            size = 2;
            consumes.power(0.1f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 30), new ItemStack(Items.titanium, 40)});
        }};

        resupplyPoint = new ResupplyPoint("resupply-point"){{
            shadow = "shadow-round-1";
            itemCapacity = 30;
        }};

        dropPoint = new DropPoint("drop-point"){{
            shadow = "shadow-round-1";
            itemCapacity = 40;
        }};

        repairPoint = new RepairPoint("repair-point"){{
            shadow = "shadow-round-1";
            repairSpeed = 0.1f;
        }};

        reconstructor = new Reconstructor("reconstructor"){{
            size = 2;
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            size = 2;
        }};

        shieldProjector = new ShieldProjector("shield-projector"){{
            size = 2;
        }};

        commandCenter = new CommandCenter("command-center"){{
            size = 2;
        }};
    }
}
