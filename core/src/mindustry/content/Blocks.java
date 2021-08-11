package mindustry.content;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.type.ItemStack.*;

public class Blocks implements ContentList{
    public static Block

    //environment
    air, spawn, cliff, deepwater, water, taintedWater, tar, slag, stone, craters, charr, sand, darksand, dirt, mud, ice, snow, darksandTaintedWater, space,
    dacite,
    stoneWall, dirtWall, sporeWall, iceWall, daciteWall, sporePine, snowPine, pine, shrubs, whiteTree, whiteTreeDead, sporeCluster,
    iceSnow, sandWater, darksandWater, duneWall, sandWall, moss, sporeMoss, shale, shaleWall, shaleBoulder, sandBoulder, daciteBoulder, boulder, snowBoulder, basaltBoulder, grass, salt,
    metalFloor, metalFloorDamaged, metalFloor2, metalFloor3, metalFloor4, metalFloor5, basalt, magmarock, hotrock, snowWall, saltWall,
    darkPanel1, darkPanel2, darkPanel3, darkPanel4, darkPanel5, darkPanel6, darkMetal,
    pebbles, tendrils,

    //ores
    oreCopper, oreLead, oreScrap, oreCoal, oreTitanium, oreThorium,

    //crafting
    siliconSmelter, siliconCrucible, kiln, graphitePress, plastaniumCompressor, multiPress, phaseWeaver, surgeSmelter, pyratiteMixer, blastMixer, cryofluidMixer,
    melter, separator, disassembler, sporePress, pulverizer, incinerator, coalCentrifuge,

    //sandbox
    powerSource, powerVoid, itemSource, itemVoid, liquidSource, liquidVoid, payloadSource, payloadVoid, illuminator,

    //defense
    copperWall, copperWallLarge, titaniumWall, titaniumWallLarge, plastaniumWall, plastaniumWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
    phaseWall, phaseWallLarge, surgeWall, surgeWallLarge, mender, mendProjector, overdriveProjector, overdriveDome, forceProjector, shockMine,
    scrapWall, scrapWallLarge, scrapWallHuge, scrapWallGigantic, thruster, //ok, these names are getting ridiculous, but at least I don't have humongous walls yet

    //transport
    conveyor, titaniumConveyor, plastaniumConveyor, armoredConveyor, distributor, junction, itemBridge, phaseConveyor, sorter, invertedSorter, router,
    overflowGate, underflowGate, massDriver,
    duct, ductRouter, ductBridge,

    //liquid
    mechanicalPump, rotaryPump, thermalPump, conduit, pulseConduit, platedConduit, liquidRouter, liquidTank, liquidJunction, bridgeConduit, phaseConduit,

    //power
    combustionGenerator, thermalGenerator, steamGenerator, differentialGenerator, rtgGenerator, solarPanel, largeSolarPanel, thoriumReactor,
    impactReactor, battery, batteryLarge, powerNode, powerNodeLarge, surgeTower, diode,

    //production
    mechanicalDrill, pneumaticDrill, laserDrill, blastDrill, waterExtractor, oilExtractor, cultivator,

    //storage
    coreShard, coreFoundation, coreNucleus, vault, container, unloader,

    //turrets
    duo, scatter, scorch, hail, arc, wave, lancer, swarmer, salvo, fuse, ripple, cyclone, foreshadow, spectre, meltdown, segment, parallax, tsunami,

    //units
    commandCenter,
    groundFactory, airFactory, navalFactory,
    additiveReconstructor, multiplicativeReconstructor, exponentialReconstructor, tetrativeReconstructor,
    repairPoint, repairTurret,

    //payloads
    payloadConveyor, payloadRouter, payloadPropulsionTower,

    //logic
    message, switchBlock, microProcessor, logicProcessor, hyperProcessor, largeLogicDisplay, logicDisplay, memoryCell, memoryBank,

    //campaign
    launchPad, interplanetaryAccelerator,

    //misc experimental
    blockForge, blockLoader, blockUnloader
    ;

