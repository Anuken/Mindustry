package mindustry.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.experimental.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

public class Blocks implements ContentList{
    public static Block

    //environment
    air, spawn, cliff, deepwater, water, taintedWater, tar, slag, stone, craters, charr, sand, darksand, ice, snow, darksandTaintedWater,
    holostone, rocks, sporerocks, icerocks, cliffs, sporePine, snowPine, pine, shrubs, whiteTree, whiteTreeDead, sporeCluster,
    iceSnow, sandWater, darksandWater, duneRocks, sandRocks, moss, sporeMoss, shale, shaleRocks, shaleBoulder, sandBoulder, grass, salt,
    metalFloor, metalFloorDamaged, metalFloor2, metalFloor3, metalFloor5, ignarock, magmarock, hotrock, snowrocks, rock, snowrock, saltRocks,
    darkPanel1, darkPanel2, darkPanel3, darkPanel4, darkPanel5, darkPanel6, darkMetal,
    pebbles, tendrils,

    //ores
    oreCopper, oreLead, oreScrap, oreCoal, oreTitanium, oreThorium,

    //crafting
    siliconSmelter, kiln, graphitePress, plastaniumCompressor, multiPress, phaseWeaver, surgeSmelter, pyratiteMixer, blastMixer, cryofluidMixer,
    melter, separator, sporePress, pulverizer, incinerator, coalCentrifuge,

    //sandbox
    powerSource, powerVoid, itemSource, itemVoid, liquidSource, liquidVoid, message, illuminator,

    //defense
    copperWall, copperWallLarge, titaniumWall, titaniumWallLarge, plastaniumWall, plastaniumWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
    phaseWall, phaseWallLarge, surgeWall, surgeWallLarge, mender, mendProjector, overdriveProjector, forceProjector, shockMine,
    scrapWall, scrapWallLarge, scrapWallHuge, scrapWallGigantic, thruster, //ok, these names are getting ridiculous, but at least I don't have humongous walls yet

    //transport
    conveyor, titaniumConveyor, plastaniumConveyor, armoredConveyor, distributor, junction, itemBridge, phaseConveyor, sorter, invertedSorter, router,
    overflowGate, underflowGate, massDriver, massConveyor,

    //liquid
    mechanicalPump, rotaryPump, thermalPump, conduit, pulseConduit, platedConduit, liquidRouter, liquidTank, liquidJunction, bridgeConduit, phaseConduit,

    //power
    combustionGenerator, thermalGenerator, turbineGenerator, differentialGenerator, rtgGenerator, solarPanel, largeSolarPanel, thoriumReactor,
    impactReactor, battery, batteryLarge, powerNode, powerNodeLarge, surgeTower, diode,

    //production
    mechanicalDrill, pneumaticDrill, laserDrill, blastDrill, waterExtractor, oilExtractor, cultivator,

    //storage
    coreShard, coreFoundation, coreNucleus, vault, container, unloader, launchPad, launchPadLarge,

    //turrets
    duo, scatter, scorch, hail, arc, wave, lancer, swarmer, salvo, fuse, ripple, cyclone, spectre, meltdown,

    //units
    groundFactory, airFactory, navalFactory, repairPoint,

    //misc experimental

    blockForge, blockLauncher;

    ;

