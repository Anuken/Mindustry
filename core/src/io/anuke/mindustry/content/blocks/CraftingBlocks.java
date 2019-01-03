package io.anuke.mindustry.content.blocks;

import io.anuke.arc.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.production.*;

public class CraftingBlocks extends BlockList implements ContentList{
    public static Block arcsmelter, siliconsmelter, plastaniumCompressor, phaseWeaver, alloySmelter,
            pyratiteMixer, blastMixer,
            cryofluidmixer, melter, separator, biomatterCompressor, pulverizer, incinerator;

    @Override
    public void load(){

        siliconsmelter = new PowerSmelter("silicon-smelter"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.silicon;
            craftTime = 40f;
            powerCapacity = 20f;
            size = 2;
            hasLiquids = false;
            flameColor = Color.valueOf("ffef99");

            consumes.items(new ItemStack(Items.sand, 2));
            consumes.power(0.05f);
        }};

        plastaniumCompressor = new PlastaniumCompressor("plastanium-compressor"){{
            hasItems = true;
            liquidCapacity = 60f;
            craftTime = 60f;
            output = Items.plastanium;
            powerCapacity = 40f;
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = BlockFx.formsmoke;
            updateEffect = BlockFx.plasticburn;

            consumes.liquid(Liquids.oil, 0.25f);
            consumes.power(0.3f);
            consumes.item(Items.titanium, 2);
        }};

        phaseWeaver = new PhaseWeaver("phase-weaver"){{
            craftEffect = BlockFx.smeltsmoke;
            result = Items.phasefabric;
            craftTime = 120f;
            powerCapacity = 50f;
            size = 2;

            consumes.items(new ItemStack(Items.thorium, 4), new ItemStack(Items.sand, 10));
            consumes.power(0.5f);
        }};

        alloySmelter = new PowerSmelter("alloy-smelter"){{
            craftEffect = BlockFx.smeltsmoke;
            result = Items.surgealloy;
            craftTime = 75f;
            powerCapacity = 60f;
            size = 2;

            useFlux = true;
            fluxNeeded = 3;

            consumes.power(0.4f);
            consumes.items(new ItemStack(Items.titanium, 2), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.copper, 3));
        }};

        cryofluidmixer = new LiquidMixer("cryofluidmixer"){{
            outputLiquid = Liquids.cryofluid;
            liquidPerItem = 50f;
            size = 2;
            hasPower = true;

            consumes.power(0.1f);
            consumes.item(Items.titanium);
            consumes.liquid(Liquids.water, 0.3f);
        }};

        blastMixer = new GenericCrafter("blast-mixer"){{
            hasItems = true;
            hasPower = true;
            hasLiquids = true;
            output = Items.blastCompound;
            size = 2;

            consumes.liquid(Liquids.oil, 0.05f);
            consumes.item(Items.pyratite, 1);
            consumes.power(0.04f);
        }};

        pyratiteMixer = new PowerSmelter("pyratite-mixer"){{
            flameColor = Color.CLEAR;
            hasItems = true;
            hasPower = true;
            result = Items.pyratite;

            size = 2;

            consumes.power(0.02f);
            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.lead, 1), new ItemStack(Items.sand, 1));
        }};

        melter = new PowerCrafter("melter"){{
            health = 200;
            outputLiquid = Liquids.slag;
            outputLiquidAmount = 1f;
            craftTime = 10f;
            size = 2;
            hasLiquids = hasPower = true;

            consumes.power(0.1f);
            consumes.item(Items.scrap, 1);
        }};

        separator = new Separator("separator"){{
            results = new ItemStack[]{
                new ItemStack(Items.copper, 5),
                new ItemStack(Items.lead, 3),
                new ItemStack(Items.titanium, 2),
                new ItemStack(Items.thorium, 1)
            };

            hasPower = true;
            filterTime = 15f;
            health = 50 * 4;
            spinnerLength = 1.5f;
            spinnerRadius = 3.5f;
            spinnerThickness = 1.5f;
            spinnerSpeed = 3f;
            size = 2;

            consumes.power(0.1f);
            consumes.liquid(Liquids.slag, 0.2f);
        }};

        biomatterCompressor = new Compressor("biomattercompressor"){{
            liquidCapacity = 60f;
            craftTime = 20f;
            outputLiquid = Liquids.oil;
            outputLiquidAmount = 2.5f;
            size = 2;
            health = 320;
            hasLiquids = true;

            consumes.item(Items.biomatter, 1);
            consumes.power(0.06f);
        }};

        pulverizer = new Pulverizer("pulverizer"){{
            output = Items.sand;
            craftEffect = BlockFx.pulverize;
            craftTime = 40f;
            updateEffect = BlockFx.pulverizeSmall;
            hasItems = hasPower = true;

            consumes.item(Items.scrap, 1);
            consumes.power(0.05f);
        }};

        incinerator = new Incinerator("incinerator"){{
            health = 90;
        }};
    }
}