    @Override
    public void load(){
        //region environment

        air = new AirBlock("air");

        spawn = new SpawnBlock("spawn");

        cliff = new Cliff("cliff"){{
            inEditor = false;
            saveData = true;
        }};

        //Registers build blocks
        //no reference is needed here since they can be looked up by name later
        for(int i = 1; i <= Vars.maxBlockSize; i++){
            new ConstructBlock(i);
        }

        deepwater = new Floor("deep-water"){{
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            liquidMultiplier = 1.5f;
            isLiquid = true;
            status = StatusEffects.wet;
            statusDuration = 120f;
            drownTime = 140f;
            cacheLayer = CacheLayer.water;
            albedo = 0.5f;
        }};

        water = new Floor("shallow-water"){{
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
            attributes.set(Attribute.spores, 0.15f);
        }};

        darksandTaintedWater = new ShallowLiquid("darksand-tainted-water"){{
            speedMultiplier = 0.75f;
            statusDuration = 60f;
            albedo = 0.5f;
            attributes.set(Attribute.spores, 0.1f);
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

        slag = new Floor("molten-slag"){{
            drownTime = 150f;
            status = StatusEffects.melting;
            statusDuration = 240f;
            speedMultiplier = 0.19f;
            variants = 0;
            liquidDrop = Liquids.slag;
            isLiquid = true;
            cacheLayer = CacheLayer.slag;
            attributes.set(Attribute.heat, 0.85f);

            emitLight = true;
            lightRadius = 40f;
            lightColor = Color.orange.cpy().a(0.38f);
        }};

        space = new Floor("space"){{
            cacheLayer = CacheLayer.space;
            placeableOn = false;
            solid = true;
            variants = 0;
        }};

        stone = new Floor("stone");

        craters = new Floor("crater-stone"){{
            variants = 3;
            blendGroup = stone;
        }};

        charr = new Floor("char"){{
            blendGroup = stone;
        }};

        basalt = new Floor("basalt"){{
            attributes.set(Attribute.water, -0.25f);
        }};

        hotrock = new Floor("hotrock"){{
            attributes.set(Attribute.heat, 0.5f);
            attributes.set(Attribute.water, -0.5f);
            blendGroup = basalt;

            emitLight = true;
            lightRadius = 30f;
            lightColor = Color.orange.cpy().a(0.15f);
        }};

        magmarock = new Floor("magmarock"){{
            attributes.set(Attribute.heat, 0.75f);
            attributes.set(Attribute.water, -0.75f);
            blendGroup = basalt;

            emitLight = true;
            lightRadius = 50f;
            lightColor = Color.orange.cpy().a(0.3f);
        }};

        sand = new Floor("sand"){{
            itemDrop = Items.sand;
            playerUnmineable = true;
            attributes.set(Attribute.oil, 0.7f);
        }};

        darksand = new Floor("darksand"){{
            itemDrop = Items.sand;
            playerUnmineable = true;
            attributes.set(Attribute.oil, 1.5f);
        }};

        dirt = new Floor("dirt");

        mud = new Floor("mud"){{
            speedMultiplier = 0.6f;
            variants = 3;
            status = StatusEffects.muddy;
            statusDuration = 30f;
            attributes.set(Attribute.water, 1f);
            cacheLayer = CacheLayer.mud;
            albedo = 0.35f;
            walkSound = Sounds.mud;
            walkSoundVolume = 0.08f;
            walkSoundPitchMin = 0.4f;
            walkSoundPitchMax = 0.5f;
        }};

        ((ShallowLiquid)darksandTaintedWater).set(Blocks.taintedWater, Blocks.darksand);
        ((ShallowLiquid)sandWater).set(Blocks.water, Blocks.sand);
        ((ShallowLiquid)darksandWater).set(Blocks.water, Blocks.darksand);

        dacite = new Floor("dacite");

        grass = new Floor("grass"){{
            attributes.set(Attribute.water, 0.1f);
        }};

        salt = new Floor("salt"){{
            variants = 0;
            attributes.set(Attribute.water, -0.3f);
            attributes.set(Attribute.oil, 0.3f);
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

        shale = new Floor("shale"){{
            variants = 3;
            attributes.set(Attribute.oil, 1.6f);
        }};

        stoneWall = new StaticWall("stone-wall"){{
            variants = 2;
        }};

        sporeWall = new StaticWall("spore-wall"){{
            variants = 2;
        }};

        dirtWall = new StaticWall("dirt-wall"){{
            variants = 2;
        }};

        daciteWall = new StaticWall("dacite-wall"){{
            variants = 2;
        }};

        iceWall = new StaticWall("ice-wall"){{
            variants = 2;
            iceSnow.asFloor().wall = this;
        }};

        snowWall = new StaticWall("snow-wall"){{
            variants = 2;
        }};

        duneWall = new StaticWall("dune-wall"){{
            variants = 2;
            basalt.asFloor().wall = darksandWater.asFloor().wall = darksandTaintedWater.asFloor().wall = this;
        }};

        sandWall = new StaticWall("sand-wall"){{
            variants = 2;
            sandWater.asFloor().wall = water.asFloor().wall = deepwater.asFloor().wall = this;
        }};

        saltWall = new StaticWall("salt-wall");

        shrubs = new StaticWall("shrubs");

        shaleWall = new StaticWall("shale-wall"){{
            variants = 2;
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

        whiteTreeDead = new TreeBlock("white-tree-dead");

        whiteTree = new TreeBlock("white-tree");

        sporeCluster = new Prop("spore-cluster"){{
            variants = 3;
            breakSound = Sounds.plantBreak;
        }};

        boulder = new Prop("boulder"){{
            variants = 2;
            stone.asFloor().decoration = this;
        }};

        snowBoulder = new Prop("snow-boulder"){{
            variants = 2;
            snow.asFloor().decoration = ice.asFloor().decoration = iceSnow.asFloor().decoration = salt.asFloor().decoration = this;
        }};

        shaleBoulder = new Prop("shale-boulder"){{
            variants = 2;
        }};

        sandBoulder = new Prop("sand-boulder"){{
            variants = 2;
        }};

        daciteBoulder = new Prop("dacite-boulder"){{
            variants = 2;
        }};

        basaltBoulder = new Prop("basalt-boulder"){{
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
            wall = sporeWall;
        }};

        metalFloor = new Floor("metal-floor", 0);
        metalFloorDamaged = new Floor("metal-floor-damaged", 3);

        metalFloor2 = new Floor("metal-floor-2", 0);
        metalFloor3 = new Floor("metal-floor-3", 0);
        metalFloor4 = new Floor("metal-floor-4", 0);
        metalFloor5 = new Floor("metal-floor-5", 0);

        darkPanel1 = new Floor("dark-panel-1", 0);
        darkPanel2 = new Floor("dark-panel-2", 0);
        darkPanel3 = new Floor("dark-panel-3", 0);
        darkPanel4 = new Floor("dark-panel-4", 0);
        darkPanel5 = new Floor("dark-panel-5", 0);
        darkPanel6 = new Floor("dark-panel-6", 0);

        darkMetal = new StaticWall("dark-metal");

        Seq.with(metalFloor, metalFloorDamaged, metalFloor2, metalFloor3, metalFloor4, metalFloor5, darkPanel1, darkPanel2, darkPanel3, darkPanel4, darkPanel5, darkPanel6)
        .each(b -> b.asFloor().wall = darkMetal);

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
            requirements(Category.crafting, with(Items.copper, 75, Items.lead, 30));

            craftEffect = Fx.pulverizeMedium;
            outputItem = new ItemStack(Items.graphite, 1);
            craftTime = 90f;
            size = 2;
            hasItems = true;

            consumes.item(Items.coal, 2);
        }};

        multiPress = new GenericCrafter("multi-press"){{
            requirements(Category.crafting, with(Items.titanium, 100, Items.silicon, 25, Items.lead, 100, Items.graphite, 50));

            craftEffect = Fx.pulverizeMedium;
            outputItem = new ItemStack(Items.graphite, 2);
            craftTime = 30f;
            itemCapacity = 20;
            size = 3;
            hasItems = true;
            hasLiquids = true;
            hasPower = true;

            consumes.power(1.8f);
            consumes.item(Items.coal, 3);
            consumes.liquid(Liquids.water, 0.1f);
        }};

        siliconSmelter = new GenericCrafter("silicon-smelter"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 25));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.silicon, 1);
            craftTime = 40f;
            size = 2;
            hasPower = true;
            hasLiquids = false;
            drawer = new DrawSmelter(Color.valueOf("ffef99"));

            consumes.items(with(Items.coal, 1, Items.sand, 2));
            consumes.power(0.50f);
        }};

        siliconCrucible = new AttributeCrafter("silicon-crucible"){{
            requirements(Category.crafting, with(Items.titanium, 120, Items.metaglass, 80, Items.plastanium, 35, Items.silicon, 60));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.silicon, 8);
            craftTime = 90f;
            size = 3;
            hasPower = true;
            hasLiquids = false;
            itemCapacity = 30;
            boostScale = 0.15f;
            drawer = new DrawSmelter(Color.valueOf("ffef99"));

            consumes.items(with(Items.coal, 4, Items.sand, 6, Items.pyratite, 1));
            consumes.power(4f);
        }};