    @Override
    public void load(){
        //region environment

        air = new Floor("air"){
            {
                alwaysReplace = true;
                hasShadow = false;
            }

            public void drawBase(Tile tile){}
            public void load(){}
            public void init(){}
            public boolean isHidden(){
                return true;
            }

            public TextureRegion[] variantRegions(){
                if(variantRegions == null){
                    variantRegions = new TextureRegion[]{Core.atlas.find("clear")};
                }
                return variantRegions;
            }
        };

        spawn = new OverlayFloor("spawn"){
            {
                variants = 0;
            }
            @Override
            public void drawBase(Tile tile){}
        };

        cliff = new Cliff("cliff");

        //Registers build blocks
        //no reference is needed here since they can be looked up by name later
        for(int i = 1; i <= BuildBlock.maxSize; i++){
            new BuildBlock(i);
        }

        deepwater = new Floor("deepwater"){{
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            isLiquid = true;
            status = StatusEffects.wet;
            statusDuration = 120f;
            drownTime = 140f;
            cacheLayer = CacheLayer.water;
            albedo = 0.5f;
        }};

        water = new Floor("water"){{
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.5f;
        }};

        taintedWater = new Floor("tainted-water"){{
            speedMultiplier = 0.17f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 140f;
            drownTime = 120f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.5f;
        }};

        darksandTaintedWater = new ShallowLiquid("darksand-tainted-water"){{
            speedMultiplier = 0.75f;
            statusDuration = 60f;
            albedo = 0.5f;
        }};

        sandWater = new ShallowLiquid("sand-water"){{
            speedMultiplier = 0.8f;
            statusDuration = 50f;
            albedo = 0.5f;
        }};

        darksandWater = new ShallowLiquid("darksand-water"){{
            speedMultiplier = 0.8f;
            statusDuration = 50f;
            albedo = 0.5f;
        }};

        tar = new Floor("tar"){{
            drownTime = 150f;
            status = StatusEffects.tarred;
            statusDuration = 240f;
            speedMultiplier = 0.19f;
            variants = 0;
            liquidDrop = Liquids.oil;
            isLiquid = true;
            cacheLayer = CacheLayer.tar;
        }};

        slag = new Floor("slag"){{
            drownTime = 150f;
            status = StatusEffects.melting;
            statusDuration = 240f;
            speedMultiplier = 0.19f;
            variants = 0;
            liquidDrop = Liquids.slag;
            isLiquid = true;
            cacheLayer = CacheLayer.slag;
        }};

        stone = new Floor("stone"){{

        }};

        craters = new Floor("craters"){{
            variants = 3;
            blendGroup = stone;
        }};

        charr = new Floor("char"){{
            blendGroup = stone;
        }};

        ignarock = new Floor("ignarock"){{
            attributes.set(Attribute.water, -0.1f);
        }};

        hotrock = new Floor("hotrock"){{
            attributes.set(Attribute.heat, 0.5f);
            attributes.set(Attribute.water, -0.2f);
            blendGroup = ignarock;
        }};

        magmarock = new Floor("magmarock"){{
            attributes.set(Attribute.heat, 0.75f);
            attributes.set(Attribute.water, -0.5f);
            updateEffect = Fx.magmasmoke;
            blendGroup = ignarock;
        }};

        sand = new Floor("sand"){{
            itemDrop = Items.sand;
            playerUnmineable = true;
        }};

        darksand = new Floor("darksand"){{
            itemDrop = Items.sand;
            playerUnmineable = true;
        }};

        ((ShallowLiquid)darksandTaintedWater).set(Blocks.taintedWater, Blocks.darksand);
        ((ShallowLiquid)sandWater).set(Blocks.water, Blocks.sand);
        ((ShallowLiquid)darksandWater).set(Blocks.water, Blocks.darksand);

        holostone = new Floor("holostone"){{

        }};

        grass = new Floor("grass"){{

        }};

        salt = new Floor("salt"){{
            variants = 0;
            attributes.set(Attribute.water, -0.2f);
        }};

        snow = new Floor("snow"){{
            attributes.set(Attribute.water, 0.2f);
        }};

        ice = new Floor("ice"){{
            dragMultiplier = 0.35f;
            speedMultiplier = 0.9f;
            attributes.set(Attribute.water, 0.4f);
        }};

        iceSnow = new Floor("ice-snow"){{
            dragMultiplier = 0.6f;
            variants = 3;
            attributes.set(Attribute.water, 0.3f);
        }};

        cliffs = new StaticWall("cliffs"){{
            variants = 1;
            fillsTile = false;
        }};

        rocks = new StaticWall("rocks"){{
            variants = 2;
        }};

        sporerocks = new StaticWall("sporerocks"){{
            variants = 2;
        }};

        rock = new Rock("rock"){{
            variants = 2;
        }};

        snowrock = new Rock("snowrock"){{
            variants = 2;
        }};

        icerocks = new StaticWall("icerocks"){{
            variants = 2;
            iceSnow.asFloor().wall = this;
        }};

        snowrocks = new StaticWall("snowrocks"){{
            variants = 2;
        }};

        duneRocks = new StaticWall("dunerocks"){{
            variants = 2;
        }};

        sandRocks = new StaticWall("sandrocks"){{
            variants = 2;
        }};

        saltRocks = new StaticWall("saltrocks"){{
        }};

        sporePine = new StaticTree("spore-pine"){{
            variants = 0;
        }};

        snowPine = new StaticTree("snow-pine"){{
            variants = 0;
        }};

        pine = new StaticTree("pine"){{
            variants = 0;
        }};

        shrubs = new StaticWall("shrubs"){{

        }};

        whiteTreeDead = new TreeBlock("white-tree-dead"){{
        }};

        whiteTree = new TreeBlock("white-tree"){{
        }};

        sporeCluster = new Rock("spore-cluster"){{
            variants = 3;
        }};

        shale = new Floor("shale"){{
            variants = 3;
            attributes.set(Attribute.oil, 0.15f);
        }};

        shaleRocks = new StaticWall("shalerocks"){{
            variants = 2;
        }};

        shaleBoulder = new Rock("shale-boulder"){{
            variants = 2;
        }};

        sandBoulder = new Rock("sand-boulder"){{
            variants = 2;
        }};

        moss = new Floor("moss"){{
            variants = 3;
            attributes.set(Attribute.spores, 0.15f);
            wall = sporePine;
        }};

        sporeMoss = new Floor("spore-moss"){{
            variants = 3;
            attributes.set(Attribute.spores, 0.3f);
            wall = sporerocks;
        }};

        metalFloor = new Floor("metal-floor"){{
            variants = 0;
        }};

        metalFloorDamaged = new Floor("metal-floor-damaged"){{
            variants = 3;
        }};

        metalFloor2 = new Floor("metal-floor-2"){{
            variants = 0;
        }};

        metalFloor3 = new Floor("metal-floor-3"){{
            variants = 0;
        }};

        metalFloor5 = new Floor("metal-floor-5"){{
            variants = 0;
        }};

        darkPanel1 = new Floor("dark-panel-1"){{ variants = 0; }};
        darkPanel2 = new Floor("dark-panel-2"){{ variants = 0; }};
        darkPanel3 = new Floor("dark-panel-3"){{ variants = 0; }};
        darkPanel4 = new Floor("dark-panel-4"){{ variants = 0; }};
        darkPanel5 = new Floor("dark-panel-5"){{ variants = 0; }};
        darkPanel6 = new Floor("dark-panel-6"){{ variants = 0; }};

        darkMetal = new StaticWall("dark-metal");

        pebbles = new DoubleOverlayFloor("pebbles");

        tendrils = new OverlayFloor("tendrils");

        //endregion
        //region ore

        oreCopper = new OreBlock(Items.copper){{
            oreDefault = true;
            oreThreshold = 0.81f;
            oreScale = 23.47619f;
        }};

        oreLead = new OreBlock(Items.lead){{
            oreDefault = true;
            oreThreshold = 0.828f;
            oreScale = 23.952381f;
        }};

        oreScrap = new OreBlock(Items.scrap);

        oreCoal = new OreBlock(Items.coal){{
            oreDefault = true;
            oreThreshold = 0.846f;
            oreScale = 24.428572f;
        }};

        oreTitanium = new OreBlock(Items.titanium){{
            oreDefault = true;
            oreThreshold = 0.864f;
            oreScale = 24.904762f;
        }};

        oreThorium = new OreBlock(Items.thorium){{
            oreDefault = true;
            oreThreshold = 0.882f;
            oreScale = 25.380953f;
        }};

        //endregion
        //region crafting

        graphitePress = new GenericCrafter("graphite-press"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 75, Items.lead, 30));

            craftEffect = Fx.pulverizeMedium;
            outputItem = new ItemStack(Items.graphite, 1);
            craftTime = 90f;
            size = 2;
            hasItems = true;

