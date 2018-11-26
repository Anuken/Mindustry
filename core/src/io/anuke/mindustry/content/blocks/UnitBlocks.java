package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.units.*;

public class UnitBlocks extends BlockList implements ContentList{
    public static Block
        spiritFactory, phantomFactory,
        wraithFactory, ghoulFactory, revenantFactory,
        daggerFactory, titanFactory, fortressFactory,
        reconstructor, repairPoint, commandCenter;

    @Override
    public void load(){
        spiritFactory = new UnitFactory("spirit-factory"){{
            type = UnitTypes.spirit;
            produceTime = 5700;
            size = 2;
            consumes.powerDirect(0.08f);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30));
        }};

        phantomFactory = new UnitFactory("phantom-factory"){{
            type = UnitTypes.phantom;
            produceTime = 7300;
            size = 2;
            consumes.powerDirect(0.2f);
            consumes.items(new ItemStack(Items.silicon, 70), new ItemStack(Items.lead, 80), new ItemStack(Items.titanium, 80));
        }};

        wraithFactory = new UnitFactory("wraith-factory"){{
            type = UnitTypes.wraith;
            produceTime = 1800;
            size = 2;
            consumes.powerDirect(0.1f);
            consumes.items(new ItemStack(Items.silicon, 10), new ItemStack(Items.titanium, 10));
        }};

        ghoulFactory = new UnitFactory("ghoul-factory"){{
            type = UnitTypes.ghoul;
            produceTime = 3600;
            size = 3;
            consumes.powerDirect(0.2f);
            shadow = "shadow-round-3";
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.titanium, 30), new ItemStack(Items.plastanium, 20));
        }};

        revenantFactory = new UnitFactory("revenant-factory"){{
            type = UnitTypes.revenant;
            produceTime = 8000;
            size = 4;
            consumes.powerDirect(0.3f);
            shadow = "shadow-round-4";
            consumes.items(new ItemStack(Items.silicon, 80), new ItemStack(Items.titanium, 80), new ItemStack(Items.plastanium, 50));
        }};

        daggerFactory = new UnitFactory("dagger-factory"){{
            type = UnitTypes.dagger;
            produceTime = 1700;
            size = 2;
            consumes.powerDirect(0.05f);
            consumes.items(new ItemStack(Items.silicon, 10));
        }};

        titanFactory = new UnitFactory("titan-factory"){{
            type = UnitTypes.titan;
            produceTime = 3400;
            size = 3;
            consumes.powerDirect(0.15f);
            shadow = "shadow-round-3";
            consumes.items(new ItemStack(Items.silicon, 20), new ItemStack(Items.thorium, 30));
        }};

        fortressFactory = new UnitFactory("fortress-factory"){{
            type = UnitTypes.fortress;
            produceTime = 5000;
            size = 3;
            consumes.powerDirect(0.2f);
            shadow = "shadow-round-3";
            consumes.items(new ItemStack(Items.silicon, 40), new ItemStack(Items.thorium, 50));
        }};

        repairPoint = new RepairPoint("repair-point"){{
            shadow = "shadow-round-1";
            repairSpeed = 0.1f;
        }};

        reconstructor = new Reconstructor("reconstructor"){{
            size = 2;
        }};

        commandCenter = new CommandCenter("command-center"){{
            size = 2;
        }};
    }
}
