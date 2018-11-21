package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.production.*;

public class CraftingBlocks extends BlockList implements ContentList{
    public static Block smelter, arcsmelter, siliconsmelter, plastaniumCompressor, phaseWeaver, alloySmelter,
            pyratiteMixer, blastMixer,
            cryofluidmixer, melter, separator, centrifuge, biomatterCompressor, pulverizer, solidifier, incinerator;

    @Override
    public void load(){
        smelter = new Smelter("smelter"){{
            health = 70;
            result = Items.densealloy;
            craftTime = 45f;
            burnDuration = 46f;
            useFlux = true;

            consumes.items(new ItemStack[]{new ItemStack(Items.copper, 1), new ItemStack(Items.lead, 2)});
            consumes.item(Items.coal).optional(true);
        }};

        arcsmelter = new PowerSmelter("arc-smelter"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.densealloy;
            craftTime = 30f;
            size = 2;

            useFlux = true;
            fluxNeeded = 2;

            consumes.items(new ItemStack[]{new ItemStack(Items.copper, 1), new ItemStack(Items.lead, 2)});
            consumes.powerDirect(0.1f);
        }};

        siliconsmelter = new PowerSmelter("silicon-smelter"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.silicon;
            craftTime = 40f;
            size = 2;
            hasLiquids = false;
            flameColor = Color.valueOf("ffef99");

            consumes.items(new ItemStack[]{new ItemStack(Items.coal, 1), new ItemStack(Items.sand, 2)});
            consumes.powerDirect(0.05f);
        }};

        plastaniumCompressor = new PlastaniumCompressor("plastanium-compressor"){{
            hasItems = true;
            liquidCapacity = 60f;
            craftTime = 60f;
            output = Items.plastanium;
            itemCapacity = 30;
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = BlockFx.formsmoke;
            updateEffect = BlockFx.plasticburn;

            consumes.liquid(Liquids.oil, 0.25f);
            consumes.powerDirect(0.3f);
            consumes.item(Items.titanium, 2);
        }};

        phaseWeaver = new PhaseWeaver("phase-weaver"){{
            craftEffect = BlockFx.smeltsmoke;
            result = Items.phasefabric;
            craftTime = 120f;
            size = 2;

            consumes.items(new ItemStack[]{new ItemStack(Items.thorium, 4), new ItemStack(Items.sand, 10)});
            consumes.powerDirect(0.5f);
        }};

        alloySmelter = new PowerSmelter("alloy-smelter"){{
            craftEffect = BlockFx.smeltsmoke;
            result = Items.surgealloy;
            craftTime = 75f;
            size = 2;

            useFlux = true;
            fluxNeeded = 3;

            consumes.powerDirect(0.4f);
            consumes.items(new ItemStack[]{new ItemStack(Items.titanium, 2), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.copper, 3)});
        }};

        cryofluidmixer = new LiquidMixer("cryofluidmixer"){{
            outputLiquid = Liquids.cryofluid;
            liquidPerItem = 50f;
            itemCapacity = 50;
            size = 2;
            hasPower = true;

            consumes.powerDirect(0.1f);
            consumes.item(Items.titanium);
            consumes.liquid(Liquids.water, 0.3f);
        }};

        blastMixer = new GenericCrafter("blast-mixer"){{
            itemCapacity = 20;
            hasItems = true;
            hasPower = true;
            hasLiquids = true;
            output = Items.blastCompound;
            size = 2;

            consumes.liquid(Liquids.oil, 0.05f);
            consumes.item(Items.pyratite, 1);
            consumes.powerDirect(0.04f);
        }};

        pyratiteMixer = new PowerSmelter("pyratite-mixer"){{
            flameColor = Color.CLEAR;
            itemCapacity = 20;
            hasItems = true;
            hasPower = true;
            result = Items.pyratite;

            size = 2;

            consumes.powerDirect(0.02f);
            consumes.items(new ItemStack[]{new ItemStack(Items.coal, 1), new ItemStack(Items.lead, 2), new ItemStack(Items.sand, 2)});
        }};

        melter = new PowerCrafter("melter"){{
            health = 200;
            outputLiquid = Liquids.lava;
            outputLiquidAmount = 0.75f;
            itemCapacity = 50;
            craftTime = 10f;
            hasLiquids = hasPower = true;

            consumes.powerDirect(0.1f);
            consumes.item(Items.stone, 2);
        }};

        separator = new Separator("separator"){{
            results = new ItemStack[]{
                new ItemStack(null, 10),
                new ItemStack(Items.sand, 10),
                new ItemStack(Items.stone, 9),
                new ItemStack(Items.copper, 4),
                new ItemStack(Items.lead, 2),
                new ItemStack(Items.coal, 2),
                new ItemStack(Items.titanium, 1),
            };
            filterTime = 40f;
            itemCapacity = 40;
            health = 50;

            consumes.item(Items.stone, 2);
            consumes.liquid(Liquids.water, 0.3f);
        }};

        centrifuge = new Separator("centrifuge"){{
            results = new ItemStack[]{
                new ItemStack(null, 13),
                new ItemStack(Items.sand, 12),
                new ItemStack(Items.stone, 11),
                new ItemStack(Items.copper, 5),
                new ItemStack(Items.lead, 3),
                new ItemStack(Items.coal, 3),
                new ItemStack(Items.titanium, 2),
                new ItemStack(Items.thorium, 1)
            };

            hasPower = true;
            filterTime = 15f;
            itemCapacity = 60;
            health = 50 * 4;
            spinnerLength = 1.5f;
            spinnerRadius = 3.5f;
            spinnerThickness = 1.5f;
            spinnerSpeed = 3f;
            size = 2;

            consumes.item(Items.stone, 2);
            consumes.powerDirect(0.2f);
            consumes.liquid(Liquids.water, 0.5f);
        }};

        biomatterCompressor = new Compressor("biomattercompressor"){{
            liquidCapacity = 60f;
            itemCapacity = 50;
            craftTime = 25f;
            outputLiquid = Liquids.oil;
            outputLiquidAmount = 1.5f;
            size = 2;
            health = 320;
            hasLiquids = true;

            consumes.item(Items.biomatter, 1);
            consumes.powerDirect(0.06f);
        }};

        pulverizer = new Pulverizer("pulverizer"){{
            itemCapacity = 40;
            output = Items.sand;
            health = 80;
            craftEffect = BlockFx.pulverize;
            craftTime = 40f;
            updateEffect = BlockFx.pulverizeSmall;
            hasItems = hasPower = true;

            consumes.item(Items.stone, 1);
            consumes.powerDirect(0.05f);
        }};

        solidifier = new GenericCrafter("solidifer"){{
            liquidCapacity = 21f;
            craftTime = 14;
            output = Items.stone;
            itemCapacity = 20;
            health = 80;
            craftEffect = BlockFx.purifystone;
            hasLiquids = hasItems = true;

            consumes.liquid(Liquids.lava, 1f);
        }};

        incinerator = new Incinerator("incinerator"){{
            health = 90;
        }};
    }
}
