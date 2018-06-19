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

public class CraftingBlocks extends BlockList implements ContentList {
    public static Block smelter, arcsmelter, siliconsmelter, plasteelcompressor, phaseweaver, alloysmelter, alloyfuser, cryofluidmixer, melter, separator, centrifuge, biomatterCompressor, pulverizer, oilRefinery, stoneFormer, incinerator;

    @Override
    public void load() {
        smelter = new Smelter("smelter") {{
            health = 70;
            inputs = new Item[]{Items.iron};
            fuel = Items.coal;
            result = Items.steel;
            craftTime = 35f;
            useFlux = true;
        }};

        arcsmelter = new PowerSmelter("arc-smelter") {{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            inputs = new ItemStack[]{new ItemStack(Items.coal, 1), new ItemStack(Items.iron, 1)};
            result = Items.steel;
            powerUse = 0.1f;
            craftTime = 25f;
            size = 2;

            useFlux = true;
            fluxNeeded = 2;
        }};

        siliconsmelter = new PowerSmelter("silicon-smelter") {{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            inputs = new ItemStack[]{new ItemStack(Items.coal, 1), new ItemStack(Items.sand, 2)};
            result = Items.silicon;
            powerUse = 0.05f;
            craftTime = 40f;
            size = 2;
            hasLiquids = false;
            flameColor = Color.valueOf("ffef99");
        }};

        plasteelcompressor = new PlasteelCompressor("plasteel-compressor") {{
            inputLiquid = Liquids.oil;
            inputItem = new ItemStack(Items.steel, 1);
            liquidUse = 0.3f;
            liquidCapacity = 60f;
            powerUse = 0.5f;
            craftTime = 80f;
            output = Items.plasteel;
            itemCapacity = 30;
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = BlockFx.formsmoke;
            updateEffect = BlockFx.plasticburn;
        }};

        phaseweaver = new PowerSmelter("phase-weaver") {{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            inputs = new ItemStack[]{new ItemStack(Items.thorium, 2), new ItemStack(Items.sand, 6)};
            result = Items.phasematter;
            powerUse = 0.4f;
            craftTime = 100f;
            size = 3;
        }};

        alloysmelter = new PowerSmelter("alloy-smelter") {{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            inputs = new ItemStack[]{new ItemStack(Items.titanium, 2), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.plasteel, 2)};
            result = Items.surgealloy;
            powerUse = 0.3f;
            craftTime = 50f;
            size = 2;

            useFlux = true;
            fluxNeeded = 4;
        }};

        alloyfuser = new PowerSmelter("alloy-fuser") {{
            health = 90;
            craftEffect = BlockFx.smeltsmoke;
            inputs = new ItemStack[]{new ItemStack(Items.titanium, 3), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.plasteel, 2)};
            result = Items.surgealloy;
            powerUse = 0.4f;
            craftTime = 30f;
            size = 3;

            useFlux = true;
            fluxNeeded = 4;
        }};

        cryofluidmixer = new LiquidMixer("cryofluidmixer") {{
            health = 200;
            inputLiquid = Liquids.water;
            outputLiquid = Liquids.cryofluid;
            inputItem = Items.titanium;
            liquidPerItem = 50f;
            itemCapacity = 50;
            powerUse = 0.1f;
            size = 2;
        }};

        melter = new PowerCrafter("melter") {{
            health = 200;
            outputLiquid = Liquids.lava;
            outputLiquidAmount = 0.05f;
            input = new ItemStack(Items.stone, 1);
            itemCapacity = 50;
            craftTime = 10f;
            powerUse = 0.1f;
            hasLiquids = hasPower = true;
        }};

        separator = new Separator("separator") {{
            liquid = Liquids.water;
            item = Items.stone;
            results = new Item[]{
                    null, null, null, null, null, null, null, null, null, null,
                    Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand,
                    Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone,
                    Items.iron, Items.iron, Items.iron, Items.iron,
                    Items.lead, Items.lead,
                    Items.coal, Items.coal,
                    Items.titanium
            };
            liquidUse = 0.2f;
            filterTime = 40f;
            itemCapacity = 40;
            health = 50;
        }};

        centrifuge = new Separator("centrifuge") {{
            liquid = Liquids.water;
            item = Items.stone;
            results = new Item[]{
                    null, null, null, null, null, null, null, null, null, null, null, null, null,
                    Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand, Items.sand,
                    Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone, Items.stone,
                    Items.iron, Items.iron, Items.iron, Items.iron, Items.iron,
                    Items.lead, Items.lead, Items.lead,
                    Items.coal, Items.coal, Items.coal,
                    Items.titanium, Items.titanium,
                    Items.thorium,
            };

            liquidUse = 0.3f;
            hasPower = true;
            powerUse = 0.2f;
            filterTime = 15f;
            itemCapacity = 60;
            health = 50 * 4;
            spinnerLength = 1.5f;
            spinnerRadius = 3.5f;
            spinnerThickness = 1.5f;
            spinnerSpeed = 3f;
            size = 2;
        }};

        biomatterCompressor = new Compressor("biomattercompressor") {{
            input = new ItemStack(Items.biomatter, 1);
            liquidCapacity = 60f;
            itemCapacity = 50;
            powerUse = 0.06f;
            craftTime = 25f;
            outputLiquid = Liquids.oil;
            outputLiquidAmount = 0.1f;
            size = 2;
            health = 320;
            hasLiquids = true;
        }};

        pulverizer = new Pulverizer("pulverizer") {{
            inputItem = new ItemStack(Items.stone, 2);
            itemCapacity = 40;
            powerUse = 0.2f;
            output = Items.sand;
            health = 80;
            craftEffect = BlockFx.pulverize;
            craftTime = 60f;
            updateEffect = BlockFx.pulverizeSmall;
            hasItems = hasPower = true;
        }};

        oilRefinery = new GenericCrafter("oilrefinery") {{
            inputLiquid = Liquids.oil;
            powerUse = 0.05f;
            liquidUse = 0.1f;
            liquidCapacity = 56f;
            output = Items.coal;
            health = 80;
            craftEffect = BlockFx.purifyoil;
            hasItems = hasLiquids = hasPower = true;
        }};

        stoneFormer = new GenericCrafter("stoneformer") {{
            inputLiquid = Liquids.lava;
            liquidUse = 1f;
            liquidCapacity = 21f;
            craftTime = 14;
            output = Items.stone;
            health = 80;
            craftEffect = BlockFx.purifystone;
            hasLiquids = hasItems = true;
        }};

        incinerator = new Incinerator("incinerator") {{
            health = 90;
        }};
    }
}
