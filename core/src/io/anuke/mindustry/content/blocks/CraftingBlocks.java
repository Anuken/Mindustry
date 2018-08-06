package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.production.*;

public class CraftingBlocks extends BlockList implements ContentList{
    public static Block smelter, arcsmelter, siliconsmelter, plastaniumCompressor, phaseWeaver, alloysmelter, alloyfuser,
            pyratiteMixer, blastMixer,
            cryofluidmixer, melter, separator, centrifuge, biomatterCompressor, pulverizer, solidifier, incinerator;

    @Override
    public void load(){
        smelter = new Smelter("smelter"){{
            health = 70;
            result = Items.carbide;
            craftTime = 45f;
            burnDuration = 45f;
            useFlux = true;

            consumes.items(new ItemStack[]{new ItemStack(Items.tungsten, 3)});
            consumes.item(Items.coal);
        }};

        arcsmelter = new PowerSmelter("arc-smelter"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.carbide;
            craftTime = 30f;
            size = 2;

            useFlux = true;
            fluxNeeded = 2;

            consumes.items(new ItemStack[]{new ItemStack(Items.coal, 1), new ItemStack(Items.tungsten, 2)});
            consumes.power(0.1f);
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
            consumes.power(0.05f);
        }};

        plastaniumCompressor = new PlastaniumCompressor("plastanium-compressor"){{
            hasItems = true;
            liquidCapacity = 60f;
            craftTime = 80f;
            output = Items.plastanium;
            itemCapacity = 30;
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = BlockFx.formsmoke;
            updateEffect = BlockFx.plasticburn;

            consumes.liquid(Liquids.oil, 0.3f);
            consumes.power(0.4f);
            consumes.item(Items.titanium, 2);
        }};

        phaseWeaver = new PhaseWeaver("phase-weaver"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.phasematter;
            craftTime = 120f;
            size = 2;

            consumes.items(new ItemStack[]{new ItemStack(Items.thorium, 4), new ItemStack(Items.sand, 10)});
            consumes.power(0.5f);
        }};

        alloysmelter = new PowerSmelter("alloy-smelter"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.surgealloy;
            craftTime = 50f;
            size = 2;

            useFlux = true;
            fluxNeeded = 4;

            consumes.power(0.3f);
            consumes.items(new ItemStack[]{new ItemStack(Items.titanium, 2), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.plastanium, 2)});
        }};

        alloyfuser = new PowerSmelter("alloy-fuser"){{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            result = Items.surgealloy;
            craftTime = 30f;
            size = 3;

            useFlux = true;
            fluxNeeded = 4;

            consumes.items(new ItemStack[]{new ItemStack(Items.titanium, 3), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.plastanium, 2)});
            consumes.power(0.4f);
        }};

        cryofluidmixer = new LiquidMixer("cryofluidmixer"){{
            health = 200;
            outputLiquid = Liquids.cryofluid;
            liquidPerItem = 50f;
            itemCapacity = 50;
            size = 2;
            hasPower = true;

            consumes.power(0.1f);
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
            consumes.power(0.04f);
        }};

        pyratiteMixer = new PowerSmelter("pyratite-mixer"){{
            flameColor = Color.CLEAR;
            itemCapacity = 20;
            hasItems = true;
            hasPower = true;
            result = Items.pyratite;

            size = 2;

            consumes.power(0.02f);
            consumes.items(new ItemStack[]{new ItemStack(Items.coal, 1), new ItemStack(Items.lead, 2), new ItemStack(Items.sand, 2)});
        }};

        melter = new PowerCrafter("melter"){{
            health = 200;
            outputLiquid = Liquids.lava;
            outputLiquidAmount = 0.75f;
            itemCapacity = 50;
            craftTime = 10f;
            hasLiquids = hasPower = true;

            consumes.power(0.1f);
            consumes.item(Items.stone, 2);
        }};

        separator = new Separator("separator"){{
            results = new Item[]{
                    null, null, null, null, null, null, null, null, null, null,
                    Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand,
                    Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone,
                    Items.tungsten, Items.tungsten, Items.tungsten, Items.tungsten,
                    Items.lead, Items.lead,
                    Items.coal, Items.coal,
                    Items.titanium
            };
            filterTime = 40f;
            itemCapacity = 40;
            health = 50;

            consumes.item(Items.stone, 2);
            consumes.liquid(Liquids.water, 0.3f);
        }};

        centrifuge = new Separator("centrifuge"){{
            results = new Item[]{
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand,
                    Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone,
                    Items.tungsten, Items.tungsten, Items.tungsten, Items.tungsten, Items.tungsten,
                    Items.lead, Items.lead, Items.lead,
                    Items.coal, Items.coal, Items.coal,
                    Items.titanium, Items.titanium,
                    Items.thorium,
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
            consumes.power(0.2f);
            consumes.liquid(Liquids.water, 0.5f);
        }};

        biomatterCompressor = new Compressor("biomattercompressor"){{
            liquidCapacity = 60f;
            itemCapacity = 50;
            craftTime = 25f;
            outputLiquid = Liquids.oil;
            outputLiquidAmount = 0.9f;
            size = 2;
            health = 320;
            hasLiquids = true;

            consumes.item(Items.biomatter, 1);
            consumes.power(0.06f);
        }};

        pulverizer = new Pulverizer("pulverizer"){{
            itemCapacity = 40;
            output = Items.sand;
            health = 80;
            craftEffect = BlockFx.pulverize;
            craftTime = 50f;
            updateEffect = BlockFx.pulverizeSmall;
            hasItems = hasPower = true;

            consumes.item(Items.stone, 2);
            consumes.power(0.1f);
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
