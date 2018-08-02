package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.*;

public class UnitBlocks extends BlockList implements ContentList{
    public static Block resupplyPoint, repairPoint, dronePad,
    fabricatorPad, interceptorPad, monsoonPad, scoutPad, titanPad,
    dropPoint, reconstructor, overdriveProjector, shieldProjector, commandCenter;

    @Override
    public void load(){
        dronePad = new UnitPad("drone-pad"){{
            type = UnitTypes.drone;
            produceTime = 800;
            size = 2;
            consumes.power(0.08f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30)});
        }};

        fabricatorPad = new UnitPad("fabricator-pad"){{
            type = UnitTypes.fabricator;
            produceTime = 1600;
            size = 2;
            consumes.power(0.2f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 70), new ItemStack(Items.lead, 80), new ItemStack(Items.titanium, 80)});
        }};

        interceptorPad = new UnitPad("interceptor-pad"){{
            type = UnitTypes.interceptor;
            produceTime = 1300;
            size = 2;
            consumes.power(0.1f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 30), new ItemStack(Items.titanium, 40)});
        }};

        monsoonPad = new UnitPad("monsoon-pad"){{
            type = UnitTypes.monsoon;
            produceTime = 1400;
            size = 3;
            consumes.power(0.14f);
            shadow = "shadow-round-3";
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 40), new ItemStack(Items.titanium, 50), new ItemStack(Items.plastanium, 50)});
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