            consumes.item(Items.coal, 2);
        }};

        multiPress = new GenericCrafter("multi-press"){{
            requirements(Category.crafting, ItemStack.with(Items.titanium, 100, Items.silicon, 25, Items.lead, 100, Items.graphite, 50));

            craftEffect = Fx.pulverizeMedium;
            outputItem = new ItemStack(Items.graphite, 2);
            craftTime = 30f;
            size = 3;
            hasItems = true;
            hasLiquids = true;
            hasPower = true;

            consumes.power(1.8f);
            consumes.item(Items.coal, 3);
            consumes.liquid(Liquids.water, 0.1f);
        }};

        siliconSmelter = new GenericSmelter("silicon-smelter"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 25));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.silicon, 1);
            craftTime = 40f;
            size = 2;
            hasPower = true;
            hasLiquids = false;
            flameColor = Color.valueOf("ffef99");

            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.sand, 2));
            consumes.power(0.50f);
        }};

        kiln = new GenericSmelter("kiln"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 60, Items.graphite, 30, Items.lead, 30));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.metaglass, 1);
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = true;
            flameColor = Color.valueOf("ffc099");

            consumes.items(new ItemStack(Items.lead, 1), new ItemStack(Items.sand, 1));
            consumes.power(0.60f);
        }};

        plastaniumCompressor = new GenericCrafter("plastanium-compressor"){{
            requirements(Category.crafting, ItemStack.with(Items.silicon, 80, Items.lead, 115, Items.graphite, 60, Items.titanium, 80));
            hasItems = true;
            liquidCapacity = 60f;
            craftTime = 60f;
            outputItem = new ItemStack(Items.plastanium, 1);
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = Fx.formsmoke;
            updateEffect = Fx.plasticburn;

            consumes.liquid(Liquids.oil, 0.25f);
            consumes.power(3f);
            consumes.item(Items.titanium, 2);

            int topRegion = reg("-top");

            drawer = entity -> {
                Draw.rect(region, entity.x(), entity.y());
                Draw.alpha(Mathf.absin(entity.totalProgress, 3f, 0.9f) * entity.warmup);
                Draw.rect(reg(topRegion), entity.x(), entity.y());
                Draw.reset();
            };
        }};

        phaseWeaver = new GenericCrafter("phase-weaver"){{
            requirements(Category.crafting, ItemStack.with(Items.silicon, 130, Items.lead, 120, Items.thorium, 75));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.phasefabric, 1);
            craftTime = 120f;
            size = 2;
            hasPower = true;

            consumes.items(new ItemStack(Items.thorium, 4), new ItemStack(Items.sand, 10));
            consumes.power(5f);
            itemCapacity = 20;

            int bottomRegion = reg("-bottom"), weaveRegion = reg("-weave");

            drawIcons = () -> new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name), Core.atlas.find(name + "-weave")};

            drawer = entity -> {
                Draw.rect(reg(bottomRegion), entity.x(), entity.y());
                Draw.rect(reg(weaveRegion), entity.x(), entity.y(), entity.totalProgress);

                Draw.color(Pal.accent);
                Draw.alpha(entity.warmup);

                Lines.lineAngleCenter(
                entity.x() + Mathf.sin(entity.totalProgress, 6f, Vars.tilesize / 3f * size),
                entity.y(),
                90,
                size * Vars.tilesize / 2f);

                Draw.reset();

                Draw.rect(region, entity.x(), entity.y());
            };
        }};

        surgeSmelter = new GenericSmelter("alloy-smelter"){{
            requirements(Category.crafting, ItemStack.with(Items.silicon, 80, Items.lead, 80, Items.thorium, 70));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.surgealloy, 1);
            craftTime = 75f;
            size = 3;
            hasPower = true;

            consumes.power(4f);
            consumes.items(new ItemStack(Items.copper, 3), new ItemStack(Items.lead, 4), new ItemStack(Items.titanium, 2), new ItemStack(Items.silicon, 3));
        }};

        cryofluidMixer = new LiquidConverter("cryofluidmixer"){{
            requirements(Category.crafting, ItemStack.with(Items.lead, 65, Items.silicon, 40, Items.titanium, 60));
            outputLiquid = new LiquidStack(Liquids.cryofluid, 0.2f);
            craftTime = 120f;
            size = 2;
            hasPower = true;
            hasItems = true;
            hasLiquids = true;
            rotate = false;
            solid = true;
            outputsLiquid = true;

            consumes.power(1f);
            consumes.item(Items.titanium);
            consumes.liquid(Liquids.water, 0.2f);

            int liquidRegion = reg("-liquid"), topRegion = reg("-top"), bottomRegion = reg("-bottom");

            drawIcons = () -> new TextureRegion[]{Core.atlas.find(name + "-bottom"), Core.atlas.find(name + "-top")};

            drawer = entity -> {
                int rotation = rotate ? entity.rotation() * 90 : 0;

                Draw.rect(reg(bottomRegion), entity.x(), entity.y(), rotation);

                if(entity.liquids().total() > 0.001f){
                    Draw.color(outputLiquid.liquid.color);
                    Draw.alpha(entity.liquids().get(outputLiquid.liquid) / liquidCapacity);
                    Draw.rect(reg(liquidRegion), entity.x(), entity.y(), rotation);
                    Draw.color();
                }

                Draw.rect(reg(topRegion), entity.x(), entity.y(), rotation);
            };
        }};

        blastMixer = new GenericCrafter("blast-mixer"){{
            requirements(Category.crafting, ItemStack.with(Items.lead, 30, Items.titanium, 20));
            hasItems = true;
            hasPower = true;
            outputItem = new ItemStack(Items.blastCompound, 1);
            size = 2;

            consumes.items(new ItemStack(Items.pyratite, 1), new ItemStack(Items.sporePod, 1));
            consumes.power(0.40f);
        }};

        pyratiteMixer = new GenericSmelter("pyratite-mixer"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 50, Items.lead, 25));
            flameColor = Color.clear;
            hasItems = true;
            hasPower = true;
            outputItem = new ItemStack(Items.pyratite, 1);

            size = 2;

            consumes.power(0.20f);
            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.lead, 2), new ItemStack(Items.sand, 2));
        }};

        melter = new GenericCrafter("melter"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 35, Items.graphite, 45));
            health = 200;
            outputLiquid = new LiquidStack(Liquids.slag, 2f);
            craftTime = 10f;
            hasLiquids = hasPower = true;

            consumes.power(1f);
            consumes.item(Items.scrap, 1);
        }};

        separator = new Separator("separator"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.titanium, 25));
            results = ItemStack.with(
            Items.copper, 5,
            Items.lead, 3,
            Items.graphite, 2,
            Items.titanium, 2
            );
            hasPower = true;
            craftTime = 35f;
            size = 2;

            consumes.power(1f);
            consumes.liquid(Liquids.slag, 0.07f);
        }};

        sporePress = new GenericCrafter("spore-press"){{
            requirements(Category.crafting, ItemStack.with(Items.lead, 35, Items.silicon, 30));
            liquidCapacity = 60f;
            craftTime = 20f;
            outputLiquid = new LiquidStack(Liquids.oil, 6f);
            size = 2;
            health = 320;
            hasLiquids = true;
            hasPower = true;
            craftEffect = Fx.none;

            consumes.item(Items.sporePod, 1);
            consumes.power(0.60f);

            int[] frameRegions = new int[3];
            for(int i = 0; i < 3; i++){
                frameRegions[i] = reg("-frame" + i);
            }

            int liquidRegion = reg("-liquid");
            int topRegion = reg("-top");

            drawIcons = () -> new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-top")};
            drawer = entity -> {
                Draw.rect(region, entity.x(), entity.y());
                Draw.rect(reg(frameRegions[(int)Mathf.absin(entity.totalProgress, 5f, 2.999f)]), entity.x(), entity.y());
                Draw.color(Color.clear, entity.liquids().current().color, entity.liquids().total() / liquidCapacity);
                Draw.rect(reg(liquidRegion), entity.x(), entity.y());
                Draw.color();
                Draw.rect(reg(topRegion), entity.x(), entity.y());
            };
        }};

        pulverizer = new GenericCrafter("pulverizer"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 30, Items.lead, 25));
            outputItem = new ItemStack(Items.sand, 1);
            craftEffect = Fx.pulverize;
            craftTime = 40f;
            updateEffect = Fx.pulverizeSmall;
            hasItems = hasPower = true;

            consumes.item(Items.scrap, 1);
            consumes.power(0.50f);

            int rotatorRegion = reg("-rotator");

            drawIcons = () -> new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-rotator")};

            drawer = entity -> {
                Draw.rect(region, entity.x(), entity.y());
                Draw.rect(reg(rotatorRegion), entity.x(), entity.y(), entity.totalProgress * 2f);
            };
        }};

        coalCentrifuge = new GenericCrafter("coal-centrifuge"){{
            requirements(Category.crafting, ItemStack.with(Items.titanium, 20, Items.graphite, 40, Items.lead, 30));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.coal, 1);
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = hasLiquids = true;

            consumes.liquid(Liquids.oil, 0.09f);
            consumes.power(0.5f);
        }};

        incinerator = new Incinerator("incinerator"){{
            requirements(Category.crafting, ItemStack.with(Items.graphite, 5, Items.lead, 15));
            health = 90;
            consumes.power(0.50f);
        }};

        //endregion
        //region defense

        int wallHealthMultiplier = 4;

        copperWall = new Wall("copper-wall"){{
            requirements(Category.defense, ItemStack.with(Items.copper, 6));
            health = 80 * wallHealthMultiplier;
        }};

        copperWallLarge = new Wall("copper-wall-large"){{
            requirements(Category.defense, ItemStack.mult(copperWall.requirements, 4));
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        titaniumWall = new Wall("titanium-wall"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 6));
            health = 110 * wallHealthMultiplier;
        }};

        titaniumWallLarge = new Wall("titanium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(titaniumWall.requirements, 4));
            health = 110 * wallHealthMultiplier * 4;
            size = 2;
        }};

        plastaniumWall = new Wall("plastanium-wall"){{
            requirements(Category.defense, ItemStack.with(Items.plastanium, 5, Items.metaglass, 2));
            health = 190 * wallHealthMultiplier;
            insulated = true;
        }};

        plastaniumWallLarge = new Wall("plastanium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(plastaniumWall.requirements, 4));
            health = 190 * wallHealthMultiplier * 4;
            size = 2;
            insulated = true;
        }};

        thoriumWall = new Wall("thorium-wall"){{
            requirements(Category.defense, ItemStack.with(Items.thorium, 6));
            health = 200 * wallHealthMultiplier;
        }};

        thoriumWallLarge = new Wall("thorium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(thoriumWall.requirements, 4));
            health = 200 * wallHealthMultiplier * 4;
            size = 2;
        }};

        phaseWall = new DeflectorWall("phase-wall"){{
            requirements(Category.defense, ItemStack.with(Items.phasefabric, 6));
            health = 150 * wallHealthMultiplier;
        }};

        phaseWallLarge = new DeflectorWall("phase-wall-large"){{
            requirements(Category.defense, ItemStack.mult(phaseWall.requirements, 4));
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        surgeWall = new SurgeWall("surge-wall"){{
            requirements(Category.defense, ItemStack.with(Items.surgealloy, 6));
            health = 230 * wallHealthMultiplier;
        }};

        surgeWallLarge = new SurgeWall("surge-wall-large"){{
            requirements(Category.defense, ItemStack.mult(surgeWall.requirements, 4));
            health = 230 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        door = new Door("door"){{
            requirements(Category.defense, ItemStack.with(Items.graphite, 6, Items.silicon, 4));
            health = 100 * wallHealthMultiplier;
        }};

        doorLarge = new Door("door-large"){{
            requirements(Category.defense, ItemStack.mult(door.requirements, 4));
            openfx = Fx.dooropenlarge;
            closefx = Fx.doorcloselarge;
            health = 100 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        scrapWall = new Wall("scrap-wall"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.with());
            health = 60 * wallHealthMultiplier;
            variants = 5;
        }};

        scrapWallLarge = new Wall("scrap-wall-large"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.with());
            health = 60 * 4 * wallHealthMultiplier;
            size = 2;
            variants = 4;
        }};

        scrapWallHuge = new Wall("scrap-wall-huge"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.with());
            health = 60 * 9 * wallHealthMultiplier;
            size = 3;
            variants = 3;
        }};

        scrapWallGigantic = new Wall("scrap-wall-gigantic"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.with());
            health = 60 * 16 * wallHealthMultiplier;
            size = 4;
        }};

        thruster = new Wall("thruster"){{
            health = 55 * 16 * wallHealthMultiplier;
            size = 4;
        }};

        mender = new MendProjector("mender"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 30, Items.copper, 25));
            consumes.power(0.3f);
            size = 1;
            reload = 200f;
            range = 40f;
            healPercent = 4f;
            phaseBoost = 4f;
            phaseRangeBoost = 20f;
            health = 80;
            consumes.item(Items.silicon).boost();
        }};

        mendProjector = new MendProjector("mend-projector"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 100, Items.titanium, 25, Items.silicon, 40));
            consumes.power(1.5f);
            size = 2;
            reload = 250f;
            range = 85f;
            healPercent = 14f;
            health = 80 * size * size;
            consumes.item(Items.phasefabric).boost();
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 100, Items.titanium, 75, Items.silicon, 75, Items.plastanium, 30));
            consumes.power(3.50f);
            size = 2;
            consumes.item(Items.phasefabric).boost();
        }};

        forceProjector = new ForceProjector("force-projector"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 100, Items.titanium, 75, Items.silicon, 125));
            size = 3;
            consumes.item(Items.phasefabric).boost();
            consumes.power(3f);
        }};

        shockMine = new ShockMine("shock-mine"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 25, Items.silicon, 12));
            hasShadow = false;
            health = 40;
            damage = 11;
            tileDamage = 7f;
            length = 10;
            tendrils = 5;
        }};

        //endregion
        //region distribution

        conveyor = new Conveyor("conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 1), true);
            health = 45;
            speed = 0.03f;
            displayedSpeed = 4.2f;
        }};

        titaniumConveyor = new Conveyor("titanium-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 1, Items.lead, 1, Items.titanium, 1));
            health = 65;
            speed = 0.08f;
            displayedSpeed = 11f;
        }};

        plastaniumConveyor = new StackConveyor("plastanium-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.plastanium, 1, Items.silicon, 1, Items.graphite, 1));
            health = 75;
            speed = 2.5f / 60f;
            recharge = 2f;
        }};

        armoredConveyor = new ArmoredConveyor("armored-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.plastanium, 1, Items.thorium, 1, Items.metaglass, 1));
            health = 180;
            speed = 0.08f;
            displayedSpeed = 10f;
        }};

        junction = new Junction("junction"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 2), true);
            speed = 26;
            capacity = 12;
            health = 30;
            buildCostMultiplier = 6f;
        }};

        itemBridge = new BufferedItemBridge("bridge-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 4, Items.copper, 4));
            range = 4;
            speed = 70f;
            bufferCapacity = 14;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.phasefabric, 5, Items.silicon, 7, Items.lead, 10, Items.graphite, 10));
            range = 12;
            canOverdrive = false;
            hasPower = true;
            consumes.power(0.30f);
        }};

        sorter = new Sorter("sorter"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 2, Items.copper, 2));
            buildCostMultiplier = 3f;
        }};

        invertedSorter = new Sorter("inverted-sorter"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 2, Items.copper, 2));
            buildCostMultiplier = 3f;
            invert = true;
        }};

        router = new Router("router"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 3));
            buildCostMultiplier = 2f;
        }};

        distributor = new Router("distributor"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 4, Items.copper, 4));
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 2, Items.copper, 4));
            buildCostMultiplier = 3f;
        }};

        underflowGate = new OverflowGate("underflow-gate"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 2, Items.copper, 4));
            buildCostMultiplier = 3f;
            invert = true;
        }};

        massDriver = new MassDriver("mass-driver"){{
            requirements(Category.distribution, ItemStack.with(Items.titanium, 125, Items.silicon, 75, Items.lead, 125, Items.thorium, 50));
            size = 3;
            itemCapacity = 120;
            reloadTime = 200f;
            range = 440f;
            consumes.power(1.75f);
        }};

        massConveyor = new PayloadConveyor("mass-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 1));
        }};

        //endregion
        //region liquid

        mechanicalPump = new Pump("mechanical-pump"){{
            requirements(Category.liquid, ItemStack.with(Items.copper, 15, Items.metaglass, 10));
            pumpAmount = 0.1f;
        }};

        rotaryPump = new Pump("rotary-pump"){{
            requirements(Category.liquid, ItemStack.with(Items.copper, 70, Items.metaglass, 50, Items.silicon, 20, Items.titanium, 35));
            pumpAmount = 0.8f;
            consumes.power(0.15f);
            liquidCapacity = 30f;
            hasPower = true;
            size = 2;
        }};

        thermalPump = new Pump("thermal-pump"){{
            requirements(Category.liquid, ItemStack.with(Items.copper, 80, Items.metaglass, 70, Items.silicon, 30, Items.titanium, 40, Items.thorium, 35));
            pumpAmount = 1.5f;
            consumes.power(0.30f);
            liquidCapacity = 40f;
            hasPower = true;
            size = 3;
        }};

        conduit = new Conduit("conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.metaglass, 1));
            health = 45;
        }};

        pulseConduit = new Conduit("pulse-conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.titanium, 2, Items.metaglass, 1));
            liquidCapacity = 16f;
            liquidPressure = 1.025f;
            health = 90;
        }};

        platedConduit = new ArmoredConduit("plated-conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.thorium, 2, Items.metaglass, 1, Items.plastanium, 1));
            liquidCapacity = 16f;
            liquidPressure = 1.025f;
            health = 220;
        }};

        liquidRouter = new LiquidRouter("liquid-router"){{
            requirements(Category.liquid, ItemStack.with(Items.graphite, 4, Items.metaglass, 2));
            liquidCapacity = 20f;
        }};

        liquidTank = new LiquidRouter("liquid-tank"){{
            requirements(Category.liquid, ItemStack.with(Items.titanium, 25, Items.metaglass, 25));
            size = 3;
            liquidCapacity = 1500f;
            health = 500;
        }};

        liquidJunction = new LiquidJunction("liquid-junction"){{
            requirements(Category.liquid, ItemStack.with(Items.graphite, 2, Items.metaglass, 2));
        }};

        bridgeConduit = new LiquidExtendingBridge("bridge-conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.graphite, 4, Items.metaglass, 8));
            range = 4;
            hasPower = false;
        }};

        phaseConduit = new LiquidBridge("phase-conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.phasefabric, 5, Items.silicon, 7, Items.metaglass, 20, Items.titanium, 10));
            range = 12;
            hasPower = true;
            canOverdrive = false;
            consumes.power(0.30f);
        }};

        //endregion
        //region power

        powerNode = new PowerNode("power-node"){{
            requirements(Category.power, ItemStack.with(Items.copper, 1, Items.lead, 3));
            maxNodes = 20;
            laserRange = 6;
        }};

        powerNodeLarge = new PowerNode("power-node-large"){{
            requirements(Category.power, ItemStack.with(Items.titanium, 5, Items.lead, 10, Items.silicon, 3));
            size = 2;
            maxNodes = 30;
            laserRange = 9.5f;
        }};

        surgeTower = new PowerNode("surge-tower"){{
            requirements(Category.power, ItemStack.with(Items.titanium, 7, Items.lead, 10, Items.silicon, 15, Items.surgealloy, 15));
            size = 2;
            maxNodes = 2;
            laserRange = 30f;
        }};

        diode = new PowerDiode("diode"){{
            requirements(Category.power, ItemStack.with(Items.silicon, 10, Items.plastanium, 5, Items.metaglass, 10));
        }};

        battery = new Battery("battery"){{
            requirements(Category.power, ItemStack.with(Items.copper, 4, Items.lead, 20));
            consumes.powerBuffered(4000f);
        }};

        batteryLarge = new Battery("battery-large"){{
            requirements(Category.power, ItemStack.with(Items.titanium, 20, Items.lead, 40, Items.silicon, 20));
            size = 3;
            consumes.powerBuffered(50000f);
        }};

        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 25, Items.lead, 15));
            powerProduction = 1f;
            itemDuration = 120f;
        }};

        thermalGenerator = new ThermalGenerator("thermal-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 40, Items.graphite, 35, Items.lead, 50, Items.silicon, 35, Items.metaglass, 40));
            powerProduction = 1.8f;
            generateEffect = Fx.redgeneratespark;
            size = 2;
        }};

        turbineGenerator = new BurnerGenerator("turbine-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 35, Items.graphite, 25, Items.lead, 40, Items.silicon, 30));
            powerProduction = 6f;
            itemDuration = 90f;
            consumes.liquid(Liquids.water, 0.05f);
            hasLiquids = true;
            size = 2;
        }};

        differentialGenerator = new SingleTypeGenerator("differential-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 70, Items.titanium, 50, Items.lead, 100, Items.silicon, 65, Items.metaglass, 50));
            powerProduction = 16f;
            itemDuration = 140f;
            hasLiquids = true;
            hasItems = true;
            size = 3;

            consumes.item(Items.pyratite).optional(true, false);
            consumes.liquid(Liquids.cryofluid, 0.15f);
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            requirements(Category.power, ItemStack.with(Items.lead, 100, Items.silicon, 75, Items.phasefabric, 25, Items.plastanium, 75, Items.thorium, 50));
            size = 2;
            powerProduction = 3f;
            itemDuration = 440f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            requirements(Category.power, ItemStack.with(Items.lead, 10, Items.silicon, 15));
            powerProduction = 0.06f;
        }};

        largeSolarPanel = new SolarGenerator("solar-panel-large"){{
            requirements(Category.power, ItemStack.with(Items.lead, 100, Items.silicon, 145, Items.phasefabric, 15));
            size = 3;
            powerProduction = 0.9f;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            requirements(Category.power, ItemStack.with(Items.lead, 300, Items.silicon, 200, Items.graphite, 150, Items.thorium, 150, Items.metaglass, 50));
            size = 3;
            health = 700;
            itemDuration = 360f;
            powerProduction = 14f;
            consumes.item(Items.thorium);
            heating = 0.02f;
            consumes.liquid(Liquids.cryofluid, heating / coolantPower).update(false);
        }};

        impactReactor = new ImpactReactor("impact-reactor"){{
            requirements(Category.power, ItemStack.with(Items.lead, 500, Items.silicon, 300, Items.graphite, 400, Items.thorium, 100, Items.surgealloy, 250, Items.metaglass, 250));
            size = 4;
            health = 900;
            powerProduction = 130f;
            itemDuration = 140f;
            consumes.power(25f);
            consumes.item(Items.blastCompound);
            consumes.liquid(Liquids.cryofluid, 0.25f);
        }};

        //endregion power
        //region production

        mechanicalDrill = new Drill("mechanical-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 12), true);
            tier = 2;
            drillTime = 600;
            size = 2;
            drawMineItem = true;
            consumes.liquid(Liquids.water, 0.05f).boost();
        }};

        pneumaticDrill = new Drill("pneumatic-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 18, Items.graphite, 10));
            tier = 3;
            drillTime = 400;
            size = 2;
            drawMineItem = true;
            consumes.liquid(Liquids.water, 0.06f).boost();
        }};

        laserDrill = new Drill("laser-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 35, Items.graphite, 30, Items.silicon, 30, Items.titanium, 20));
            drillTime = 280;
            size = 3;
            hasPower = true;
            tier = 4;
            updateEffect = Fx.pulverizeMedium;
            drillEffect = Fx.mineBig;

            consumes.power(1.10f);
            consumes.liquid(Liquids.water, 0.08f).boost();
        }};

        blastDrill = new Drill("blast-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 65, Items.silicon, 60, Items.titanium, 50, Items.thorium, 75));
            drillTime = 280;
            size = 4;
            drawRim = true;
            hasPower = true;
            tier = 5;
            updateEffect = Fx.pulverizeRed;
            updateEffectChance = 0.03f;
            drillEffect = Fx.mineHuge;
            rotateSpeed = 6f;
            warmupSpeed = 0.01f;

            consumes.power(3f);
            consumes.liquid(Liquids.water, 0.1f).boost();
        }};

        waterExtractor = new SolidPump("water-extractor"){{
            requirements(Category.production, ItemStack.with(Items.copper, 25, Items.graphite, 25, Items.lead, 20));
            result = Liquids.water;
            pumpAmount = 0.13f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;
            attribute = Attribute.water;

            consumes.power(1f);
        }};

        cultivator = new Cultivator("cultivator"){{
            requirements(Category.production, ItemStack.with(Items.copper, 10, Items.lead, 25, Items.silicon, 10));
            outputItem = new ItemStack(Items.sporePod, 1);
            craftTime = 140;
            size = 2;
            hasLiquids = true;
            hasPower = true;
            hasItems = true;

            consumes.power(0.80f);
            consumes.liquid(Liquids.water, 0.18f);
        }};

        oilExtractor = new Fracker("oil-extractor"){{
            requirements(Category.production, ItemStack.with(Items.copper, 150, Items.graphite, 175, Items.lead, 115, Items.thorium, 115, Items.silicon, 75));
            result = Liquids.oil;
            updateEffect = Fx.pulverize;
            liquidCapacity = 50f;
            updateEffectChance = 0.05f;
            pumpAmount = 0.25f;
            size = 3;
            liquidCapacity = 30f;
            attribute = Attribute.oil;

            consumes.item(Items.sand);
            consumes.power(3f);
            consumes.liquid(Liquids.water, 0.15f);
        }};

        //endregion
        //region storage

        coreShard = new CoreBlock("core-shard"){{
            requirements(Category.effect, BuildVisibility.debugOnly, ItemStack.with());
            alwaysUnlocked = true;

            health = 1100;
            itemCapacity = 4000;
            size = 3;
        }};

        coreFoundation = new CoreBlock("core-foundation"){{
            requirements(Category.effect, BuildVisibility.debugOnly, ItemStack.with());

            health = 2000;
            itemCapacity = 9000;
            size = 4;
        }};

        coreNucleus = new CoreBlock("core-nucleus"){{
            requirements(Category.effect, BuildVisibility.debugOnly, ItemStack.with());

            health = 4000;
            itemCapacity = 13000;
            size = 5;
        }};

        vault = new StorageBlock("vault"){{
            requirements(Category.effect, ItemStack.with(Items.titanium, 250, Items.thorium, 125));
            size = 3;
            itemCapacity = 1000;
        }};

        container = new StorageBlock("container"){{
            requirements(Category.effect, ItemStack.with(Items.titanium, 100));
            size = 2;
            itemCapacity = 300;
        }};

        unloader = new Unloader("unloader"){{
            requirements(Category.effect, ItemStack.with(Items.titanium, 25, Items.silicon, 30));
            speed = 7f;
        }};

        launchPad = new LaunchPad("launch-pad"){{
            requirements(Category.effect, BuildVisibility.campaignOnly, ItemStack.with(Items.copper, 250, Items.silicon, 75, Items.lead, 100));
            size = 3;
            itemCapacity = 100;
            launchTime = 60f * 16;
            hasPower = true;
            consumes.power(1f);
        }};

        launchPadLarge = new LaunchPad("launch-pad-large"){{
            requirements(Category.effect, BuildVisibility.campaignOnly, ItemStack.with(Items.titanium, 200, Items.silicon, 150, Items.lead, 250, Items.plastanium, 75));
            size = 4;
            itemCapacity = 250;
            launchTime = 60f * 14;
            hasPower = true;
            consumes.power(2f);
        }};


        //endregion
        //region turrets

        duo = new ItemTurret("duo"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 35), true);
            ammo(
            Items.copper, Bullets.standardCopper,
            Items.graphite, Bullets.standardDense,
            Items.pyratite, Bullets.standardIncendiary,
            Items.silicon, Bullets.standardHoming
            );

            spread = 4f;
            shots = 2;
            alternate = true;
            reloadTime = 20f;
            restitution = 0.03f;
            range = 100;
            shootCone = 15f;
            ammoUseEffect = Fx.shellEjectSmall;
            health = 250;
            inaccuracy = 2f;
            rotatespeed = 10f;
        }};

        scatter = new BurstTurret("scatter"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 85, Items.lead, 45));
            ammo(
            Items.scrap, Bullets.flakScrap,
            Items.lead, Bullets.flakLead,
            Items.metaglass, Bullets.flakGlass
            );
            reloadTime = 18f;
            range = 170f;
            size = 2;
            burstSpacing = 5f;
            shots = 2;
            targetGround = false;

            recoilAmount = 2f;
            rotatespeed = 15f;
            inaccuracy = 17f;
            shootCone = 35f;

            health = 200 * size * size;
            shootSound = Sounds.shootSnap;
        }};

        scorch = new ItemTurret("scorch"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 25, Items.graphite, 22));
            ammo(
            Items.coal, Bullets.basicFlame,
            Items.pyratite, Bullets.pyraFlame
            );
            recoilAmount = 0f;
            reloadTime = 5f;
            coolantMultiplier = 2f;
            range = 60f;
            shootCone = 50f;
            targetAir = false;
            ammoUseEffect = Fx.none;
            health = 400;
            shootSound = Sounds.flame;
        }};

        hail = new ArtilleryTurret("hail"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 40, Items.graphite, 17));
            ammo(
            Items.graphite, Bullets.artilleryDense,
            Items.silicon, Bullets.artilleryHoming,
            Items.pyratite, Bullets.artilleryIncendiary
            );
            reloadTime = 60f;
            recoilAmount = 2f;
            range = 230f;
            inaccuracy = 1f;
            shootCone = 10f;
            health = 260;
            shootSound = Sounds.artillery;
        }};

        wave = new LiquidTurret("wave"){{
            requirements(Category.turret, ItemStack.with(Items.metaglass, 45, Items.lead, 75));
            ammo(
            Liquids.water, Bullets.waterShot,
            Liquids.slag, Bullets.slagShot,
            Liquids.cryofluid, Bullets.cryoShot,
            Liquids.oil, Bullets.oilShot
            );
            size = 2;
            recoilAmount = 0f;
            reloadTime = 2f;
            inaccuracy = 5f;
            shootCone = 50f;
            shootEffect = Fx.shootLiquid;
            range = 110f;
            health = 250 * size * size;
            shootSound = Sounds.splash;
        }};

        lancer = new ChargeTurret("lancer"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 25, Items.lead, 50, Items.silicon, 45));
            range = 155f;
            chargeTime = 50f;
            chargeMaxDelay = 30f;
            chargeEffects = 7;
            shootType = Bullets.lancerLaser;
            recoilAmount = 2f;
            reloadTime = 90f;
            cooldown = 0.03f;
            powerUse = 2.5f;
            shootShake = 2f;
            shootEffect = Fx.lancerLaserShoot;
            smokeEffect = Fx.lancerLaserShootSmoke;
            chargeEffect = Fx.lancerLaserCharge;
            chargeBeginEffect = Fx.lancerLaserChargeBegin;
            heatColor = Color.red;
            size = 2;
            health = 280 * size * size;
            targetAir = false;
            shootSound = Sounds.laser;
        }};

        arc = new PowerTurret("arc"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 35, Items.lead, 50));
            shootType = Bullets.arc;
            reloadTime = 35f;
            shootCone = 40f;
            rotatespeed = 8f;
            powerUse = 1.5f;
            targetAir = false;
            range = 90f;
            shootEffect = Fx.lightningShoot;
            heatColor = Color.red;
            recoilAmount = 1f;
            size = 1;
            health = 260;
            shootSound = Sounds.spark;
        }};

        swarmer = new BurstTurret("swarmer"){{
            requirements(Category.turret, ItemStack.with(Items.graphite, 35, Items.titanium, 35, Items.plastanium, 45, Items.silicon, 30));
            ammo(
            Items.blastCompound, Bullets.missileExplosive,
            Items.pyratite, Bullets.missileIncendiary,
            Items.surgealloy, Bullets.missileSurge
            );
            reloadTime = 40f;
            shots = 4;
            burstSpacing = 5;
            inaccuracy = 10f;
            range = 185f;
            xRand = 6f;
            size = 2;
            health = 300 * size * size;
            shootSound = Sounds.missile;
        }};

        salvo = new BurstTurret("salvo"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 105, Items.graphite, 95, Items.titanium, 60));
            ammo(
            Items.copper, Bullets.standardCopper,
            Items.graphite, Bullets.standardDense,
            Items.pyratite, Bullets.standardIncendiary,
            Items.silicon, Bullets.standardHoming,
            Items.thorium, Bullets.standardThorium
            );

            size = 2;
            range = 150f;
            reloadTime = 38f;
            restitution = 0.03f;
            ammoEjectBack = 3f;
            cooldown = 0.03f;
            recoilAmount = 3f;
            shootShake = 2f;
            burstSpacing = 3f;
            shots = 4;
            ammoUseEffect = Fx.shellEjectBig;
            health = 240 * size * size;
            shootSound = Sounds.shootBig;
        }};

        fuse = new ItemTurret("fuse"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 225, Items.graphite, 225, Items.thorium, 100));

            reloadTime = 35f;
            shootShake = 4f;
            range = 90f;
            recoilAmount = 5f;
            shots = 3;
            spread = 20f;
            restitution = 0.1f;
            shootCone = 30;
            size = 3;

            health = 220 * size * size;
            shootSound = Sounds.shotgun;

            ammo(Items.graphite, new BulletType(0.01f, 105){
                int rays = 1;
                float rayLength = range + 10f;

                {
                    hitEffect = Fx.hitLancer;
                    shootEffect = smokeEffect = Fx.lightningShoot;
                    lifetime = 10f;
                    despawnEffect = Fx.none;
                    pierce = true;
                }

                @Override
                public void init(Bulletc b){
                    for(int i = 0; i < rays; i++){
                        Damage.collideLine(b, b.team(), hitEffect, b.x(), b.y(), b.rotation(), rayLength - Math.abs(i - (rays / 2)) * 20f);
                    }
                }

                @Override
                public void draw(Bulletc b){
                    super.draw(b);
                    Draw.color(Color.white, Pal.lancerLaser, b.fin());
                    //Draw.alpha(b.fout());
                    for(int i = 0; i < 7; i++){
                        Tmp.v1.trns(b.rotation(), i * 8f);
                        float sl = Mathf.clamp(b.fout() - 0.5f) * (80f - i * 10);
                        Drawf.tri(b.x() + Tmp.v1.x, b.y() + Tmp.v1.y, 4f, sl, b.rotation() + 90);
                        Drawf.tri(b.x() + Tmp.v1.x, b.y() + Tmp.v1.y, 4f, sl, b.rotation() - 90);
                    }
                    Drawf.tri(b.x(), b.y(), 20f * b.fout(), (rayLength + 50), b.rotation());
                    Drawf.tri(b.x(), b.y(), 20f * b.fout(), 10f, b.rotation() + 180f);
                    Draw.reset();
                }
            });
        }};

        ripple = new ArtilleryTurret("ripple"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 150, Items.graphite, 135, Items.titanium, 60));
            ammo(
            Items.graphite, Bullets.artilleryDense,
            Items.silicon, Bullets.artilleryHoming,
            Items.pyratite, Bullets.artilleryIncendiary,
            Items.blastCompound, Bullets.artilleryExplosive,
            Items.plastanium, Bullets.artilleryPlastic
            );
            size = 3;
            shots = 4;
            inaccuracy = 12f;
            reloadTime = 60f;
            ammoEjectBack = 5f;
            ammoUseEffect = Fx.shellEjectBig;
            cooldown = 0.03f;
            velocityInaccuracy = 0.2f;
            restitution = 0.02f;
            recoilAmount = 6f;
            shootShake = 2f;
            range = 290f;

            health = 130 * size * size;
            shootSound = Sounds.artillery;
        }};

        cyclone = new ItemTurret("cyclone"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 200, Items.titanium, 125, Items.plastanium, 80));
            ammo(
            Items.metaglass, Bullets.flakGlass,
            Items.blastCompound, Bullets.flakExplosive,
            Items.plastanium, Bullets.flakPlastic,
            Items.surgealloy, Bullets.flakSurge
            );
            xRand = 4f;
            reloadTime = 6f;
            range = 200f;
            size = 3;
            recoilAmount = 3f;
            rotatespeed = 10f;
            inaccuracy = 10f;
            shootCone = 30f;
            shootSound = Sounds.shootSnap;

            health = 145 * size * size;
        }};

        spectre = new ItemTurret("spectre"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 350, Items.graphite, 300, Items.surgealloy, 250, Items.plastanium, 175, Items.thorium, 250));
            ammo(
            Items.graphite, Bullets.standardDenseBig,
            Items.pyratite, Bullets.standardIncendiaryBig,
            Items.thorium, Bullets.standardThoriumBig
            );
            reloadTime = 6f;
            coolantMultiplier = 0.5f;
            restitution = 0.1f;
            ammoUseEffect = Fx.shellEjectBig;
            range = 200f;
            inaccuracy = 3f;
            recoilAmount = 3f;
            spread = 8f;
            alternate = true;
            shootShake = 2f;
            shots = 2;
            size = 4;
            shootCone = 24f;
            shootSound = Sounds.shootBig;

            health = 155 * size * size;
            consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 2f)).update(false).optional(true, true);
        }};

        meltdown = new LaserTurret("meltdown"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 250, Items.lead, 350, Items.graphite, 300, Items.surgealloy, 325, Items.silicon, 325));
            shootType = Bullets.meltdownLaser;
            shootEffect = Fx.shootBigSmoke2;
            shootCone = 40f;
            recoilAmount = 4f;
            size = 4;
            shootShake = 2f;
            range = 190f;
            reloadTime = 80f;
            firingMoveFract = 0.5f;
            shootDuration = 220f;
            powerUse = 14f;
            shootSound = Sounds.laserbig;
            activeSound = Sounds.beam;
            activeSoundVolume = 2f;

            health = 200 * size * size;
            consumes.add(new ConsumeLiquidFilter(liquid -> liquid.temperature <= 0.5f && liquid.flammability < 0.1f, 0.5f)).update(false);
        }};

        //endregion
        //region units

        //for testing only.

        groundFactory = new UnitFactory("ground-factory"){{
            requirements(Category.units, ItemStack.with(Items.copper, 30, Items.lead, 70));
            plans = new UnitPlan[]{
                new UnitPlan(UnitTypes.dagger, 500f, ItemStack.with(Items.silicon, 10)),
                new UnitPlan(UnitTypes.titan, 800f, ItemStack.with(Items.silicon, 20, Items.titanium, 10)),
            };
            size = 3;
            consumes.power(1.2f);
        }};

        airFactory = new UnitFactory("air-factory"){{
            requirements(Category.units, ItemStack.with(Items.copper, 30, Items.lead, 70));
            plans = new UnitPlan[]{
                new UnitPlan(UnitTypes.wraith, 200f, ItemStack.with(Items.silicon, 10)),
                //new UnitPlan(UnitTypes.ghoul, 200f, ItemStack.with(Items.silicon, 10)),
            };
            size = 3;
            consumes.power(1.2f);
        }};

        navalFactory = new UnitFactory("naval-factory"){{
            requirements(Category.units, ItemStack.with(Items.copper, 30, Items.lead, 70));
            plans = new UnitPlan[]{
                new UnitPlan(UnitTypes.vanguard, 200f, ItemStack.with(Items.silicon, 10)),
            };
            size = 3;
            requiresWater = true;
            consumes.power(1.2f);
        }};

        repairPoint = new RepairPoint("repair-point"){{
            requirements(Category.units, ItemStack.with(Items.lead, 15, Items.copper, 15, Items.silicon, 15));
            repairSpeed = 0.5f;
            repairRadius = 65f;
            powerUse = 1f;
        }};

        //endregion
        //region sandbox

        powerSource = new PowerSource("power-source"){{
            requirements(Category.power, BuildVisibility.sandboxOnly, ItemStack.with());
            alwaysUnlocked = true;
        }};

        powerVoid = new PowerVoid("power-void"){{
            requirements(Category.power, BuildVisibility.sandboxOnly, ItemStack.with());
            alwaysUnlocked = true;
        }};

        itemSource = new ItemSource("item-source"){{
            requirements(Category.distribution, BuildVisibility.sandboxOnly, ItemStack.with());
            alwaysUnlocked = true;
        }};

        itemVoid = new ItemVoid("item-void"){{
            requirements(Category.distribution, BuildVisibility.sandboxOnly, ItemStack.with());
            alwaysUnlocked = true;
        }};

        liquidSource = new LiquidSource("liquid-source"){{
            requirements(Category.liquid, BuildVisibility.sandboxOnly, ItemStack.with());
            alwaysUnlocked = true;
        }};

        liquidVoid = new LiquidVoid("liquid-void"){{
            requirements(Category.liquid, BuildVisibility.sandboxOnly, ItemStack.with());
            alwaysUnlocked = true;
        }};

        message = new MessageBlock("message"){{
            requirements(Category.effect, ItemStack.with(Items.graphite, 5));
        }};

        illuminator = new LightBlock("illuminator"){{
            requirements(Category.effect, BuildVisibility.lightingOnly, ItemStack.with(Items.graphite, 4, Items.silicon, 2));
            brightness = 0.67f;
            radius = 120f;
            consumes.power(0.05f);
        }};

        //endregion
        //region legacy

        //looked up by name, no ref needed
        new LegacyMechPad("legacy-mech-pad");
        new LegacyUnitFactory("legacy-unit-factory");
        new LegacyCommandCenter("legacy-command-center");

        //endregion
        //region experimental

        blockForge = new BlockForge("block-forge"){{
            requirements(Category.production, BuildVisibility.debugOnly, ItemStack.with(Items.thorium, 100));
            hasPower = true;
            consumes.power(2f);
            size = 3;
        }};

        blockLauncher = new BlockLauncher("block-launcher"){{
            requirements(Category.production, BuildVisibility.debugOnly, ItemStack.with(Items.thorium, 100));
            size = 3;
            hasPower = true;
            consumes.power(2f);
        }};

        //endregion
    }
}