        kiln = new GenericCrafter("kiln"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.graphite, 30, Items.lead, 30));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.metaglass, 1);
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = true;
            drawer = new DrawSmelter(Color.valueOf("ffc099"));

            consumes.items(with(Items.lead, 1, Items.sand, 1));
            consumes.power(0.60f);
        }};

        plastaniumCompressor = new GenericCrafter("plastanium-compressor"){{
            requirements(Category.crafting, with(Items.silicon, 80, Items.lead, 115, Items.graphite, 60, Items.titanium, 80));
            hasItems = true;
            liquidCapacity = 60f;
            craftTime = 60f;
            outputItem = new ItemStack(Items.plastanium, 1);
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = Fx.formsmoke;
            updateEffect = Fx.plasticburn;
            drawer = new DrawGlow();

            consumes.liquid(Liquids.oil, 0.25f);
            consumes.power(3f);
            consumes.item(Items.titanium, 2);
        }};

        phaseWeaver = new GenericCrafter("phase-weaver"){{
            requirements(Category.crafting, with(Items.silicon, 130, Items.lead, 120, Items.thorium, 75));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.phaseFabric, 1);
            craftTime = 120f;
            size = 2;
            hasPower = true;
            drawer = new DrawWeave();

            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;

            consumes.items(with(Items.thorium, 4, Items.sand, 10));
            consumes.power(5f);
            itemCapacity = 20;
        }};

        surgeSmelter = new GenericCrafter("alloy-smelter"){{
            requirements(Category.crafting, with(Items.silicon, 80, Items.lead, 80, Items.thorium, 70));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.surgeAlloy, 1);
            craftTime = 75f;
            size = 3;
            hasPower = true;
            itemCapacity = 20;
            drawer = new DrawSmelter();

            consumes.power(4f);
            consumes.items(with(Items.copper, 3, Items.lead, 4, Items.titanium, 2, Items.silicon, 3));
        }};

        cryofluidMixer = new LiquidConverter("cryofluid-mixer"){{
            requirements(Category.crafting, with(Items.lead, 65, Items.silicon, 40, Items.titanium, 60));
            outputLiquid = new LiquidStack(Liquids.cryofluid, 0.2f);
            craftTime = 120f;
            size = 2;
            hasPower = true;
            hasItems = true;
            hasLiquids = true;
            rotate = false;
            solid = true;
            outputsLiquid = true;
            drawer = new DrawMixer();

            consumes.power(1f);
            consumes.item(Items.titanium);
            consumes.liquid(Liquids.water, 0.2f);
        }};

        pyratiteMixer = new GenericCrafter("pyratite-mixer"){{
            requirements(Category.crafting, with(Items.copper, 50, Items.lead, 25));
            hasItems = true;
            hasPower = true;
            outputItem = new ItemStack(Items.pyratite, 1);

            size = 2;

            consumes.power(0.20f);
            consumes.items(with(Items.coal, 1, Items.lead, 2, Items.sand, 2));
        }};

        blastMixer = new GenericCrafter("blast-mixer"){{
            requirements(Category.crafting, with(Items.lead, 30, Items.titanium, 20));
            hasItems = true;
            hasPower = true;
            outputItem = new ItemStack(Items.blastCompound, 1);
            size = 2;

            consumes.items(with(Items.pyratite, 1, Items.sporePod, 1));
            consumes.power(0.40f);
        }};

        melter = new GenericCrafter("melter"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 35, Items.graphite, 45));
            health = 200;
            outputLiquid = new LiquidStack(Liquids.slag, 2f);
            craftTime = 10f;
            hasLiquids = hasPower = true;
            drawer = new DrawLiquid();

            consumes.power(1f);
            consumes.item(Items.scrap, 1);
        }};

        separator = new Separator("separator"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.titanium, 25));
            results = with(
                Items.copper, 5,
                Items.lead, 3,
                Items.graphite, 2,
                Items.titanium, 2
            );
            hasPower = true;
            craftTime = 35f;
            size = 2;

            consumes.power(1.1f);
            consumes.liquid(Liquids.slag, 4f / 60f);
        }};

        disassembler = new Separator("disassembler"){{
            requirements(Category.crafting, with(Items.plastanium, 40, Items.titanium, 100, Items.silicon, 150, Items.thorium, 80));
            results = with(
                Items.sand, 4,
                Items.graphite, 2,
                Items.titanium, 2,
                Items.thorium, 2
            );
            hasPower = true;
            craftTime = 15f;
            size = 3;
            itemCapacity = 20;

            consumes.power(4f);
            consumes.item(Items.scrap);
            consumes.liquid(Liquids.slag, 0.12f);
        }};

        sporePress = new GenericCrafter("spore-press"){{
            requirements(Category.crafting, with(Items.lead, 35, Items.silicon, 30));
            liquidCapacity = 60f;
            craftTime = 20f;
            outputLiquid = new LiquidStack(Liquids.oil, 6f);
            size = 2;
            health = 320;
            hasLiquids = true;
            hasPower = true;
            craftEffect = Fx.none;
            drawer = new DrawAnimation();

            consumes.item(Items.sporePod, 1);
            consumes.power(0.7f);
        }};

        pulverizer = new GenericCrafter("pulverizer"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 25));
            outputItem = new ItemStack(Items.sand, 1);
            craftEffect = Fx.pulverize;
            craftTime = 40f;
            updateEffect = Fx.pulverizeSmall;
            hasItems = hasPower = true;
            drawer = new DrawRotator(){{
                drawSpinSprite = true;
            }};
            ambientSound = Sounds.grinding;
            ambientSoundVolume = 0.025f;

            consumes.item(Items.scrap, 1);
            consumes.power(0.50f);
        }};

        coalCentrifuge = new GenericCrafter("coal-centrifuge"){{
            requirements(Category.crafting, with(Items.titanium, 20, Items.graphite, 40, Items.lead, 30));
            craftEffect = Fx.coalSmeltsmoke;
            outputItem = new ItemStack(Items.coal, 1);
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = hasLiquids = true;

            consumes.liquid(Liquids.oil, 0.1f);
            consumes.power(0.7f);
        }};

        incinerator = new Incinerator("incinerator"){{
            requirements(Category.crafting, with(Items.graphite, 5, Items.lead, 15));
            health = 90;
            consumes.power(0.50f);
        }};

        //endregion
        //region defense

        int wallHealthMultiplier = 4;

        copperWall = new Wall("copper-wall"){{
            requirements(Category.defense, with(Items.copper, 6));
            health = 80 * wallHealthMultiplier;
        }};

        copperWallLarge = new Wall("copper-wall-large"){{
            requirements(Category.defense, ItemStack.mult(copperWall.requirements, 4));
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        titaniumWall = new Wall("titanium-wall"){{
            requirements(Category.defense, with(Items.titanium, 6));
            health = 110 * wallHealthMultiplier;
        }};

        titaniumWallLarge = new Wall("titanium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(titaniumWall.requirements, 4));
            health = 110 * wallHealthMultiplier * 4;
            size = 2;
        }};

        plastaniumWall = new Wall("plastanium-wall"){{
            requirements(Category.defense, with(Items.plastanium, 5, Items.metaglass, 2));
            health = 125 * wallHealthMultiplier;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
        }};

        plastaniumWallLarge = new Wall("plastanium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(plastaniumWall.requirements, 4));
            health = 125 * wallHealthMultiplier * 4;
            size = 2;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
        }};

        thoriumWall = new Wall("thorium-wall"){{
            requirements(Category.defense, with(Items.thorium, 6));
            health = 200 * wallHealthMultiplier;
        }};

        thoriumWallLarge = new Wall("thorium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(thoriumWall.requirements, 4));
            health = 200 * wallHealthMultiplier * 4;
            size = 2;
        }};

        phaseWall = new Wall("phase-wall"){{
            requirements(Category.defense, with(Items.phaseFabric, 6));
            health = 150 * wallHealthMultiplier;
            chanceDeflect = 10f;
            flashHit = true;
        }};

        phaseWallLarge = new Wall("phase-wall-large"){{
            requirements(Category.defense, ItemStack.mult(phaseWall.requirements, 4));
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
            chanceDeflect = 10f;
            flashHit = true;
        }};

        surgeWall = new Wall("surge-wall"){{
            requirements(Category.defense, with(Items.surgeAlloy, 6));
            health = 230 * wallHealthMultiplier;
            lightningChance = 0.05f;
        }};

        surgeWallLarge = new Wall("surge-wall-large"){{
            requirements(Category.defense, ItemStack.mult(surgeWall.requirements, 4));
            health = 230 * 4 * wallHealthMultiplier;
            size = 2;
            lightningChance = 0.05f;
        }};

        door = new Door("door"){{
            requirements(Category.defense, with(Items.titanium, 6, Items.silicon, 4));
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
            requirements(Category.defense, BuildVisibility.sandboxOnly, with(Items.scrap, 6));
            health = 60 * wallHealthMultiplier;
            variants = 5;
        }};

        scrapWallLarge = new Wall("scrap-wall-large"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.mult(scrapWall.requirements, 4));
            health = 60 * 4 * wallHealthMultiplier;
            size = 2;
            variants = 4;
        }};

        scrapWallHuge = new Wall("scrap-wall-huge"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.mult(scrapWall.requirements, 9));
            health = 60 * 9 * wallHealthMultiplier;
            size = 3;
            variants = 3;
        }};

        scrapWallGigantic = new Wall("scrap-wall-gigantic"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.mult(scrapWall.requirements, 16));
            health = 60 * 16 * wallHealthMultiplier;
            size = 4;
        }};

        thruster = new Thruster("thruster"){{
            health = 55 * 16 * wallHealthMultiplier;
            size = 4;
        }};

        mender = new MendProjector("mender"){{
            requirements(Category.effect, with(Items.lead, 30, Items.copper, 25));
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
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 25, Items.silicon, 40));
            consumes.power(1.5f);
            size = 2;
            reload = 250f;
            range = 85f;
            healPercent = 11f;
            phaseBoost = 15f;
            health = 80 * size * size;
            consumes.item(Items.phaseFabric).boost();
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 75, Items.plastanium, 30));
            consumes.power(3.50f);
            size = 2;
            consumes.item(Items.phaseFabric).boost();
        }};

        overdriveDome = new OverdriveProjector("overdrive-dome"){{
            requirements(Category.effect, with(Items.lead, 200, Items.titanium, 130, Items.silicon, 130, Items.plastanium, 80, Items.surgeAlloy, 120));
            consumes.power(10f);
            size = 3;
            range = 200f;
            speedBoost = 2.5f;
            useTime = 300f;
            hasBoost = false;
            consumes.items(with(Items.phaseFabric, 1, Items.silicon, 1));
        }};

        forceProjector = new ForceProjector("force-projector"){{
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 125));
            size = 3;
            phaseRadiusBoost = 80f;
            radius = 101.7f;
            shieldHealth = 750f;
            cooldownNormal = 1.5f;
            cooldownLiquid = 1.2f;
            cooldownBrokenBase = 0.35f;

            consumes.item(Items.phaseFabric).boost();
            consumes.power(4f);
        }};

        shockMine = new ShockMine("shock-mine"){{
            requirements(Category.effect, with(Items.lead, 25, Items.silicon, 12));
            hasShadow = false;
            health = 50;
            damage = 25;
            tileDamage = 7f;
            length = 10;
            tendrils = 4;
        }};

        //endregion
        //region distribution

        conveyor = new Conveyor("conveyor"){{
            requirements(Category.distribution, with(Items.copper, 1), true);
            health = 45;
            speed = 0.03f;
            displayedSpeed = 4.2f;
            buildCostMultiplier = 2f;
        }};

        titaniumConveyor = new Conveyor("titanium-conveyor"){{
            requirements(Category.distribution, with(Items.copper, 1, Items.lead, 1, Items.titanium, 1));
            health = 65;
            speed = 0.08f;
            displayedSpeed = 11f;
        }};

        plastaniumConveyor = new StackConveyor("plastanium-conveyor"){{
            requirements(Category.distribution, with(Items.plastanium, 1, Items.silicon, 1, Items.graphite, 1));
            health = 75;
            speed = 4f / 60f;
            itemCapacity = 10;
        }};

        armoredConveyor = new ArmoredConveyor("armored-conveyor"){{
            requirements(Category.distribution, with(Items.plastanium, 1, Items.thorium, 1, Items.metaglass, 1));
            health = 180;
            speed = 0.08f;
            displayedSpeed = 11f;
        }};

        junction = new Junction("junction"){{
            requirements(Category.distribution, with(Items.copper, 2), true);
            speed = 26;
            capacity = 6;
            health = 30;
            buildCostMultiplier = 6f;
        }};

        itemBridge = new BufferedItemBridge("bridge-conveyor"){{
            requirements(Category.distribution, with(Items.lead, 6, Items.copper, 6));
            fadeIn = moveArrows = false;
            range = 4;
            speed = 74f;
            arrowSpacing = 6f;
            bufferCapacity = 14;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            requirements(Category.distribution, with(Items.phaseFabric, 5, Items.silicon, 7, Items.lead, 10, Items.graphite, 10));
            range = 12;
            arrowPeriod = 0.9f;
            arrowTimeScl = 2.75f;
            hasPower = true;
            pulse = true;
            consumes.power(0.30f);
        }};

        sorter = new Sorter("sorter"){{
            requirements(Category.distribution, with(Items.lead, 2, Items.copper, 2));
            buildCostMultiplier = 3f;
        }};

        invertedSorter = new Sorter("inverted-sorter"){{
            requirements(Category.distribution, with(Items.lead, 2, Items.copper, 2));
            buildCostMultiplier = 3f;
            invert = true;
        }};

        router = new Router("router"){{
            requirements(Category.distribution, with(Items.copper, 3));
            buildCostMultiplier = 4f;
        }};

        distributor = new Router("distributor"){{
            requirements(Category.distribution, with(Items.lead, 4, Items.copper, 4));
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate"){{
            requirements(Category.distribution, with(Items.lead, 2, Items.copper, 4));
            buildCostMultiplier = 3f;
        }};

        underflowGate = new OverflowGate("underflow-gate"){{
            requirements(Category.distribution, with(Items.lead, 2, Items.copper, 4));
            buildCostMultiplier = 3f;
            invert = true;
        }};

        massDriver = new MassDriver("mass-driver"){{
            requirements(Category.distribution, with(Items.titanium, 125, Items.silicon, 75, Items.lead, 125, Items.thorium, 50));
            size = 3;
            itemCapacity = 120;
            reloadTime = 200f;
            range = 440f;
            consumes.power(1.75f);
        }};

        //special transport blocks

        duct = new Duct("duct"){{
            requirements(Category.distribution, BuildVisibility.debugOnly, with(Items.graphite, 5, Items.metaglass, 2));
            speed = 4f;
        }};

        ductRouter = new DuctRouter("duct-router"){{
            requirements(Category.distribution, BuildVisibility.debugOnly, with(Items.graphite, 10, Items.metaglass, 4));
            speed = 4f;
        }};

        ductBridge = new DuctBridge("duct-bridge"){{
            requirements(Category.distribution, BuildVisibility.debugOnly, with(Items.graphite, 20, Items.metaglass, 8));
            speed = 4f;
        }};

        //endregion
        //region liquid

        mechanicalPump = new Pump("mechanical-pump"){{
            requirements(Category.liquid, with(Items.copper, 15, Items.metaglass, 10));
            pumpAmount = 7f / 60f;
        }};

        rotaryPump = new Pump("rotary-pump"){{
            requirements(Category.liquid, with(Items.copper, 70, Items.metaglass, 50, Items.silicon, 20, Items.titanium, 35));
            pumpAmount = 0.2f;
            consumes.power(0.3f);
            liquidCapacity = 30f;
            hasPower = true;
            size = 2;
        }};

        thermalPump = new Pump("thermal-pump"){{
            requirements(Category.liquid, with(Items.copper, 80, Items.metaglass, 90, Items.silicon, 30, Items.titanium, 40, Items.thorium, 35));
            pumpAmount = 0.22f;
            consumes.power(1.3f);
            liquidCapacity = 40f;
            hasPower = true;
            size = 3;
        }};

        conduit = new Conduit("conduit"){{
            requirements(Category.liquid, with(Items.metaglass, 1));
            health = 45;
        }};

        pulseConduit = new Conduit("pulse-conduit"){{
            requirements(Category.liquid, with(Items.titanium, 2, Items.metaglass, 1));
            liquidCapacity = 16f;
            liquidPressure = 1.025f;
            health = 90;
        }};

        platedConduit = new ArmoredConduit("plated-conduit"){{
            requirements(Category.liquid, with(Items.thorium, 2, Items.metaglass, 1, Items.plastanium, 1));
            liquidCapacity = 16f;
            liquidPressure = 1.025f;
            health = 220;
        }};

        liquidRouter = new LiquidRouter("liquid-router"){{
            requirements(Category.liquid, with(Items.graphite, 4, Items.metaglass, 2));
            liquidCapacity = 20f;
        }};

        liquidTank = new LiquidRouter("liquid-tank"){{
            requirements(Category.liquid, with(Items.titanium, 25, Items.metaglass, 25));
            size = 3;
            liquidCapacity = 1500f;
            health = 500;
        }};

        liquidJunction = new LiquidJunction("liquid-junction"){{
            requirements(Category.liquid, with(Items.graphite, 2, Items.metaglass, 2));
        }};

        bridgeConduit = new LiquidBridge("bridge-conduit"){{
            requirements(Category.liquid, with(Items.graphite, 4, Items.metaglass, 8));
            fadeIn = moveArrows = false;
            arrowSpacing = 6f;
            range = 4;
            hasPower = false;
        }};

        phaseConduit = new LiquidBridge("phase-conduit"){{
            requirements(Category.liquid, with(Items.phaseFabric, 5, Items.silicon, 7, Items.metaglass, 20, Items.titanium, 10));
            range = 12;
            arrowPeriod = 0.9f;
            arrowTimeScl = 2.75f;
            hasPower = true;
            canOverdrive = false;
            pulse = true;
            consumes.power(0.30f);
        }};

        //endregion
        //region power

        powerNode = new PowerNode("power-node"){{
            requirements(Category.power, with(Items.copper, 1, Items.lead, 3));
            maxNodes = 10;
            laserRange = 6;
        }};

        powerNodeLarge = new PowerNode("power-node-large"){{
            requirements(Category.power, with(Items.titanium, 5, Items.lead, 10, Items.silicon, 3));
            size = 2;
            maxNodes = 15;
            laserRange = 9.5f;
        }};

        surgeTower = new PowerNode("surge-tower"){{
            requirements(Category.power, with(Items.titanium, 7, Items.lead, 10, Items.silicon, 15, Items.surgeAlloy, 15));
            size = 2;
            maxNodes = 2;
            laserRange = 40f;
            schematicPriority = -15;
        }};

        diode = new PowerDiode("diode"){{
            requirements(Category.power, with(Items.silicon, 10, Items.plastanium, 5, Items.metaglass, 10));
        }};

        battery = new Battery("battery"){{
            requirements(Category.power, with(Items.copper, 5, Items.lead, 20));
            consumes.powerBuffered(4000f);
        }};

        batteryLarge = new Battery("battery-large"){{
            requirements(Category.power, with(Items.titanium, 20, Items.lead, 40, Items.silicon, 20));
            size = 3;
            consumes.powerBuffered(50000f);
        }};

        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            requirements(Category.power, with(Items.copper, 25, Items.lead, 15));
            powerProduction = 1f;
            itemDuration = 120f;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.03f;
        }};

        thermalGenerator = new ThermalGenerator("thermal-generator"){{
            requirements(Category.power, with(Items.copper, 40, Items.graphite, 35, Items.lead, 50, Items.silicon, 35, Items.metaglass, 40));
            powerProduction = 1.8f;
            generateEffect = Fx.redgeneratespark;
            effectChance = 0.011f;
            size = 2;
            floating = true;
            ambientSound = Sounds.hum;
            ambientSoundVolume = 0.06f;
        }};

        steamGenerator = new BurnerGenerator("steam-generator"){{
            requirements(Category.power, with(Items.copper, 35, Items.graphite, 25, Items.lead, 40, Items.silicon, 30));
            powerProduction = 5.5f;
            itemDuration = 90f;
            consumes.liquid(Liquids.water, 0.1f);
            hasLiquids = true;
            size = 2;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.06f;
        }};

        differentialGenerator = new SingleTypeGenerator("differential-generator"){{
            requirements(Category.power, with(Items.copper, 70, Items.titanium, 50, Items.lead, 100, Items.silicon, 65, Items.metaglass, 50));
            powerProduction = 18f;
            itemDuration = 220f;
            hasLiquids = true;
            hasItems = true;
            size = 3;
            ambientSound = Sounds.steam;
            ambientSoundVolume = 0.03f;

            consumes.item(Items.pyratite).optional(true, false);
            consumes.liquid(Liquids.cryofluid, 0.1f);
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            requirements(Category.power, with(Items.lead, 100, Items.silicon, 75, Items.phaseFabric, 25, Items.plastanium, 75, Items.thorium, 50));
            size = 2;
            powerProduction = 4.5f;
            itemDuration = 60 * 14f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            requirements(Category.power, with(Items.lead, 10, Items.silicon, 15));
            powerProduction = 0.1f;
        }};

        largeSolarPanel = new SolarGenerator("solar-panel-large"){{
            requirements(Category.power, with(Items.lead, 80, Items.silicon, 110, Items.phaseFabric, 15));
            size = 3;
            powerProduction = 1.3f;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            requirements(Category.power, with(Items.lead, 300, Items.silicon, 200, Items.graphite, 150, Items.thorium, 150, Items.metaglass, 50));
            ambientSound = Sounds.hum;
            ambientSoundVolume = 0.24f;
            size = 3;
            health = 700;
            itemDuration = 360f;
            powerProduction = 15f;
            consumes.item(Items.thorium);
            heating = 0.02f;
            consumes.liquid(Liquids.cryofluid, heating / coolantPower).update(false);
        }};

        impactReactor = new ImpactReactor("impact-reactor"){{
            requirements(Category.power, with(Items.lead, 500, Items.silicon, 300, Items.graphite, 400, Items.thorium, 100, Items.surgeAlloy, 250, Items.metaglass, 250));
            size = 4;
            health = 900;
            powerProduction = 130f;
            itemDuration = 140f;
            ambientSound = Sounds.pulse;
            ambientSoundVolume = 0.07f;

            consumes.power(25f);
            consumes.item(Items.blastCompound);
            consumes.liquid(Liquids.cryofluid, 0.25f);
        }};

        //endregion power
        //region production

        mechanicalDrill = new Drill("mechanical-drill"){{
            requirements(Category.production, with(Items.copper, 12), true);
            tier = 2;
            drillTime = 600;
            size = 2;

            consumes.liquid(Liquids.water, 0.05f).boost();
        }};

        pneumaticDrill = new Drill("pneumatic-drill"){{
            requirements(Category.production, with(Items.copper, 18, Items.graphite, 10));
            tier = 3;
            drillTime = 400;
            size = 2;

            consumes.liquid(Liquids.water, 0.06f).boost();
        }};

        laserDrill = new Drill("laser-drill"){{
            requirements(Category.production, with(Items.copper, 35, Items.graphite, 30, Items.silicon, 30, Items.titanium, 20));
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
            requirements(Category.production, with(Items.copper, 65, Items.silicon, 60, Items.titanium, 50, Items.thorium, 75));
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
            itemCapacity = 20;

            //more than the laser drill
            liquidBoostIntensity = 1.8f;

            consumes.power(3f);
            consumes.liquid(Liquids.water, 0.1f).boost();
        }};

        waterExtractor = new SolidPump("water-extractor"){{
            requirements(Category.production, with(Items.metaglass, 30, Items.graphite, 30, Items.lead, 30));
            result = Liquids.water;
            pumpAmount = 0.11f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;
            attribute = Attribute.water;
            envRequired |= Env.groundWater;

            consumes.power(1.5f);
        }};

        cultivator = new AttributeCrafter("cultivator"){{
            requirements(Category.production, with(Items.copper, 25, Items.lead, 25, Items.silicon, 10));
            outputItem = new ItemStack(Items.sporePod, 1);
            craftTime = 100;
            size = 2;
            hasLiquids = true;
            hasPower = true;
            hasItems = true;

            craftEffect = Fx.none;
            envRequired |= Env.spores;
            attribute = Attribute.spores;

            legacyReadWarmup = true;
            drawer = new DrawCultivator();
            maxBoost = 2f;

            consumes.power(80f / 60f);
            consumes.liquid(Liquids.water, 18f / 60f);
        }};

        oilExtractor = new Fracker("oil-extractor"){{
            requirements(Category.production, with(Items.copper, 150, Items.graphite, 175, Items.lead, 115, Items.thorium, 115, Items.silicon, 75));
            result = Liquids.oil;
            updateEffect = Fx.pulverize;
            updateEffectChance = 0.05f;
            pumpAmount = 0.25f;
            size = 3;
            liquidCapacity = 30f;
            attribute = Attribute.oil;
            baseEfficiency = 0f;
            itemUseTime = 60f;

            consumes.item(Items.sand);
            consumes.power(3f);
            consumes.liquid(Liquids.water, 0.15f);
        }};

        //endregion
        //region storage

        coreShard = new CoreBlock("core-shard"){{
            requirements(Category.effect, BuildVisibility.editorOnly, with(Items.copper, 1000, Items.lead, 800));
            alwaysUnlocked = true;

            unitType = UnitTypes.alpha;
            health = 1100;
            itemCapacity = 4000;
            size = 3;

            unitCapModifier = 8;
        }};

        coreFoundation = new CoreBlock("core-foundation"){{
            requirements(Category.effect, with(Items.copper, 3000, Items.lead, 3000, Items.silicon, 2000));

            unitType = UnitTypes.beta;
            health = 3500;
            itemCapacity = 9000;
            size = 4;
            thrusterLength = 34/4f;

            unitCapModifier = 16;
            researchCostMultiplier = 0.07f;
        }};

        coreNucleus = new CoreBlock("core-nucleus"){{
            requirements(Category.effect, with(Items.copper, 8000, Items.lead, 8000, Items.silicon, 5000, Items.thorium, 4000));

            unitType = UnitTypes.gamma;
            health = 6000;
            itemCapacity = 13000;
            size = 5;
            thrusterLength = 40/4f;

            unitCapModifier = 24;
            researchCostMultiplier = 0.11f;
        }};

        vault = new StorageBlock("vault"){{
            requirements(Category.effect, with(Items.titanium, 250, Items.thorium, 125));
            size = 3;
            itemCapacity = 1000;
            health = size * size * 55;
        }};

        container = new StorageBlock("container"){{
            requirements(Category.effect, with(Items.titanium, 100));
            size = 2;
            itemCapacity = 300;
            health = size * size * 55;
        }};

        unloader = new Unloader("unloader"){{
            requirements(Category.effect, with(Items.titanium, 25, Items.silicon, 30));
            speed = 60f / 11f;
            group = BlockGroup.transportation;
        }};

        //endregion
        //region turrets

        duo = new ItemTurret("duo"){{
            requirements(Category.turret, with(Items.copper, 35), true);
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
            range = 110;
            shootCone = 15f;
            ammoUseEffect = Fx.casing1;
            health = 250;
            inaccuracy = 2f;
            rotateSpeed = 10f;

            limitRange();
        }};

        scatter = new ItemTurret("scatter"){{
            requirements(Category.turret, with(Items.copper, 85, Items.lead, 45));
            ammo(
                Items.scrap, Bullets.flakScrap,
                Items.lead, Bullets.flakLead,
                Items.metaglass, Bullets.flakGlass
            );
            reloadTime = 18f;
            range = 220f;
            size = 2;
            burstSpacing = 5f;
            shots = 2;
            targetGround = false;

            recoilAmount = 2f;
            rotateSpeed = 15f;
            inaccuracy = 17f;
            shootCone = 35f;

            health = 200 * size * size;
            shootSound = Sounds.shootSnap;

            limitRange(2);
        }};

        scorch = new ItemTurret("scorch"){{
            requirements(Category.turret, with(Items.copper, 25, Items.graphite, 22));
            ammo(
                Items.coal, Bullets.basicFlame,
                Items.pyratite, Bullets.pyraFlame
            );
            recoilAmount = 0f;
            reloadTime = 6f;
            coolantMultiplier = 1.5f;
            range = 60f;
            shootCone = 50f;
            targetAir = false;
            ammoUseEffect = Fx.none;
            health = 400;
            shootSound = Sounds.flame;
        }};

        hail = new ItemTurret("hail"){{
            requirements(Category.turret, with(Items.copper, 40, Items.graphite, 17));
            ammo(
                Items.graphite, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artilleryIncendiary
            );
            targetAir = false;
            reloadTime = 60f;
            recoilAmount = 2f;
            range = 235f;
            inaccuracy = 1f;
            shootCone = 10f;
            health = 260;
            shootSound = Sounds.bang;
            limitRange(0f);
        }};

        wave = new LiquidTurret("wave"){{
            requirements(Category.turret, with(Items.metaglass, 45, Items.lead, 75));
            ammo(
                Liquids.water, Bullets.waterShot,
                Liquids.slag, Bullets.slagShot,
                Liquids.cryofluid, Bullets.cryoShot,
                Liquids.oil, Bullets.oilShot
            );
            size = 2;
            recoilAmount = 0f;
            reloadTime = 3f;
            inaccuracy = 5f;
            shootCone = 50f;
            liquidCapacity = 10f;
            shootEffect = Fx.shootLiquid;
            range = 110f;
            health = 250 * size * size;
            flags = EnumSet.of(BlockFlag.turret, BlockFlag.extinguisher);
        }};

        lancer = new PowerTurret("lancer"){{
            requirements(Category.turret, with(Items.copper, 60, Items.lead, 70, Items.silicon, 50));
            range = 165f;
            chargeTime = 40f;
            chargeMaxDelay = 30f;
            chargeEffects = 7;
            recoilAmount = 2f;
            reloadTime = 80f;
            cooldown = 0.03f;
            powerUse = 6f;
            shootShake = 2f;
            shootEffect = Fx.lancerLaserShoot;
            smokeEffect = Fx.none;
            chargeEffect = Fx.lancerLaserCharge;
            chargeBeginEffect = Fx.lancerLaserChargeBegin;
            heatColor = Color.red;
            size = 2;
            health = 280 * size * size;
            targetAir = false;
            shootSound = Sounds.laser;

            shootType = new LaserBulletType(140){{
                colors = new Color[]{Pal.lancerLaser.cpy().a(0.4f), Pal.lancerLaser, Color.white};
                hitEffect = Fx.hitLancer;
                hitSize = 4;
                lifetime = 16f;
                drawSize = 400f;
                collidesAir = false;
                length = 173f;
                ammoMultiplier = 1f;
            }};
        }};

        arc = new PowerTurret("arc"){{
            requirements(Category.turret, with(Items.copper, 50, Items.lead, 50));
            shootType = new LightningBulletType(){{
                damage = 20;
                lightningLength = 25;
                collidesAir = false;
                ammoMultiplier = 1f;
            }};
            reloadTime = 35f;
            shootCone = 40f;
            rotateSpeed = 8f;
            powerUse = 3.3f;
            targetAir = false;
            range = 90f;
            shootEffect = Fx.lightningShoot;
            heatColor = Color.red;
            recoilAmount = 1f;
            size = 1;
            health = 260;
            shootSound = Sounds.spark;
        }};

        parallax = new TractorBeamTurret("parallax"){{
            requirements(Category.turret, with(Items.silicon, 120, Items.titanium, 90, Items.graphite, 30));

            hasPower = true;
            size = 2;
            force = 12f;
            scaledForce = 6f;
            range = 240f;
            damage = 0.3f;
            health = 160 * size * size;
            rotateSpeed = 10;

            consumes.powerCond(3f, (TractorBeamBuild e) -> e.target != null);
        }};

        swarmer = new ItemTurret("swarmer"){{
            requirements(Category.turret, with(Items.graphite, 35, Items.titanium, 35, Items.plastanium, 45, Items.silicon, 30));
            ammo(
                Items.blastCompound, Bullets.missileExplosive,
                Items.pyratite, Bullets.missileIncendiary,
                Items.surgeAlloy, Bullets.missileSurge
            );
            reloadTime = 30f;
            shots = 4;
            burstSpacing = 5;
            inaccuracy = 10f;
            range = 235f;
            xRand = 6f;
            size = 2;
            health = 300 * size * size;
            shootSound = Sounds.missile;

            limitRange(5f);
        }};

        salvo = new ItemTurret("salvo"){{
            requirements(Category.turret, with(Items.copper, 100, Items.graphite, 80, Items.titanium, 50));
            ammo(
                Items.copper, Bullets.standardCopper,
                Items.graphite, Bullets.standardDense,
                Items.pyratite, Bullets.standardIncendiary,
                Items.silicon, Bullets.standardHoming,
                Items.thorium, Bullets.standardThorium
            );

            size = 2;
            range = 190f;
            reloadTime = 31f;
            restitution = 0.03f;
            ammoEjectBack = 3f;
            cooldown = 0.03f;
            recoilAmount = 3f;
            shootShake = 1f;
            burstSpacing = 3f;
            shots = 4;
            ammoUseEffect = Fx.casing2;
            health = 240 * size * size;
            shootSound = Sounds.shootBig;

            limitRange();
        }};

        segment = new PointDefenseTurret("segment"){{
            requirements(Category.turret, with(Items.silicon, 130, Items.thorium, 80, Items.phaseFabric, 40));

            health = 250 * size * size;
            range = 180f;
            hasPower = true;
            consumes.powerCond(8f, (PointDefenseBuild b) -> b.target != null);
            size = 2;
            shootLength = 5f;
            bulletDamage = 30f;
            reloadTime = 8f;
        }};

        tsunami = new LiquidTurret("tsunami"){{
            requirements(Category.turret, with(Items.metaglass, 100, Items.lead, 400, Items.titanium, 250, Items.thorium, 100));
            ammo(
                Liquids.water, Bullets.heavyWaterShot,
                Liquids.slag, Bullets.heavySlagShot,
                Liquids.cryofluid, Bullets.heavyCryoShot,
                Liquids.oil, Bullets.heavyOilShot
            );
            size = 3;
            reloadTime = 3f;
            shots = 2;
            velocityInaccuracy = 0.1f;
            inaccuracy = 4f;
            recoilAmount = 1f;
            restitution = 0.04f;
            shootCone = 45f;
            liquidCapacity = 40f;
            shootEffect = Fx.shootLiquid;
            range = 190f;
            health = 250 * size * size;
            flags = EnumSet.of(BlockFlag.turret, BlockFlag.extinguisher);
        }};

        fuse = new ItemTurret("fuse"){{
            requirements(Category.turret, with(Items.copper, 225, Items.graphite, 225, Items.thorium, 100));

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

            float brange = range + 10f;

            ammo(
                Items.titanium, new ShrapnelBulletType(){{
                    length = brange;
                    damage = 66f;
                    ammoMultiplier = 4f;
                    width = 17f;
                    reloadMultiplier = 1.3f;
                }},
                Items.thorium, new ShrapnelBulletType(){{
                    length = brange;
                    damage = 105f;
                    ammoMultiplier = 5f;
                    toColor = Pal.thoriumPink;
                    shootEffect = smokeEffect = Fx.thoriumShoot;
                }}
            );
        }};

        ripple = new ItemTurret("ripple"){{
            requirements(Category.turret, with(Items.copper, 150, Items.graphite, 135, Items.titanium, 60));
            ammo(
                Items.graphite, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artilleryIncendiary,
                Items.blastCompound, Bullets.artilleryExplosive,
                Items.plastanium, Bullets.artilleryPlastic
            );

            targetAir = false;
            size = 3;
            shots = 4;
            inaccuracy = 12f;
            reloadTime = 60f;
            ammoEjectBack = 5f;
            ammoUseEffect = Fx.casing3Double;
            ammoPerShot = 2;
            cooldown = 0.03f;
            velocityInaccuracy = 0.2f;
            restitution = 0.02f;
            recoilAmount = 6f;
            shootShake = 2f;
            range = 290f;
            minRange = 50f;

            health = 130 * size * size;
            shootSound = Sounds.artillery;
        }};

        cyclone = new ItemTurret("cyclone"){{
            requirements(Category.turret, with(Items.copper, 200, Items.titanium, 125, Items.plastanium, 80));
            ammo(
                Items.metaglass, Bullets.fragGlass,
                Items.blastCompound, Bullets.fragExplosive,
                Items.plastanium, Bullets.fragPlastic,
                Items.surgeAlloy, Bullets.fragSurge
            );
            xRand = 4f;
            reloadTime = 8f;
            range = 200f;
            size = 3;
            recoilAmount = 3f;
            rotateSpeed = 10f;
            inaccuracy = 10f;
            shootCone = 30f;
            shootSound = Sounds.shootSnap;

            health = 145 * size * size;
            limitRange();
        }};

        foreshadow = new ItemTurret("foreshadow"){{
            float brange = range = 500f;

            requirements(Category.turret, with(Items.copper, 1000, Items.metaglass, 600, Items.surgeAlloy, 300, Items.plastanium, 200, Items.silicon, 600));
            ammo(
                Items.surgeAlloy, new PointBulletType(){{
                    shootEffect = Fx.instShoot;
                    hitEffect = Fx.instHit;
                    smokeEffect = Fx.smokeCloud;
                    trailEffect = Fx.instTrail;
                    despawnEffect = Fx.instBomb;
                    trailSpacing = 20f;
                    damage = 1350;
                    buildingDamageMultiplier = 0.2f;
                    speed = brange;
                    hitShake = 6f;
                    ammoMultiplier = 1f;
                }}
            );

            maxAmmo = 40;
            ammoPerShot = 5;
            rotateSpeed = 2f;
            reloadTime = 200f;
            ammoUseEffect = Fx.casing3Double;
            recoilAmount = 5f;
            restitution = 0.009f;
            cooldown = 0.009f;
            shootShake = 4f;
            shots = 1;
            size = 4;
            shootCone = 2f;
            shootSound = Sounds.railgun;
            unitSort = (u, x, y) -> -u.maxHealth + Mathf.dst2(u.x, u.y, x, y) / 6400f;

            coolantMultiplier = 0.4f;

            health = 150 * size * size;
            coolantUsage = 1f;

            consumes.powerCond(10f, TurretBuild::isActive);
        }};

        spectre = new ItemTurret("spectre"){{
            requirements(Category.turret, with(Items.copper, 900, Items.graphite, 300, Items.surgeAlloy, 250, Items.plastanium, 175, Items.thorium, 250));
            ammo(
                Items.graphite, Bullets.standardDenseBig,
                Items.pyratite, Bullets.standardIncendiaryBig,
                Items.thorium, Bullets.standardThoriumBig
            );
            reloadTime = 7f;
            coolantMultiplier = 0.5f;
            restitution = 0.1f;
            ammoUseEffect = Fx.casing3;
            range = 260f;
            inaccuracy = 3f;
            recoilAmount = 3f;
            spread = 8f;
            alternate = true;
            shootShake = 2f;
            shots = 2;
            size = 4;
            shootCone = 24f;
            shootSound = Sounds.shootBig;

            health = 160 * size * size;
            coolantUsage = 1f;

            limitRange();
        }};

        meltdown = new LaserTurret("meltdown"){{
            requirements(Category.turret, with(Items.copper, 1200, Items.lead, 350, Items.graphite, 300, Items.surgeAlloy, 325, Items.silicon, 325));
            shootEffect = Fx.shootBigSmoke2;
            shootCone = 40f;
            recoilAmount = 4f;
            size = 4;
            shootShake = 2f;
            range = 195f;
            reloadTime = 90f;
            firingMoveFract = 0.5f;
            shootDuration = 230f;
            powerUse = 17f;
            shootSound = Sounds.laserbig;
            loopSound = Sounds.beam;
            loopSoundVolume = 2f;

            shootType = new ContinuousLaserBulletType(78){{
                length = 200f;
                hitEffect = Fx.hitMeltdown;
                hitColor = Pal.meltdownHit;
                drawSize = 420f;

                incendChance = 0.4f;
                incendSpread = 5f;
                incendAmount = 1;
                ammoMultiplier = 1f;
            }};

            health = 200 * size * size;
            consumes.add(new ConsumeCoolant(0.5f)).update(false);
        }};

        //endregion
        //region units

        commandCenter = new CommandCenter("command-center"){{
            requirements(Category.units, ItemStack.with(Items.copper, 200, Items.lead, 250, Items.silicon, 250, Items.graphite, 100));
            size = 2;
            health = size * size * 55;
        }};

        groundFactory = new UnitFactory("ground-factory"){{
            requirements(Category.units, with(Items.copper, 50, Items.lead, 120, Items.silicon, 80));
            plans = Seq.with(
                new UnitPlan(UnitTypes.dagger, 60f * 15, with(Items.silicon, 10, Items.lead, 10)),
                new UnitPlan(UnitTypes.crawler, 60f * 12, with(Items.silicon, 10, Items.coal, 20)),
                new UnitPlan(UnitTypes.nova, 60f * 40, with(Items.silicon, 30, Items.lead, 20, Items.titanium, 20))
            );
            size = 3;
            consumes.power(1.2f);
        }};

        airFactory = new UnitFactory("air-factory"){{
            requirements(Category.units, with(Items.copper, 60, Items.lead, 70));
            plans = Seq.with(
                new UnitPlan(UnitTypes.flare, 60f * 15, with(Items.silicon, 15)),
                new UnitPlan(UnitTypes.mono, 60f * 35, with(Items.silicon, 30, Items.lead, 15))
            );
            size = 3;
            consumes.power(1.2f);
        }};

        navalFactory = new UnitFactory("naval-factory"){{
            requirements(Category.units, with(Items.copper, 150, Items.lead, 130, Items.metaglass, 120));
            plans = Seq.with(
                new UnitPlan(UnitTypes.risso, 60f * 45f, with(Items.silicon, 20, Items.metaglass, 35)),
                new UnitPlan(UnitTypes.retusa, 60f * 50f, with(Items.silicon, 15, Items.metaglass, 25, Items.titanium, 20))
            );
            size = 3;
            consumes.power(1.2f);
            floating = true;
        }};

        additiveReconstructor = new Reconstructor("additive-reconstructor"){{
            requirements(Category.units, with(Items.copper, 200, Items.lead, 120, Items.silicon, 90));

            size = 3;
            consumes.power(3f);
            consumes.items(with(Items.silicon, 40, Items.graphite, 40));

            constructTime = 60f * 10f;

            upgrades.addAll(
                new UnitType[]{UnitTypes.nova, UnitTypes.pulsar},
                new UnitType[]{UnitTypes.dagger, UnitTypes.mace},
                new UnitType[]{UnitTypes.crawler, UnitTypes.atrax},
                new UnitType[]{UnitTypes.flare, UnitTypes.horizon},
                new UnitType[]{UnitTypes.mono, UnitTypes.poly},
                new UnitType[]{UnitTypes.risso, UnitTypes.minke},
                new UnitType[]{UnitTypes.retusa, UnitTypes.oxynoe}
            );
        }};

        multiplicativeReconstructor = new Reconstructor("multiplicative-reconstructor"){{
            requirements(Category.units, with(Items.lead, 650, Items.silicon, 450, Items.titanium, 350, Items.thorium, 650));

            size = 5;
            consumes.power(6f);
            consumes.items(with(Items.silicon, 130, Items.titanium, 80, Items.metaglass, 40));

            constructTime = 60f * 30f;

            upgrades.addAll(
                new UnitType[]{UnitTypes.horizon, UnitTypes.zenith},
                new UnitType[]{UnitTypes.mace, UnitTypes.fortress},
                new UnitType[]{UnitTypes.poly, UnitTypes.mega},
                new UnitType[]{UnitTypes.minke, UnitTypes.bryde},
                new UnitType[]{UnitTypes.pulsar, UnitTypes.quasar},
                new UnitType[]{UnitTypes.atrax, UnitTypes.spiroct},
                new UnitType[]{UnitTypes.oxynoe, UnitTypes.cyerce}
            );
        }};

        exponentialReconstructor = new Reconstructor("exponential-reconstructor"){{
            requirements(Category.units, with(Items.lead, 2000, Items.silicon, 1000, Items.titanium, 2000, Items.thorium, 750, Items.plastanium, 450, Items.phaseFabric, 600));

            size = 7;
            consumes.power(13f);
            consumes.items(with(Items.silicon, 850, Items.titanium, 750, Items.plastanium, 650));
            consumes.liquid(Liquids.cryofluid, 1f);

            constructTime = 60f * 60f * 1.5f;
            liquidCapacity = 60f;

            upgrades.addAll(
                new UnitType[]{UnitTypes.zenith, UnitTypes.antumbra},
                new UnitType[]{UnitTypes.spiroct, UnitTypes.arkyid},
                new UnitType[]{UnitTypes.fortress, UnitTypes.scepter},
                new UnitType[]{UnitTypes.bryde, UnitTypes.sei},
                new UnitType[]{UnitTypes.mega, UnitTypes.quad},
                new UnitType[]{UnitTypes.quasar, UnitTypes.vela},
                new UnitType[]{UnitTypes.cyerce, UnitTypes.aegires}
            );
        }};

        tetrativeReconstructor = new Reconstructor("tetrative-reconstructor"){{
            requirements(Category.units, with(Items.lead, 4000, Items.silicon, 3000, Items.thorium, 1000, Items.plastanium, 600, Items.phaseFabric, 600, Items.surgeAlloy, 800));

            size = 9;
            consumes.power(25f);
            consumes.items(with(Items.silicon, 1000, Items.plastanium, 600, Items.surgeAlloy, 500, Items.phaseFabric, 350));
            consumes.liquid(Liquids.cryofluid, 3f);

            constructTime = 60f * 60f * 4;
            liquidCapacity = 180f;

            upgrades.addAll(
                new UnitType[]{UnitTypes.antumbra, UnitTypes.eclipse},
                new UnitType[]{UnitTypes.arkyid, UnitTypes.toxopid},
                new UnitType[]{UnitTypes.scepter, UnitTypes.reign},
                new UnitType[]{UnitTypes.sei, UnitTypes.omura},
                new UnitType[]{UnitTypes.quad, UnitTypes.oct},
                new UnitType[]{UnitTypes.vela, UnitTypes.corvus},
                new UnitType[]{UnitTypes.aegires, UnitTypes.navanax}
            );
        }};

        repairPoint = new RepairPoint("repair-point"){{
            requirements(Category.units, with(Items.lead, 30, Items.copper, 30, Items.silicon, 20));
            repairSpeed = 0.45f;
            repairRadius = 60f;
            beamWidth = 0.73f;
            powerUse = 1f;
            pulseRadius = 5f;
        }};

        repairTurret = new RepairPoint("repair-turret"){{
            requirements(Category.units, with(Items.silicon, 90, Items.thorium, 80, Items.plastanium, 60));
            size = 2;
            length = 6f;
            repairSpeed = 3f;
            repairRadius = 145f;
            powerUse = 5f;
            beamWidth = 1.1f;
            pulseRadius = 6.1f;
            coolantUse = 0.16f;
            coolantMultiplier = 1.6f;
            acceptCoolant = true;
        }};

        //endregion
        //region payloads

        payloadConveyor = new PayloadConveyor("payload-conveyor"){{
            requirements(Category.units, with(Items.graphite, 10, Items.copper, 20));
            canOverdrive = false;
        }};

        payloadRouter = new PayloadRouter("payload-router"){{
            requirements(Category.units, with(Items.graphite, 15, Items.copper, 20));
            canOverdrive = false;
        }};

        payloadPropulsionTower = new PayloadMassDriver("payload-propulsion-tower"){{
            requirements(Category.units, with(Items.thorium, 300, Items.silicon, 200, Items.plastanium, 200, Items.phaseFabric, 50));
            size = 5;
            reloadTime = 130f;
            chargeTime = 100f;
            range = 1000f;
            maxPayloadSize = 3.5f;
            consumes.power(6f);
        }};

        //endregion
        //region sandbox

        powerSource = new PowerSource("power-source"){{
            requirements(Category.power, BuildVisibility.sandboxOnly, with());
            powerProduction = 1000000f / 60f;
            alwaysUnlocked = true;
        }};

        powerVoid = new PowerVoid("power-void"){{
            requirements(Category.power, BuildVisibility.sandboxOnly, with());
            alwaysUnlocked = true;
        }};

        itemSource = new ItemSource("item-source"){{
            requirements(Category.distribution, BuildVisibility.sandboxOnly, with());
            alwaysUnlocked = true;
        }};

        itemVoid = new ItemVoid("item-void"){{
            requirements(Category.distribution, BuildVisibility.sandboxOnly, with());
            alwaysUnlocked = true;
        }};

        liquidSource = new LiquidSource("liquid-source"){{
            requirements(Category.liquid, BuildVisibility.sandboxOnly, with());
            alwaysUnlocked = true;
        }};

        liquidVoid = new LiquidVoid("liquid-void"){{
            requirements(Category.liquid, BuildVisibility.sandboxOnly, with());
            alwaysUnlocked = true;
        }};

        payloadSource = new PayloadSource("payload-source"){{
            requirements(Category.units, BuildVisibility.sandboxOnly, with());
            size = 5;
        }};

        payloadVoid = new PayloadVoid("payload-void"){{
            requirements(Category.units, BuildVisibility.sandboxOnly, with());
            size = 5;
        }};

        //TODO move
        illuminator = new LightBlock("illuminator"){{
            requirements(Category.effect, BuildVisibility.lightingOnly, with(Items.graphite, 12, Items.silicon, 8));
            brightness = 0.75f;
            radius = 140f;
            consumes.power(0.05f);
        }};

        //endregion
        //region legacy

        //looked up by name, no ref needed
        new LegacyMechPad("legacy-mech-pad");
        new LegacyUnitFactory("legacy-unit-factory");
        new LegacyUnitFactory("legacy-unit-factory-air"){{
            replacement = Blocks.airFactory;
        }};
        new LegacyUnitFactory("legacy-unit-factory-ground"){{
            replacement = Blocks.groundFactory;
        }};

        //endregion
        //region campaign

        launchPad = new LaunchPad("launch-pad"){{
            requirements(Category.effect, BuildVisibility.campaignOnly, with(Items.copper, 350, Items.silicon, 140, Items.lead, 200, Items.titanium, 150));
            size = 3;
            itemCapacity = 100;
            launchTime = 60f * 20;
            hasPower = true;
            consumes.power(4f);
        }};

        interplanetaryAccelerator = new Accelerator("interplanetary-accelerator"){{
            requirements(Category.effect, BuildVisibility.campaignOnly, with(Items.copper, 16000, Items.silicon, 11000, Items.thorium, 13000, Items.titanium, 12000, Items.surgeAlloy, 6000, Items.phaseFabric, 5000));
            researchCostMultiplier = 0.1f;
            size = 7;
            hasPower = true;
            consumes.power(10f);
            buildCostMultiplier = 0.5f;
            health = size * size * 80;
        }};

        //endregion campaign
        //region logic

        message = new MessageBlock("message"){{
            requirements(Category.logic, with(Items.graphite, 5));
        }};

        switchBlock = new SwitchBlock("switch"){{
            requirements(Category.logic, with(Items.graphite, 5));
        }};

        microProcessor = new LogicBlock("micro-processor"){{
            requirements(Category.logic, with(Items.copper, 90, Items.lead, 50, Items.silicon, 50));

            instructionsPerTick = 2;

            size = 1;
        }};

        logicProcessor = new LogicBlock("logic-processor"){{
            requirements(Category.logic, with(Items.lead, 320, Items.silicon, 80, Items.graphite, 60, Items.thorium, 50));

            instructionsPerTick = 8;

            range = 8 * 22;

            size = 2;
        }};

        hyperProcessor = new LogicBlock("hyper-processor"){{
            requirements(Category.logic, with(Items.lead, 450, Items.silicon, 150, Items.thorium, 75, Items.surgeAlloy, 50));

            consumes.liquid(Liquids.cryofluid, 0.08f);
            hasLiquids = true;

            instructionsPerTick = 25;

            range = 8 * 42;

            size = 3;
        }};

        memoryCell = new MemoryBlock("memory-cell"){{
            requirements(Category.logic, with(Items.graphite, 30, Items.silicon, 30));

            memoryCapacity = 64;
        }};

        memoryBank = new MemoryBlock("memory-bank"){{
            requirements(Category.logic, with(Items.graphite, 80, Items.silicon, 80, Items.phaseFabric, 30));

            memoryCapacity = 512;
            size = 2;
        }};

        logicDisplay = new LogicDisplay("logic-display"){{
            requirements(Category.logic, with(Items.lead, 100, Items.silicon, 50, Items.metaglass, 50));

            displaySize = 80;

            size = 3;
        }};

        largeLogicDisplay = new LogicDisplay("large-logic-display"){{
            requirements(Category.logic, with(Items.lead, 200, Items.silicon, 150, Items.metaglass, 100, Items.phaseFabric, 75));

            displaySize = 176;

            size = 6;
        }};

        //endregion
        //region experimental

        blockForge = new BlockForge("block-forge"){{
            requirements(Category.units, BuildVisibility.debugOnly, with(Items.thorium, 100));
            hasPower = true;
            consumes.power(2f);
            size = 3;
        }};

        blockLoader = new BlockLoader("block-loader"){{
            requirements(Category.units, BuildVisibility.debugOnly, with(Items.thorium, 100));
            hasPower = true;
            consumes.power(2f);
            size = 3;
        }};

        blockUnloader = new BlockUnloader("block-unloader"){{
            requirements(Category.units, BuildVisibility.debugOnly, with(Items.thorium, 100));
            hasPower = true;
            consumes.power(2f);
            size = 3;
        }};

        //endregion
    }
}
