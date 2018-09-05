package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.*;

public class UnitBlocks extends BlockList implements ContentList{
    public static Block repairPoint, dronePad,
    fabricatorPad, interceptorPad, monsoonPad, daggerPad, titanPad,
    dropPoint, reconstructor, overdriveProjector, shieldProjector, commandCenter;

    @Override
    public void load(){
        dronePad = new UnitPad("drone-pad"){{
            type = UnitTypes.drone;
            produceTime = 5700;
            size = 2;
            consumes.power(0.08f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30)});
        }};

        fabricatorPad = new UnitPad("fabricator-pad"){{
            type = UnitTypes.fabricator;
            produceTime = 7300;
            size = 2;
            consumes.power(0.2f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 70), new ItemStack(Items.lead, 80), new ItemStack(Items.titanium, 80)});
        }};

        interceptorPad = new UnitPad("interceptor-pad"){{
            type = UnitTypes.interceptor;
            produceTime = 1800;
            size = 2;
            consumes.power(0.1f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 10), new ItemStack(Items.titanium, 10)});
        }};

        monsoonPad = new UnitPad("monsoon-pad"){{
            type = UnitTypes.monsoon;
            produceTime = 3600;
            size = 3;
            consumes.power(0.2f);
            shadow = "shadow-round-3";
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 30), new ItemStack(Items.titanium, 30), new ItemStack(Items.plastanium, 20)});
        }};

        daggerPad = new UnitPad("dagger-pad"){{
            type = UnitTypes.dagger;
            produceTime = 1700;
            size = 2;
            consumes.power(0.05f);
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 10), new ItemStack(Items.copper, 10)});
        }};

        titanPad = new UnitPad("titan-pad"){{
            type = UnitTypes.titan;
            produceTime = 3400;
            size = 3;
            consumes.power(0.15f);
            shadow = "shadow-round-3";
            consumes.items(new ItemStack[]{new ItemStack(Items.silicon, 20), new ItemStack(Items.thorium, 30)});
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
