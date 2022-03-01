package mindustry.content;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
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
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.payloads.PayloadConveyor;
import mindustry.world.blocks.payloads.PayloadRouter;
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

public class Blocks{
    public static Block

    //environment
    air, spawn, cliff, deepwater, water, taintedWater, deepTaintedWater, tar, slag, cryofluid, stone, craters, charr, sand, darksand, dirt, mud, ice, snow, darksandTaintedWater, space, empty,
    dacite, rhyolite, rhyoliteCrater, roughRhyolite, regolith, yellowStone, redIce, redStone, denseRedStone,
    arkyciteFloor, arkyicStone,
    redmat, bluemat,
    stoneWall, dirtWall, sporeWall, iceWall, daciteWall, sporePine, snowPine, pine, shrubs, whiteTree, whiteTreeDead, sporeCluster,
    redweed, purbush, yellowCoral,
    rhyoliteVent, carbonVent, arkyicVent, yellowStoneVent, redStoneVent,
    regolithWall, yellowStoneWall, rhyoliteWall, carbonWall, redIceWall, ferricStoneWall, beryllicStoneWall, arkyicWall, crystallineStoneWall, redStoneWall, redDiamondWall,
    ferricStone, ferricCraters, carbonStone, beryllicStone, crystallineStone, crystalFloor, yellowStonePlates,
    iceSnow, sandWater, darksandWater, duneWall, sandWall, moss, sporeMoss, shale, shaleWall, grass, salt,
    coreZone,
    //boulders
    shaleBoulder, sandBoulder, daciteBoulder, boulder, snowBoulder, basaltBoulder, carbonBoulder, ferricBoulder, beryllicBoulder, yellowStoneBoulder,
    arkyicBoulder, crystalCluster, vibrantCrystalCluster, crystalBlocks, crystalOrbs, crystallineBoulder, redIceBoulder, rhyoliteBoulder, redStoneBoulder,
    metalFloor, metalFloorDamaged, metalFloor2, metalFloor3, metalFloor4, metalFloor5, basalt, magmarock, hotrock, snowWall, saltWall,
    darkPanel1, darkPanel2, darkPanel3, darkPanel4, darkPanel5, darkPanel6, darkMetal,
    pebbles, tendrils,

    //ores
    oreCopper, oreLead, oreScrap, oreCoal, oreTitanium, oreThorium,
    oreBeryllium, oreTungsten, oreCrystalThorium, wallOreThorium,

    //wall ores
    wallOreBeryllium, graphiticWall, wallOreTungsten,

    //crafting
    siliconSmelter, siliconCrucible, kiln, graphitePress, plastaniumCompressor, multiPress, phaseWeaver, surgeSmelter, pyratiteMixer, blastMixer, cryofluidMixer,
    melter, separator, disassembler, sporePress, pulverizer, incinerator, coalCentrifuge,

    //erekir
    siliconArcFurnace, electrolyzer, oxidationChamber, atmosphericConcentrator, electricHeater, phaseHeater, heatRedirector, slagIncinerator,
    carbideCrucible, slagCentrifuge, surgeCrucible, cyanogenSynthesizer, phaseSynthesizer, heatReactor,

    //sandbox
    powerSource, powerVoid, itemSource, itemVoid, liquidSource, liquidVoid, payloadSource, payloadVoid, illuminator,

    //defense
    copperWall, copperWallLarge, titaniumWall, titaniumWallLarge, plastaniumWall, plastaniumWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
    phaseWall, phaseWallLarge, surgeWall, surgeWallLarge,
    berylliumWall, berylliumWallLarge, tungstenWall, tungstenWallLarge, carbideWall, carbideWallLarge,
    mender, mendProjector, overdriveProjector, overdriveDome, forceProjector, shockMine,
    scrapWall, scrapWallLarge, scrapWallHuge, scrapWallGigantic, thruster, //ok, these names are getting ridiculous, but at least I don't have humongous walls yet

    //defense - erekir
    radar,
    buildTower,
    regenProjector, barrierProjector,
    //campaign only
    shieldProjector,
    largeShieldProjector,

    //transport
    conveyor, titaniumConveyor, plastaniumConveyor, armoredConveyor, distributor, junction, itemBridge, phaseConveyor, sorter, invertedSorter, router,
    overflowGate, underflowGate, massDriver,

    //transport - alternate
    duct, ductRouter, overflowDuct, ductBridge, ductUnloader,
    surgeConveyor, surgeRouter,

    unitCargoLoader, unitCargoUnloadPoint,

    //liquid
    mechanicalPump, rotaryPump, impulsePump, conduit, pulseConduit, platedConduit, liquidRouter, liquidContainer, liquidTank, liquidJunction, bridgeConduit, phaseConduit,

    //liquid - reinforced
    reinforcedPump, reinforcedConduit, reinforcedLiquidJunction, reinforcedBridgeConduit, reinforcedLiquidRouter, reinforcedLiquidContainer, reinforcedLiquidTank,

    //power
    combustionGenerator, thermalGenerator, steamGenerator, differentialGenerator, rtgGenerator, solarPanel, largeSolarPanel, thoriumReactor,
    impactReactor, battery, batteryLarge, powerNode, powerNodeLarge, surgeTower, diode,

    //power - erekir
    //TODO rename chemicalCombustionChamber
    turbineCondenser, ventCondenser, chemicalCombustionChamber, pyrolysisGenerator,
    beamNode, beamTower, beamLink,

    //production
    mechanicalDrill, pneumaticDrill, laserDrill, blastDrill, waterExtractor, oilExtractor, cultivator,
    cliffCrusher, plasmaBore, largePlasmaBore, impactDrill, eruptionDrill,

    //storage
    coreShard, coreFoundation, /*TODO core foundation is a bad name, rename to fragment */ coreNucleus, vault, container, unloader,
    //storage - erekir
    coreBastion, coreCitadel, coreAcropolis, reinforcedContainer, reinforcedVault,

    //turrets
    duo, scatter, scorch, hail, arc, wave, lancer, swarmer, salvo, fuse, ripple, cyclone, foreshadow, spectre, meltdown, segment, parallax, tsunami,

    //turrets - erekir
    breach, sublimate, titan, disperse, afflict,

    //units
    groundFactory, airFactory, navalFactory,
    additiveReconstructor, multiplicativeReconstructor, exponentialReconstructor, tetrativeReconstructor,
    repairPoint, repairTurret,

    //units - erekir
    fabricator,
    tankReconstructor, shipReconstructor, mechReconstructor,
    tankAssembler, shipAssembler, mechAssembler,
    //TODO maybe making it 5x5 would be more appropriate, seems kinda cheap.
    basicAssemblerModule,

    //TODO remove
    droneCenter,

    //payloads
    payloadConveyor, payloadRouter, reinforcedPayloadConveyor, reinforcedPayloadRouter, payloadMassDriver, payloadPropulsionTower, smallDeconstructor, deconstructor, constructor, largeConstructor, payloadLoader, payloadUnloader,
    
    //logic
    message, switchBlock, microProcessor, logicProcessor, hyperProcessor, largeLogicDisplay, logicDisplay, memoryCell, memoryBank,
    worldProcessor, worldCell,

    //campaign
    //TODO launch pad on erekir, 5x5, uses nuclear(?) fuel
    launchPad, interplanetaryAccelerator

    ;

    public static void load(){
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
            drownTime = 200f;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
        }};

        water = new Floor("shallow-water"){{
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
        }};

        taintedWater = new Floor("tainted-water"){{
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            attributes.set(Attribute.spores, 0.15f);
            supportsOverlay = true;
        }};

        deepTaintedWater = new Floor("deep-tainted-water"){{
            speedMultiplier = 0.18f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 140f;
            drownTime = 200f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            attributes.set(Attribute.spores, 0.15f);
            supportsOverlay = true;
        }};

        darksandTaintedWater = new ShallowLiquid("darksand-tainted-water"){{
            speedMultiplier = 0.75f;
            statusDuration = 60f;
            albedo = 0.9f;
            attributes.set(Attribute.spores, 0.1f);
            supportsOverlay = true;
        }};

        sandWater = new ShallowLiquid("sand-water"){{
            speedMultiplier = 0.8f;
            statusDuration = 50f;
            albedo = 0.9f;
            supportsOverlay = true;
        }};

        darksandWater = new ShallowLiquid("darksand-water"){{
            speedMultiplier = 0.8f;
            statusDuration = 50f;
            albedo = 0.9f;
            supportsOverlay = true;
        }};

        tar = new Floor("tar"){{
            drownTime = 230f;
            status = StatusEffects.tarred;
            statusDuration = 240f;
            speedMultiplier = 0.19f;
            variants = 0;
            liquidDrop = Liquids.oil;
            isLiquid = true;
            cacheLayer = CacheLayer.tar;
        }};

        cryofluid = new Floor("pooled-cryofluid"){{
            drownTime = 150f;
            status = StatusEffects.freezing;
            statusDuration = 240f;
            speedMultiplier = 0.5f;
            variants = 0;
            liquidDrop = Liquids.cryofluid;
            liquidMultiplier = 0.5f;
            isLiquid = true;
            cacheLayer = CacheLayer.cryofluid;

            emitLight = true;
            lightRadius = 25f;
            lightColor = Color.cyan.cpy().a(0.19f);
        }};

        slag = new Floor("molten-slag"){{
            drownTime = 230f;
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
            canShadow = false;
        }};

        empty = new EmptyFloor("empty");

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
            walkSound = Sounds.mud;
            walkSoundVolume = 0.08f;
            walkSoundPitchMin = 0.4f;
            walkSoundPitchMax = 0.5f;
        }};

        ((ShallowLiquid)darksandTaintedWater).set(Blocks.taintedWater, Blocks.darksand);
        ((ShallowLiquid)sandWater).set(Blocks.water, Blocks.sand);
        ((ShallowLiquid)darksandWater).set(Blocks.water, Blocks.darksand);

        dacite = new Floor("dacite");

        rhyolite = new Floor("rhyolite"){{
            attributes.set(Attribute.water, -1f);
        }};

        rhyoliteCrater = new Floor("rhyolite-crater"){{
            attributes.set(Attribute.water, -1f);
            blendGroup = rhyolite;
        }};

        roughRhyolite = new Floor("rough-rhyolite"){{
            attributes.set(Attribute.water, -1f);
            variants = 3;
        }};

        regolith = new Floor("regolith"){{
            attributes.set(Attribute.water, -1f);
        }};

        yellowStone = new Floor("yellow-stone"){{
            attributes.set(Attribute.water, -1f);
        }};

        carbonStone = new Floor("carbon-stone"){{
            attributes.set(Attribute.water, -1f);
            variants = 4;
        }};

        ferricStone = new Floor("ferric-stone"){{
            attributes.set(Attribute.water, -1f);
        }};

        ferricCraters = new Floor("ferric-craters"){{
            variants = 3;
            attributes.set(Attribute.water, -1f);
            blendGroup = ferricStone;
        }};

        beryllicStone = new Floor("beryllic-stone"){{
            variants = 4;
        }};

        crystallineStone = new Floor("crystalline-stone"){{
            variants = 5;
        }};

        crystalFloor = new Floor("crystal-floor"){{
            variants = 4;
        }};

        yellowStonePlates = new Floor("yellow-stone-plates"){{
            variants = 3;
        }};

        redStone = new Floor("red-stone"){{
            attributes.set(Attribute.water, -1f);
            variants = 4;
        }};

        denseRedStone = new Floor("dense-red-stone"){{
            attributes.set(Attribute.water, -1f);
            variants = 4;
        }};

        redIce = new Floor("red-ice"){{
            //TODO red ice boulder
            dragMultiplier = 0.4f;
            speedMultiplier = 0.9f;
            attributes.set(Attribute.water, 0.4f);
        }};

        arkyciteFloor = new Floor("arkycite-floor"){{
            speedMultiplier = 0.3f;
            variants = 0;
            liquidDrop = Liquids.arkycite;
            liquidMultiplier = 1.5f;
            isLiquid = true;
            //TODO status, cache layer for this crap
            status = StatusEffects.wet;
            statusDuration = 120f;
            drownTime = 200f;
            cacheLayer = CacheLayer.arkycite;
            albedo = 0.9f;
        }};

        arkyicStone = new Floor("arkyic-stone"){{
            variants = 3;
        }};

        //TODO different vent colors
        rhyoliteVent = new SteamVent("rhyolite-vent"){{
            parent = blendGroup = rhyolite;
            attributes.set(Attribute.steam, 1f);
        }};

        carbonVent = new SteamVent("carbon-vent"){{
            parent = blendGroup = carbonStone;
            attributes.set(Attribute.steam, 1f);
        }};

        arkyicVent = new SteamVent("arkyic-vent"){{
            parent = blendGroup = arkyicStone;
            attributes.set(Attribute.steam, 1f);
        }};

        yellowStoneVent = new SteamVent("yellow-stone-vent"){{
            parent = blendGroup = yellowStone;
            attributes.set(Attribute.steam, 1f);
        }};

        redStoneVent = new SteamVent("red-stone-vent"){{
            parent = blendGroup = denseRedStone;
            attributes.set(Attribute.steam, 1f);
        }};

        redmat = new Floor("redmat");
        bluemat = new Floor("bluemat");

        grass = new Floor("grass"){{
            //TODO grass needs a bush? classic had grass bushes.
            attributes.set(Attribute.water, 0.1f);
        }};

        salt = new Floor("salt"){{
            variants = 0;
            attributes.set(Attribute.water, -0.3f);
            attributes.set(Attribute.oil, 0.3f);
        }};

        snow = new Floor("snow"){{
            attributes.set(Attribute.water, 0.2f);
            albedo = 0.7f;
        }};

        ice = new Floor("ice"){{
            dragMultiplier = 0.35f;
            speedMultiplier = 0.9f;
            attributes.set(Attribute.water, 0.4f);
            albedo = 0.65f;
        }};

        iceSnow = new Floor("ice-snow"){{
            dragMultiplier = 0.6f;
            variants = 3;
            attributes.set(Attribute.water, 0.3f);
            albedo = 0.6f;
        }};

        shale = new Floor("shale"){{
            variants = 3;
            attributes.set(Attribute.oil, 1.6f);
        }};

        moss = new Floor("moss"){{
            variants = 3;
            attributes.set(Attribute.spores, 0.15f);
        }};

        coreZone = new Floor("core-zone"){{
            variants = 0;
            allowCorePlacement = true;
        }};

        sporeMoss = new Floor("spore-moss"){{
            variants = 3;
            attributes.set(Attribute.spores, 0.3f);
        }};

        stoneWall = new StaticWall("stone-wall"){{
            attributes.set(Attribute.sand, 1f);
        }};

        sporeWall = new StaticWall("spore-wall"){{
            taintedWater.asFloor().wall = deepTaintedWater.asFloor().wall = sporeMoss.asFloor().wall = this;
        }};

        dirtWall = new StaticWall("dirt-wall");

        daciteWall = new StaticWall("dacite-wall");

        iceWall = new StaticWall("ice-wall"){{
            iceSnow.asFloor().wall = this;
            albedo = 0.6f;
        }};

        snowWall = new StaticWall("snow-wall");

        duneWall = new StaticWall("dune-wall"){{
            basalt.asFloor().wall = darksandWater.asFloor().wall = darksandTaintedWater.asFloor().wall = this;
            attributes.set(Attribute.sand, 2f);
        }};

        regolithWall = new StaticWall("regolith-wall"){{
            regolith.asFloor().wall = this;
            attributes.set(Attribute.sand, 1f);
        }};

        yellowStoneWall = new StaticWall("yellow-stone-wall"){{
            yellowStone.asFloor().wall = slag.asFloor().wall = yellowStonePlates.asFloor().wall = this;
            attributes.set(Attribute.sand, 1.5f);
        }};

        rhyoliteWall = new StaticWall("rhyolite-wall"){{
            rhyolite.asFloor().wall = rhyoliteCrater.asFloor().wall = roughRhyolite.asFloor().wall = this;
            attributes.set(Attribute.sand, 1f);
        }};

        carbonWall = new StaticWall("carbon-wall"){{
            carbonStone.asFloor().wall = this;
            attributes.set(Attribute.sand, 0.7f);
        }};

        ferricStoneWall = new StaticWall("ferric-stone-wall"){{
            ferricStone.asFloor().wall = this;
            attributes.set(Attribute.sand, 0.5f);
        }};

        beryllicStoneWall = new StaticWall("beryllic-stone-wall"){{
            beryllicStone.asFloor().wall = this;
            attributes.set(Attribute.sand, 1.2f);
        }};

        arkyicWall = new StaticWall("arkyic-wall"){{
            variants = 3;
            arkyciteFloor.asFloor().wall = arkyicStone.asFloor().wall = this;
        }};

        crystallineStoneWall = new StaticWall("crystalline-stone-wall"){{
            variants = 4;
            crystallineStone.asFloor().wall = crystalFloor.asFloor().wall = this;
        }};

        redIceWall = new StaticWall("red-ice-wall"){{
            redIce.asFloor().wall = this;
        }};

        redStoneWall = new StaticWall("red-stone-wall"){{
            redStone.asFloor().wall = denseRedStone.asFloor().wall = this;
            attributes.set(Attribute.sand, 1.5f);
        }};

        redDiamondWall = new StaticTree("red-diamond-wall"){{
            variants = 3;
        }};

        sandWall = new StaticWall("sand-wall"){{
            sandWater.asFloor().wall = water.asFloor().wall = deepwater.asFloor().wall = this;
            attributes.set(Attribute.sand, 2f);
        }};

        saltWall = new StaticWall("salt-wall");

        shrubs = new StaticWall("shrubs");

        shaleWall = new StaticWall("shale-wall");

        sporePine = new StaticTree("spore-pine"){{
            moss.asFloor().wall = this;
        }};

        snowPine = new StaticTree("snow-pine");

        pine = new StaticTree("pine");

        whiteTreeDead = new TreeBlock("white-tree-dead");

        whiteTree = new TreeBlock("white-tree");

        sporeCluster = new Prop("spore-cluster"){{
            variants = 3;
            breakSound = Sounds.plantBreak;
        }};

        redweed = new Seaweed("redweed"){{
            variants = 3;
            redmat.asFloor().decoration = this;
        }};

        purbush = new SeaBush("pur-bush"){{
            bluemat.asFloor().decoration = this;
        }};

        yellowCoral = new SeaBush("yellowcoral"){{
            lobesMin = 2;
            lobesMax = 3;
            magMax = 8f;
            magMin = 2f;
            origin = 0.3f;
            spread = 40f;
            sclMin = 60f;
            sclMax = 100f;
        }};

        boulder = new Prop("boulder"){{
            variants = 2;
            stone.asFloor().decoration = craters.asFloor().decoration = charr.asFloor().decoration = this;
        }};

        snowBoulder = new Prop("snow-boulder"){{
            variants = 2;
            snow.asFloor().decoration = ice.asFloor().decoration = iceSnow.asFloor().decoration = salt.asFloor().decoration = this;
        }};

        shaleBoulder = new Prop("shale-boulder"){{
            variants = 2;
            shale.asFloor().decoration = this;
        }};

        sandBoulder = new Prop("sand-boulder"){{
            variants = 2;
            sand.asFloor().decoration = this;
        }};

        daciteBoulder = new Prop("dacite-boulder"){{
            variants = 2;
            dacite.asFloor().decoration = this;
        }};

        basaltBoulder = new Prop("basalt-boulder"){{
            variants = 2;
            basalt.asFloor().decoration = hotrock.asFloor().decoration = darksand.asFloor().decoration = magmarock.asFloor().decoration = this;
        }};

        carbonBoulder = new Prop("carbon-boulder"){{
            variants = 2;
            carbonStone.asFloor().decoration = this;
        }};

        ferricBoulder = new Prop("ferric-boulder"){{
            variants = 2;
            ferricStone.asFloor().decoration = ferricCraters.asFloor().decoration = this;
        }};

        beryllicBoulder = new Prop("beryllic-boulder"){{
            variants = 2;
            beryllicStone.asFloor().decoration = this;
        }};

        yellowStoneBoulder = new Prop("yellow-stone-boulder"){{
            variants = 2;
            yellowStone.asFloor().decoration = regolith.asFloor().decoration = yellowStonePlates.asFloor().decoration = this;
        }};

        //1px outline + 4.50 gaussian shadow in gimp
        arkyicBoulder = new Prop("arkyic-boulder"){{
            variants = 3;
            customShadow = true;
            arkyicStone.asFloor().decoration = this;
        }};

        crystalCluster = new TallBlock("crystal-cluster"){{
            variants = 3;
            clipSize = 128f;
        }};

        vibrantCrystalCluster = new TallBlock("vibrant-crystal-cluster"){{
            variants = 3;
            clipSize = 128f;
        }};

        crystalBlocks = new TallBlock("crystal-blocks"){{
            variants = 3;
            clipSize = 128f;
            shadowAlpha = 0.5f;
            shadowOffset = -2.5f;
        }};

        crystalOrbs = new TallBlock("crystal-orbs"){{
            variants = 3;
            clipSize = 128f;
            shadowAlpha = 0.5f;
            shadowOffset = -2.5f;
        }};

        crystallineBoulder = new Prop("crystalline-boulder"){{
            variants = 2;
            crystallineStone.asFloor().decoration = this;
        }};

        redIceBoulder = new Prop("red-ice-boulder"){{
            variants = 3;
            redIce.asFloor().decoration = this;
        }};

        rhyoliteBoulder = new Prop("rhyolite-boulder"){{
            variants = 3;
            rhyolite.asFloor().decoration = roughRhyolite.asFloor().decoration = this;
        }};

        redStoneBoulder = new Prop("red-stone-boulder"){{
            variants = 4;
            denseRedStone.asFloor().decoration = redStone.asFloor().decoration = this;
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

        pebbles = new OverlayFloor("pebbles");

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

        oreBeryllium = new OreBlock(Items.beryllium);

        oreTungsten = new OreBlock(Items.tungsten);

        oreCrystalThorium = new OreBlock("ore-crystal-thorium", Items.thorium);

        wallOreThorium = new OreBlock("ore-wall-thorium", Items.thorium){{
            wallOre = true;
        }};

        wallOreBeryllium = new OreBlock("ore-wall-beryllium", Items.beryllium){{
            wallOre = true;
        }};

        graphiticWall = new StaticWall("graphitic-wall"){{
            itemDrop = Items.graphite;
            variants = 3;
        }};

        //TODO merge with standard ore?
        wallOreTungsten = new OreBlock("ore-wall-tungsten", Items.tungsten){{
            wallOre = true;
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

            consumeItem(Items.coal, 2);
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

            consumePower(1.8f);
            consumeItem(Items.coal, 3);
            consumeLiquid(Liquids.water, 0.1f);
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
            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.07f;

            consumeItems(with(Items.coal, 1, Items.sand, 2));
            consumePower(0.50f);
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
            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.07f;

            consumeItems(with(Items.coal, 4, Items.sand, 6, Items.pyratite, 1));
            consumePower(4f);
        }};

        siliconArcFurnace = new GenericCrafter("silicon-arc-furnace"){{
            requirements(Category.crafting, with(Items.beryllium, 70, Items.graphite, 80));
            craftEffect = Fx.none;
            outputItem = new ItemStack(Items.silicon, 4);
            craftTime = 50f;
            size = 3;
            hasPower = true;
            hasLiquids = false;
            envEnabled |= Env.space | Env.underwater;
            envDisabled = Env.none;
            itemCapacity = 30;
            drawer = new DrawArcSmelter();
            researchCost = with(Items.beryllium, 150, Items.graphite, 50);

            consumeItems(with(Items.graphite, 1, Items.sand, 4));
            consumePower(6f);
        }};

        kiln = new GenericCrafter("kiln"){{
            requirements(Category.crafting, with(Items.copper, 60, Items.graphite, 30, Items.lead, 30));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.metaglass, 1);
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = true;
            drawer = new DrawSmelter(Color.valueOf("ffc099"));
            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.07f;

            consumeItems(with(Items.lead, 1, Items.sand, 1));
            consumePower(0.60f);
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

            consumeLiquid(Liquids.oil, 0.25f);
            consumePower(3f);
            consumeItem(Items.titanium, 2);
        }};

        phaseWeaver = new GenericCrafter("phase-weaver"){{
            requirements(Category.crafting, with(Items.silicon, 130, Items.lead, 120, Items.thorium, 75));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.phaseFabric, 1);
            craftTime = 120f;
            size = 2;
            hasPower = true;
            drawer = new DrawWeave();
            envEnabled |= Env.space;

            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.02f;

            consumeItems(with(Items.thorium, 4, Items.sand, 10));
            consumePower(5f);
            itemCapacity = 20;
        }};

        surgeSmelter = new GenericCrafter("surge-smelter"){{
            requirements(Category.crafting, with(Items.silicon, 80, Items.lead, 80, Items.thorium, 70));
            craftEffect = Fx.smeltsmoke;
            outputItem = new ItemStack(Items.surgeAlloy, 1);
            craftTime = 75f;
            size = 3;
            hasPower = true;
            itemCapacity = 20;
            drawer = new DrawSmelter();

            consumePower(4f);
            consumeItems(with(Items.copper, 3, Items.lead, 4, Items.titanium, 2, Items.silicon, 3));
        }};

        cryofluidMixer = new GenericCrafter("cryofluid-mixer"){{
            requirements(Category.crafting, with(Items.lead, 65, Items.silicon, 40, Items.titanium, 60));
            outputLiquid = new LiquidStack(Liquids.cryofluid, 12f / 60f);
            size = 2;
            hasPower = true;
            hasItems = true;
            hasLiquids = true;
            rotate = false;
            solid = true;
            outputsLiquid = true;
            envEnabled = Env.any;
            drawer = new DrawMixer(true);
            liquidCapacity = 24f;

            consumePower(1f);
            consumeItem(Items.titanium);
            consumeLiquid(Liquids.water, 12f / 60f);
        }};

        pyratiteMixer = new GenericCrafter("pyratite-mixer"){{
            requirements(Category.crafting, with(Items.copper, 50, Items.lead, 25));
            hasItems = true;
            hasPower = true;
            outputItem = new ItemStack(Items.pyratite, 1);
            envEnabled |= Env.space;

            size = 2;

            consumePower(0.20f);
            consumeItems(with(Items.coal, 1, Items.lead, 2, Items.sand, 2));
        }};

        blastMixer = new GenericCrafter("blast-mixer"){{
            requirements(Category.crafting, with(Items.lead, 30, Items.titanium, 20));
            hasItems = true;
            hasPower = true;
            outputItem = new ItemStack(Items.blastCompound, 1);
            size = 2;
            envEnabled |= Env.space;

            consumeItems(with(Items.pyratite, 1, Items.sporePod, 1));
            consumePower(0.40f);
        }};

        melter = new GenericCrafter("melter"){{
            requirements(Category.crafting, with(Items.copper, 30, Items.lead, 35, Items.graphite, 45));
            health = 200;
            outputLiquid = new LiquidStack(Liquids.slag, 12f / 60f);
            craftTime = 1f;
            hasLiquids = hasPower = true;
            drawer = new DrawLiquid(){{
                liquidDrawn = Liquids.slag;
            }};

            consumePower(1f);
            consumeItem(Items.scrap, 1);
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

            consumePower(1.1f);
            consumeLiquid(Liquids.slag, 4f / 60f);
        }};

        disassembler = new Separator("disassembler"){{
            requirements(Category.crafting, with(Items.plastanium, 40, Items.titanium, 100, Items.silicon, 150, Items.thorium, 80));
            results = with(
                Items.sand, 2,
                Items.graphite, 1,
                Items.titanium, 1,
                Items.thorium, 1
            );
            hasPower = true;
            craftTime = 15f;
            size = 3;
            itemCapacity = 20;

            consumePower(4f);
            consumeItem(Items.scrap);
            consumeLiquid(Liquids.slag, 0.12f);
        }};

        sporePress = new GenericCrafter("spore-press"){{
            requirements(Category.crafting, with(Items.lead, 35, Items.silicon, 30));
            liquidCapacity = 60f;
            outputLiquid = new LiquidStack(Liquids.oil, 18f / 60f);
            size = 2;
            health = 320;
            hasLiquids = true;
            hasPower = true;
            craftEffect = Fx.none;
            drawer = new DrawAnimation();

            consumeItem(Items.sporePod, 1);
            consumePower(0.7f);
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

            consumeItem(Items.scrap, 1);
            consumePower(0.50f);
        }};

        coalCentrifuge = new GenericCrafter("coal-centrifuge"){{
            requirements(Category.crafting, with(Items.titanium, 20, Items.graphite, 40, Items.lead, 30));
            craftEffect = Fx.coalSmeltsmoke;
            outputItem = new ItemStack(Items.coal, 1);
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = hasLiquids = true;
            rotateDraw = false;

            consumeLiquid(Liquids.oil, 0.1f);
            consumePower(0.7f);
        }};

        incinerator = new Incinerator("incinerator"){{
            requirements(Category.crafting, with(Items.graphite, 5, Items.lead, 15));
            health = 90;
            envEnabled |= Env.space;
            consumePower(0.50f);
        }};

        //TODO better name
        electrolyzer = new GenericCrafter("electrolyzer"){{
            requirements(Category.crafting, with(Items.silicon, 50, Items.graphite, 40, Items.beryllium, 130, Items.tungsten, 80));
            size = 3;

            researchCostMultiplier = 1.2f;
            craftTime = 10f;
            rotate = true;

            liquidCapacity = 50f;

            consumeLiquid(Liquids.water, 10f / 60f);
            consumePower(1f);

            drawer = new DrawMulti(
                new DrawRegion("-bottom"),
                new DrawLiquidTile(Liquids.water, 2f),
                new DrawBubbles(Color.valueOf("7693e3")){{
                    sides = 10;
                    recurrence = 3f;
                    spread = 6;
                    radius = 1.5f;
                    amount = 20;
                }},
                new DrawRegion(),
                new DrawLiquidOutputs(),
                new DrawGlowRegion(){{
                    alpha = 0.7f;
                    color = Color.valueOf("c4bdf3");
                    glowIntensity = 0.3f;
                    glowScale = 6f;
                }}
            );

            drawer.iconOverride = new String[]{"-bottom", ""};
            regionRotated1 = 3;
            outputLiquids = LiquidStack.with(Liquids.ozone, 4f / 60, Liquids.hydrogen, 6f / 60);
            liquidOutputDirections = new int[]{1, 3};
        }};

        atmosphericConcentrator = new HeatCrafter("atmospheric-concentrator"){{
            requirements(Category.crafting, with(Items.oxide, 60, Items.beryllium, 180, Items.silicon, 150));
            size = 3;
            hasLiquids = true;

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.nitrogen, 4.1f), new DrawBlock(), new DrawHeatInput(),
            new DrawParticles(){{
                color = Color.valueOf("d4f0ff");
                alpha = 0.6f;
                particleSize = 4f;
                particles = 10;
                particleRad = 12f;
                particleLife = 140f;
            }});

            researchCostMultiplier = 1.1f;
            liquidCapacity = 40f;
            consumePower(2f);

            heatRequirement = 6f;

            //TODO continuous output
            outputLiquid = new LiquidStack(Liquids.nitrogen, 4f / 60f);
        }};

        oxidationChamber = new HeatProducer("oxidation-chamber"){{
            requirements(Category.crafting, with(Items.tungsten, 120, Items.graphite, 80, Items.silicon, 100, Items.beryllium, 120));
            size = 3;

            outputItem = new ItemStack(Items.oxide, 1);
            researchCostMultiplier = 1.1f;

            consumeLiquid(Liquids.ozone, 2f / 60f);
            consumeItem(Items.beryllium);
            consumePower(0.5f);

            rotateDraw = false;

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidRegion(), new DrawBlock(), new DrawHeatOutput());
            drawer.iconOverride = new String[]{"-bottom", "", "-top1"};

            regionRotated1 = 2;
            craftTime = 60f * 3f;
            liquidCapacity = 30f;
            heatOutput = 5f;
        }};

        electricHeater = new HeatProducer("electric-heater"){{
            requirements(Category.crafting, with(Items.tungsten, 30, Items.oxide, 30));

            researchCostMultiplier = 4f;

            drawer = new DrawMulti(new DrawHeatOutput(true));
            rotateDraw = false;
            drawer.iconOverride = new String[]{""};
            size = 2;
            heatOutput = 3f;
            regionRotated1 = 1;
            consumePower(50f / 60f);
        }};

        phaseHeater = new HeatProducer("phase-heater"){{
            requirements(Category.crafting, with(Items.oxide, 30, Items.carbide, 30, Items.beryllium, 30));

            drawer = new DrawMulti(new DrawHeatOutput(true));
            drawer.iconOverride = new String[]{""};
            size = 2;
            heatOutput = 10f;
            craftTime = 60f * 8f;
            consumeItem(Items.phaseFabric);
            consumePower(0.5f / 60f);
        }};

        heatRedirector = new HeatConductor("heat-redirector"){{
            requirements(Category.crafting, with(Items.tungsten, 10, Items.graphite, 10));

            researchCostMultiplier = 10f;

            size = 3;
            drawer = new DrawMulti(new DrawHeatOutput(true), new DrawHeatInput("-heat"));
            regionRotated1 = 1;
        }};

        slagIncinerator = new ItemIncinerator("slag-incinerator"){{
            requirements(Category.crafting, with(Items.tungsten, 15));
            size = 1;
            consumeLiquid(Liquids.slag, 2f / 60f);
        }};

        carbideCrucible = new HeatCrafter("carbide-crucible"){{
            requirements(Category.crafting, with(Items.tungsten, 90, Items.thorium, 70, Items.oxide, 50));
            craftEffect = Fx.none;
            outputItem = new ItemStack(Items.carbide, 1);
            craftTime = 60f * 3f;
            size = 3;
            itemCapacity = 20;
            hasPower = hasItems = true;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawCrucibleFlame(), new DrawBlock(), new DrawHeatInput());
            drawer.iconOverride = new String[]{"-bottom", ""};
            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.07f;

            heatRequirement = 10f;

            consumeItems(with(Items.tungsten, 2, Items.graphite, 3));
            consumePower(2f);
        }};

        slagCentrifuge = new GenericCrafter("slag-centrifuge"){{
            requirements(Category.crafting, with(Items.carbide, 70, Items.graphite, 60, Items.silicon, 40, Items.oxide, 40));

            consumePower(2f / 60f);

            size = 3;
            consumeItem(Items.sand, 1);
            consumeLiquid(Liquids.slag, 40f / 60f);
            liquidCapacity = 80f;

            var drawers = Seq.with(new DrawRegion("-bottom"), new DrawLiquidRegion(Liquids.slag){{ alpha = 0.7f; }});

            for(int i = 0; i < 5; i++){
                int fi = i;
                drawers.add(new DrawGlowRegion(-1f){{
                    glowIntensity = 0.3f;
                    rotateSpeed = 3f / (1f + fi/1.4f);
                    alpha = 0.4f;
                    color = new Color(1f, 0.5f, 0.5f, 1f);
                }});
            }

            drawer = new DrawMulti(drawers.and(new DrawBlock()));
            drawer.iconOverride = new String[]{"-bottom", ""};

            craftTime = 60f * 2f;

            outputLiquid = new LiquidStack(Liquids.gallium, 1f / 60f);
            outputItem = new ItemStack(Items.scrap, 1);
        }};

        //TODO should have a useful turret ammo byproduct? scrap?
        //TODO surge being removed I think
        surgeCrucible = new HeatCrafter("surge-crucible"){{
            requirements(Category.crafting, with(Items.silicon, 100, Items.graphite, 80, Items.carbide, 60, Items.thorium, 90));

            size = 3;

            itemCapacity = 20;
            heatRequirement = 10f;
            craftTime = 60f * 3f;
            liquidCapacity = 80f * 5;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.07f;

            outputItem = new ItemStack(Items.surgeAlloy, 1);

            craftEffect = new RadialEffect(Fx.surgeCruciSmoke, 4, 90f, 5f);

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawCircles(){{
                color = Color.valueOf("ffc073").a(0.24f);
                strokeMax = 2.5f;
                radius = 10f;
                amount = 3;
            }}, new DrawLiquidRegion(Liquids.slag), new DrawBlock(), new DrawHeatInput(),
            new DrawHeatRegion(){{
                color = Color.valueOf("ff6060ff");
            }},
            new DrawHeatRegion("-vents"){{
                color.a = 1f;
            }});
            drawer.iconOverride = new String[]{"-bottom", ""};

            consumeItem(Items.silicon, 3);
            //TODO must consume from 2 pumps, 1, or 1.5?
            //TODO consume hydrogen/ozone?
            consumeLiquid(Liquids.slag, 80f / 60f);
            consumePower(2f); //TODO necessary?
        }};

        cyanogenSynthesizer = new HeatCrafter("cyanogen-synthesizer"){{
            //TODO requirements
            requirements(Category.crafting, with(Items.carbide, 50, Items.silicon, 80, Items.beryllium, 90));

            heatRequirement = 5f;

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.cyanogen),
            new DrawParticles(){{
                color = Color.valueOf("89e8b6");
                alpha = 0.5f;
                particleSize = 3f;
                particles = 10;
                particleRad = 9f;
                particleLife = 200f;
                reverse = true;
                particleSizeInterp = Interp.one;
            }}, new DrawBlock(), new DrawHeatInput(), new DrawHeatRegion("-heat-top"));

            drawer.iconOverride = new String[]{"-bottom", ""};

            size = 3;

            liquidCapacity = 40f;
            outputLiquid = new LiquidStack(Liquids.cyanogen, 3f / 60f);

            consumeLiquids(LiquidStack.with(Liquids.hydrogen, 3f / 60f, Liquids.nitrogen, 2f / 60f));
            consumeItem(Items.graphite);
            consumePower(2f);
        }};

        //TODO bad name, and there's no use for phase yet...
        phaseSynthesizer = new HeatCrafter("phase-synthesizer"){{
            requirements(Category.crafting, with(Items.surgeAlloy, 70, Items.carbide, 90, Items.silicon, 100, Items.thorium, 100, Items.beryllium, 200));

            size = 3;

            itemCapacity = 40;
            heatRequirement = 8f;
            craftTime = 60f * 2f;
            liquidCapacity = 10f * 4;

            ambientSound = Sounds.techloop;
            ambientSoundVolume = 0.03f;

            outputItem = new ItemStack(Items.phaseFabric, 1);

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawSpikes(){{
                color = Color.valueOf("ffd59e");
                stroke = 1.5f;
                layers = 2;
                amount = 12;
                rotateSpeed = 0.5f;
                layerSpeed = -0.9f;
            }}, new DrawMultiWeave(){{
                glowColor = new Color(1f, 0.4f, 0.4f, 0.8f);
            }}, new DrawBlock(), new DrawHeatInput(), new DrawHeatRegion("-vents"){{
                color = new Color(1f, 0.4f, 0.3f, 1f);
            }});
            drawer.iconOverride = new String[]{"-bottom", "-weave", ""};

            consumeItems(with(Items.thorium, 2, Items.sand, 6));
            consumeLiquid(Liquids.ozone, 2f / 60f);
            consumePower(8f);
        }};

        heatReactor = new HeatProducer("heat-reactor"){{
            requirements(Category.crafting, with(Items.oxide, 70, Items.graphite, 20, Items.carbide, 10, Items.thorium, 80));
            size = 3;
            craftTime = 60f * 10f;

            craftEffect = new RadialEffect(Fx.heatReactorSmoke, 4, 90f, 7f);

            itemCapacity = 20;
            consumeItem(Items.thorium, 3);
            consumeLiquid(Liquids.nitrogen, 1f / 60f);
            outputItem = new ItemStack(Items.fissileMatter, 1);
        }};

        //endregion
        //region defense

        int wallHealthMultiplier = 4;

        copperWall = new Wall("copper-wall"){{
            requirements(Category.defense, with(Items.copper, 6));
            health = 80 * wallHealthMultiplier;
            envDisabled |= Env.scorching;
        }};

        copperWallLarge = new Wall("copper-wall-large"){{
            requirements(Category.defense, ItemStack.mult(copperWall.requirements, 4));
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
            envDisabled |= Env.scorching;
        }};

        titaniumWall = new Wall("titanium-wall"){{
            requirements(Category.defense, with(Items.titanium, 6));
            health = 110 * wallHealthMultiplier;
            envDisabled |= Env.scorching;
        }};

        titaniumWallLarge = new Wall("titanium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(titaniumWall.requirements, 4));
            health = 110 * wallHealthMultiplier * 4;
            size = 2;
            envDisabled |= Env.scorching;
        }};

        plastaniumWall = new Wall("plastanium-wall"){{
            requirements(Category.defense, with(Items.plastanium, 5, Items.metaglass, 2));
            health = 125 * wallHealthMultiplier;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
            envDisabled |= Env.scorching;
        }};

        plastaniumWallLarge = new Wall("plastanium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(plastaniumWall.requirements, 4));
            health = 125 * wallHealthMultiplier * 4;
            size = 2;
            insulated = true;
            absorbLasers = true;
            schematicPriority = 10;
            envDisabled |= Env.scorching;
        }};

        //TODO all these should be hidden from view completely
        thoriumWall = new Wall("thorium-wall"){{
            requirements(Category.defense, with(Items.thorium, 6));
            health = 200 * wallHealthMultiplier;
            envDisabled |= Env.scorching;
        }};

        thoriumWallLarge = new Wall("thorium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(thoriumWall.requirements, 4));
            health = 200 * wallHealthMultiplier * 4;
            size = 2;
            envDisabled |= Env.scorching;
        }};

        phaseWall = new Wall("phase-wall"){{
            requirements(Category.defense, with(Items.phaseFabric, 6));
            health = 150 * wallHealthMultiplier;
            chanceDeflect = 10f;
            flashHit = true;
            envDisabled |= Env.scorching;
        }};

        phaseWallLarge = new Wall("phase-wall-large"){{
            requirements(Category.defense, ItemStack.mult(phaseWall.requirements, 4));
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
            chanceDeflect = 10f;
            flashHit = true;
            envDisabled |= Env.scorching;
        }};

        surgeWall = new Wall("surge-wall"){{
            requirements(Category.defense, with(Items.surgeAlloy, 6));
            health = 230 * wallHealthMultiplier;
            lightningChance = 0.05f;
            envDisabled |= Env.scorching;
        }};

        surgeWallLarge = new Wall("surge-wall-large"){{
            requirements(Category.defense, ItemStack.mult(surgeWall.requirements, 4));
            health = 230 * 4 * wallHealthMultiplier;
            size = 2;
            lightningChance = 0.05f;
            envDisabled |= Env.scorching;
        }};

        door = new Door("door"){{
            requirements(Category.defense, with(Items.titanium, 6, Items.silicon, 4));
            health = 100 * wallHealthMultiplier;
            envDisabled |= Env.scorching;
        }};

        doorLarge = new Door("door-large"){{
            requirements(Category.defense, ItemStack.mult(door.requirements, 4));
            openfx = Fx.dooropenlarge;
            closefx = Fx.doorcloselarge;
            health = 100 * 4 * wallHealthMultiplier;
            size = 2;
            envDisabled |= Env.scorching;
        }};

        scrapWall = new Wall("scrap-wall"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, with(Items.scrap, 6));
            health = 60 * wallHealthMultiplier;
            variants = 5;
            envDisabled |= Env.scorching;
        }};

        scrapWallLarge = new Wall("scrap-wall-large"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.mult(scrapWall.requirements, 4));
            health = 60 * 4 * wallHealthMultiplier;
            size = 2;
            variants = 4;
            envDisabled |= Env.scorching;
        }};

        scrapWallHuge = new Wall("scrap-wall-huge"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.mult(scrapWall.requirements, 9));
            health = 60 * 9 * wallHealthMultiplier;
            size = 3;
            variants = 3;
            envDisabled |= Env.scorching;
        }};

        scrapWallGigantic = new Wall("scrap-wall-gigantic"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, ItemStack.mult(scrapWall.requirements, 16));
            health = 60 * 16 * wallHealthMultiplier;
            size = 4;
            envDisabled |= Env.scorching;
        }};

        thruster = new Thruster("thruster"){{
            requirements(Category.defense, BuildVisibility.sandboxOnly, with(Items.scrap, 96));
            health = 55 * 16 * wallHealthMultiplier;
            size = 4;
            envDisabled |= Env.scorching;
        }};

        berylliumWall = new Wall("beryllium-wall"){{
            requirements(Category.defense, with(Items.beryllium, 6));
            health = 130 * wallHealthMultiplier;
            armor = 2f;
        }};

        berylliumWallLarge = new Wall("beryllium-wall-large"){{
            requirements(Category.defense, ItemStack.mult(berylliumWall.requirements, 4));
            health = 130 * wallHealthMultiplier * 4;
            armor = 2f;
            size = 2;
        }};

        tungstenWall = new Wall("tungsten-wall"){{
            requirements(Category.defense, with(Items.tungsten, 6));
            health = 180 * wallHealthMultiplier;
            armor = 14f;
        }};

        tungstenWallLarge = new Wall("tungsten-wall-large"){{
            requirements(Category.defense, ItemStack.mult(tungstenWall.requirements, 4));
            health = 180 * wallHealthMultiplier * 4;
            armor = 14f;
            size = 2;
        }};

        carbideWall = new Wall("carbide-wall"){{
            requirements(Category.defense, with(Items.thorium, 6, Items.carbide, 6));
            health = 240 * wallHealthMultiplier;
            armor = 16f;
        }};

        carbideWallLarge = new Wall("carbide-wall-large"){{
            requirements(Category.defense, ItemStack.mult(thoriumWall.requirements, 4));
            health = 240 * wallHealthMultiplier * 4;
            armor = 16f;
            size = 2;
        }};

        mender = new MendProjector("mender"){{
            requirements(Category.effect, with(Items.lead, 30, Items.copper, 25));
            consumePower(0.3f);
            size = 1;
            reload = 200f;
            range = 40f;
            healPercent = 4f;
            phaseBoost = 4f;
            phaseRangeBoost = 20f;
            health = 80;
            consumeItem(Items.silicon).boost();
        }};

        mendProjector = new MendProjector("mend-projector"){{
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 25, Items.silicon, 40, Items.copper, 50));
            consumePower(1.5f);
            size = 2;
            reload = 250f;
            range = 85f;
            healPercent = 11f;
            phaseBoost = 15f;
            scaledHealth = 80;
            consumeItem(Items.phaseFabric).boost();
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            requirements(Category.effect, with(Items.lead, 100, Items.titanium, 75, Items.silicon, 75, Items.plastanium, 30));
            consumePower(3.50f);
            size = 2;
            consumeItem(Items.phaseFabric).boost();
        }};

        overdriveDome = new OverdriveProjector("overdrive-dome"){{
            requirements(Category.effect, with(Items.lead, 200, Items.titanium, 130, Items.silicon, 130, Items.plastanium, 80, Items.surgeAlloy, 120));
            consumePower(10f);
            size = 3;
            range = 200f;
            speedBoost = 2.5f;
            useTime = 300f;
            hasBoost = false;
            consumeItems(with(Items.phaseFabric, 1, Items.silicon, 1));
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

            itemConsumer = consumeItem(Items.phaseFabric).boost();
            consumePower(4f);
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

        radar = new Radar("radar"){{
            requirements(Category.effect, BuildVisibility.fogOnly, with(Items.silicon, 30, Items.graphite, 30));
            outlineColor = Color.valueOf("4a4b53");
            fogRadius = 27;

            consumePower(0.2f);
        }};

        buildTower = new BuildTurret("build-tower"){{
            requirements(Category.effect, with(Items.silicon, 80, Items.carbide, 30, Items.oxide, 40, Items.thorium, 30));
            outlineColor = Pal.darkOutline;
            consumePower(3f);
            range = 160f;
            size = 3;
            buildSpeed = 1.5f;
        }};

        regenProjector = new RegenProjector("regen-projector"){{
            requirements(Category.effect, with(Items.silicon, 80, Items.tungsten, 60, Items.oxide, 40, Items.beryllium, 80));
            size = 3;
            consumePower(1f);
            range = 28;
            baseColor = Pal.regen;

            consumeLiquid(Liquids.hydrogen, 1f / 60f);

            consumeItem(Items.phaseFabric).boost();

            healPercent = 4f / 60f;

            Color col = Color.valueOf("8ca9e8");

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidTile(Liquids.hydrogen, 9f / 4f), new DrawBlock(), new DrawGlowRegion(){{
                color = Color.sky;
            }}, new DrawPulseShape(false){{
                layer = Layer.effect;
                color = col;
            }}, new DrawShape(){{
                layer = Layer.effect;
                radius = 3.5f;
                useWarmupRadius = true;
                timeScl = 2f;
                color = col;
            }});
        }};

        //TODO implement
        if(false)
        barrierProjector = new DirectionalForceProjector("barrier-projector"){{
            requirements(Category.effect, with(Items.surgeAlloy, 100, Items.silicon, 125));
            size = 3;
            width = 50f;
            length = 36;
            shieldHealth = 2000f;
            cooldownNormal = 3f;
            cooldownBrokenBase = 0.35f;

            consumePower(4f);
        }};

        //TODO 5x5??
        shieldProjector = new BaseShield("shield-projector"){{
            requirements(Category.effect, BuildVisibility.editorOnly, with());

            size = 3;

            consumePower(5f);
        }};

        largeShieldProjector = new BaseShield("large-shield-projector"){{
            requirements(Category.effect, BuildVisibility.editorOnly, with());

            size = 4;
            radius = 400f;

            consumePower(5f);
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
            envEnabled |= Env.space;
            consumePower(0.30f);
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
            buildCostMultiplier = 3f;
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
            consumePower(1.75f);
        }};

        //erekir transport blocks

        duct = new Duct("duct"){{
            requirements(Category.distribution, with(Items.beryllium, 1));
            //TODO bad idea? should this just require 1 beryllium?
            health = 90;
            speed = 4f;
            researchCost = with(Items.beryllium, 5);
        }};

        ductRouter = new DuctRouter("duct-router"){{
            requirements(Category.distribution, with(Items.beryllium, 10));
            health = 90;
            speed = 4f;
            regionRotated1 = 1;
            researchCost = with(Items.beryllium, 30);
        }};

        overflowDuct = new OverflowDuct("overflow-duct"){{
            //TODO should it use beryllium?
            requirements(Category.distribution, with(Items.graphite, 8, Items.beryllium, 8));
            health = 90;
            speed = 4f;
            researchCostMultiplier = 1.5f;
        }};

        ductBridge = new DuctBridge("duct-bridge"){{
            requirements(Category.distribution, with(Items.beryllium, 20));
            health = 90;
            speed = 4f;
            researchCostMultiplier = 0.3f;
        }};

        ductUnloader = new DirectionalUnloader("duct-unloader"){{
            requirements(Category.distribution, with(Items.graphite, 20, Items.silicon, 20, Items.tungsten, 10));
            health = 120;
            speed = 4f;
            regionRotated1 = 1;
        }};

        surgeConveyor = new StackConveyor("surge-conveyor"){{
            requirements(Category.distribution, with(Items.surgeAlloy, 3, Items.oxide, 5));
            health = 130;
            //TODO different base speed/item capacity?
            speed = 5f / 60f;
            itemCapacity = 10;

            outputRouter = false;
            hasPower = true;
            consumesPower = true;
            conductivePower = true;
            baseEfficiency = 1f;
            consumePower(1f / 60f);
        }};

        surgeRouter = new StackRouter("surge-router"){{
            requirements(Category.distribution, with(Items.oxide, 10, Items.surgeAlloy, 10));
            health = 130;

            speed = 6f;

            hasPower = true;
            consumesPower = true;
            conductivePower = true;
            baseEfficiency = 1f;
            consumePower(3f / 60f);
        }};

        unitCargoLoader = new UnitCargoLoader("unit-cargo-loader"){{
            requirements(Category.distribution, with(Items.silicon, 80, Items.phaseFabric, 60, Items.carbide, 50, Items.oxide, 40));

            size = 3;
            buildTime = 60f * 8f;

            consumePower(8f / 60f);

            //intentionally set absurdly high to make this block not overpowered
            consumeLiquid(Liquids.gallium, 20f / 60f);

            itemCapacity = 200;
        }};

        unitCargoUnloadPoint = new UnitCargoUnloadPoint("unit-cargo-unload-point"){{
            requirements(Category.distribution, with(Items.silicon, 60, Items.thorium, 80));

            size = 2;

            itemCapacity = 100;
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
            consumePower(0.3f);
            liquidCapacity = 30f;
            hasPower = true;
            size = 2;
        }};

        impulsePump = new Pump("impulse-pump"){{
            requirements(Category.liquid, with(Items.copper, 80, Items.metaglass, 90, Items.silicon, 30, Items.titanium, 40, Items.thorium, 35));
            pumpAmount = 0.22f;
            consumePower(1.3f);
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
            newDrawing = true;
        }};

        liquidContainer = new LiquidRouter("liquid-container"){{
            requirements(Category.liquid, with(Items.titanium, 10, Items.metaglass, 15));
            liquidCapacity = 700f;
            size = 2;
            newDrawing = true;
        }};

        liquidTank = new LiquidRouter("liquid-tank"){{
            requirements(Category.liquid, with(Items.titanium, 30, Items.metaglass, 40));
            size = 3;
            liquidCapacity = 1800f;
            health = 500;
            newDrawing = true;
        }};

        liquidJunction = new LiquidJunction("liquid-junction"){{
            requirements(Category.liquid, with(Items.graphite, 4, Items.metaglass, 8));
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
            consumePower(0.30f);
        }};

        //reinforced stuff

        //TODO different name
        reinforcedPump = new Pump("reinforced-pump"){{
            requirements(Category.liquid, with(Items.beryllium, 40, Items.tungsten, 30, Items.silicon, 20));
            //TODO CUSTOM DRAW ANIMATION - pistons - repurpose DrawBlock?
            consumeLiquid(Liquids.hydrogen, 1.5f / 60f);

            pumpAmount = 80f / 60f / 4f;
            liquidCapacity = 40f;
            size = 2;
        }};

        reinforcedConduit = new ArmoredConduit("reinforced-conduit"){{
            requirements(Category.liquid, with(Items.beryllium, 2));
            botColor = Pal.darkestMetal;
            leaks = true;
            liquidCapacity = 20f;
            liquidPressure = 1.03f;
            health = 250;
            researchCostMultiplier = 6;
        }};

        //TODO is this necessary? junctions are not good design
        //TODO make it leak
        reinforcedLiquidJunction = new LiquidJunction("reinforced-liquid-junction"){{
            requirements(Category.liquid, with(Items.graphite, 4, Items.beryllium, 8));
            buildCostMultiplier = 3f;
            health = 260;
            ((Conduit)reinforcedConduit).junctionReplacement = this;
            researchCostMultiplier = 6;
        }};

        reinforcedBridgeConduit = new DirectionLiquidBridge("reinforced-bridge-conduit"){{
            requirements(Category.liquid, with(Items.graphite, 6, Items.beryllium, 10));
            range = 4;
            hasPower = false;
            researchCostMultiplier = 6;

            ((Conduit)reinforcedConduit).rotBridgeReplacement = this;
        }};

        reinforcedLiquidRouter = new LiquidRouter("reinforced-liquid-router"){{
            requirements(Category.liquid, with(Items.tungsten, 4, Items.beryllium, 4));
            liquidCapacity = 30f;
            newDrawing = true;
            liquidPadding = 3f/4f;
            researchCostMultiplier = 6;
        }};

        //TODO is there a need for a container if unloaders can unload 3x3s?
        reinforcedLiquidContainer = new LiquidRouter("reinforced-liquid-container"){{
            requirements(Category.liquid, with(Items.tungsten, 10, Items.beryllium, 16));
            liquidCapacity = 1000f;
            size = 2;
            newDrawing = true;
            liquidPadding = 6f/4f;
            researchCostMultiplier = 4;
        }};

        reinforcedLiquidTank = new LiquidRouter("reinforced-liquid-tank"){{
            requirements(Category.liquid, with(Items.tungsten, 40, Items.beryllium, 50));
            size = 3;
            liquidCapacity = 2700f;
            newDrawing = true;
            liquidPadding = 2f;
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
            consumePowerBuffered(4000f);
            baseExplosiveness = 1f;
        }};

        batteryLarge = new Battery("battery-large"){{
            requirements(Category.power, with(Items.titanium, 20, Items.lead, 50, Items.silicon, 30));
            size = 3;
            consumePowerBuffered(50000f);
            baseExplosiveness = 5f;
        }};

        combustionGenerator = new ConsumeGenerator("combustion-generator"){{
            requirements(Category.power, with(Items.copper, 25, Items.lead, 15));
            powerProduction = 1f;
            itemDuration = 120f;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.03f;
            generateEffect = Fx.generatespark;

            consume(new ConsumeItemFlammable());
            consume(new ConsumeItemExplode());

            drawer = new DrawMulti(new DrawBlock(), new DrawWarmupRegion());
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

        steamGenerator = new ConsumeGenerator("steam-generator"){{
            requirements(Category.power, with(Items.copper, 35, Items.graphite, 25, Items.lead, 40, Items.silicon, 30));
            powerProduction = 5.5f;
            itemDuration = 90f;
            consumeLiquid(Liquids.water, 0.1f);
            hasLiquids = true;
            size = 2;
            drawer.iconOverride = new String[]{"", "-turbine0", "-turbine1"};
            generateEffect = Fx.generatespark;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.06f;

            consume(new ConsumeItemFlammable());
            consume(new ConsumeItemExplode());

            drawer = new DrawMulti(new DrawBlock(), new DrawWarmupRegion(), new DrawTurbines());
        }};

        differentialGenerator = new ConsumeGenerator("differential-generator"){{
            requirements(Category.power, with(Items.copper, 70, Items.titanium, 50, Items.lead, 100, Items.silicon, 65, Items.metaglass, 50));
            powerProduction = 18f;
            itemDuration = 220f;
            hasLiquids = true;
            hasItems = true;
            size = 3;
            ambientSound = Sounds.steam;
            generateEffect = Fx.generatespark;
            ambientSoundVolume = 0.03f;

            drawer = new DrawMulti(new DrawBlock(), new DrawWarmupRegion());

            consumeItem(Items.pyratite);
            consumeLiquid(Liquids.cryofluid, 0.1f);
        }};

        rtgGenerator = new ConsumeGenerator("rtg-generator"){{
            requirements(Category.power, with(Items.lead, 100, Items.silicon, 75, Items.phaseFabric, 25, Items.plastanium, 75, Items.thorium, 50));
            size = 2;
            powerProduction = 4.5f;
            itemDuration = 60 * 14f;
            envEnabled = Env.any;
            generateEffect = Fx.generatespark;

            drawer = new DrawMulti(new DrawBlock(), new DrawWarmupRegion());
            consume(new ConsumeItemRadioactive());
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
            heating = 0.02f;

            consumeItem(Items.thorium);
            //TODO how to non update
            consumeLiquid(Liquids.cryofluid, heating / coolantPower).update(false);
        }};

        impactReactor = new ImpactReactor("impact-reactor"){{
            requirements(Category.power, with(Items.lead, 500, Items.silicon, 300, Items.graphite, 400, Items.thorium, 100, Items.surgeAlloy, 250, Items.metaglass, 250));
            size = 4;
            health = 900;
            powerProduction = 130f;
            itemDuration = 140f;
            ambientSound = Sounds.pulse;
            ambientSoundVolume = 0.07f;

            consumePower(25f);
            consumeItem(Items.blastCompound);
            consumeLiquid(Liquids.cryofluid, 0.25f);
        }};

        //erekir

        beamNode = new BeamNode("beam-node"){{
            requirements(Category.power, with(Items.beryllium, 8));
            consumesPower = outputsPower = true;
            health = 90;
            consumePowerBuffered(1000f);
            range = 10;
            researchCost = with(Items.beryllium, 5);
        }};

        //TODO requirements
        beamTower = new BeamNode("beam-tower"){{
            requirements(Category.power, with(Items.beryllium, 30, Items.oxide, 20, Items.silicon, 10));
            size = 3;
            consumesPower = outputsPower = true;
            consumePowerBuffered(40000f);
            range = 23;
            scaledHealth = 90;
        }};

        beamLink = new LongPowerNode("beam-link"){{
            requirements(Category.power, BuildVisibility.editorOnly, with());
            size = 3;
            maxNodes = 1;
            laserRange = 1000f;
            autolink = false;
            laserColor2 = Color.valueOf("ffd9c2");
            laserScale = 0.8f;
            scaledHealth = 130;
        }};

        turbineCondenser = new ThermalGenerator("turbine-condenser"){{
            requirements(Category.power, with(Items.beryllium, 60));
            attribute = Attribute.steam;
            displayEfficiencyScale = 1f / 9f;
            minEfficiency = 9f - 0.0001f;
            powerProduction = 3f / 9f;
            displayEfficiency = false;
            generateEffect = Fx.turbinegenerate;
            effectChance = 0.04f;
            size = 3;
            ambientSound = Sounds.hum;
            ambientSoundVolume = 0.06f;
            spinSpeed = 0.6f;
            spinners = true;
            hasLiquids = true;
            outputLiquid = new LiquidStack(Liquids.water, 5f / 60f / 9f);
            liquidCapacity = 20f;
            researchCost = with(Items.beryllium, 15);
        }};

        //TODO rename
        chemicalCombustionChamber = new ConsumeGenerator("chemical-combustion-chamber"){{
            requirements(Category.power, with(Items.graphite, 40, Items.tungsten, 40, Items.oxide, 40f, Items.silicon, 30));
            powerProduction = 9f;
            consumeLiquids(LiquidStack.with(Liquids.ozone, 2f / 60f, Liquids.arkycite, 40f / 60f));
            size = 3;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawPistons(){{
                sinMag = 3f;
                sinScl = 5f;
            }}, new DrawRegion("-mid"), new DrawLiquidTile(Liquids.arkycite, 37f / 4f), new DrawBlock(), new DrawGlowRegion(){{
                alpha = 1f;
                glowScale = 5f;
                color = Color.valueOf("c967b099");
            }});
            drawer.iconOverride = new String[]{"-bottom", ""};
            generateEffect = Fx.none;

            liquidCapacity = 20f * 5;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.06f;
        }};

        //TODO cooler name?
        pyrolysisGenerator = new ConsumeGenerator("pyrolysis-generator"){{
            //TODO requirements
            requirements(Category.power, with(Items.graphite, 50, Items.carbide, 50, Items.oxide, 60f, Items.silicon, 50));
            powerProduction = 18f;

            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawPistons(){{
                sinMag = 2.75f;
                sinScl = 5f;
                sides = 8;
                sideOffset = Mathf.PI / 2f;
            }}, new DrawRegion("-mid"), new DrawLiquidTile(Liquids.arkycite, 38f / 4f), new DrawBlock(), new DrawGlowRegion(){{
                alpha = 1f;
                glowScale = 5f;
                color = Pal.slagOrange;
            }});

            //TODO ratios, extra requirements?
            consumeLiquids(LiquidStack.with(Liquids.slag, 20f / 60f, Liquids.arkycite, 30f / 60f));
            size = 3;

            liquidCapacity = 30f * 5;

            liquidOutput = new LiquidStack(Liquids.water, 5f / 60f);

            drawer.iconOverride = new String[]{"-bottom", ""};
            generateEffect = Fx.none;

            ambientSound = Sounds.smelter;
            ambientSoundVolume = 0.06f;
        }};

        //endregion power
        //region production

        mechanicalDrill = new Drill("mechanical-drill"){{
            requirements(Category.production, with(Items.copper, 12), true);
            tier = 2;
            drillTime = 600;
            size = 2;
            //mechanical drill doesn't work in space
            envEnabled ^= Env.space;

            consumeLiquid(Liquids.water, 0.05f).boost();
        }};

        pneumaticDrill = new Drill("pneumatic-drill"){{
            requirements(Category.production, with(Items.copper, 18, Items.graphite, 10));
            tier = 3;
            drillTime = 400;
            size = 2;

            consumeLiquid(Liquids.water, 0.06f).boost();
        }};

        laserDrill = new Drill("laser-drill"){{
            requirements(Category.production, with(Items.copper, 35, Items.graphite, 30, Items.silicon, 30, Items.titanium, 20));
            drillTime = 280;
            size = 3;
            hasPower = true;
            tier = 4;
            updateEffect = Fx.pulverizeMedium;
            drillEffect = Fx.mineBig;

            consumePower(1.10f);
            consumeLiquid(Liquids.water, 0.08f).boost();
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

            consumePower(3f);
            consumeLiquid(Liquids.water, 0.1f).boost();
        }};

        waterExtractor = new SolidPump("water-extractor"){{
            requirements(Category.production, with(Items.metaglass, 30, Items.graphite, 30, Items.lead, 30, Items.copper, 30));
            result = Liquids.water;
            pumpAmount = 0.11f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;
            attribute = Attribute.water;
            envRequired |= Env.groundWater;

            consumePower(1.5f);
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

            consumePower(80f / 60f);
            consumeLiquid(Liquids.water, 18f / 60f);
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

            consumeItem(Items.sand);
            consumePower(3f);
            consumeLiquid(Liquids.water, 0.15f);
        }};

        //TODO output heat?
        ventCondenser = new AttributeCrafter("vent-condenser"){{
            requirements(Category.production, with(Items.graphite, 20, Items.beryllium, 60));
            attribute = Attribute.steam;
            minEfficiency = 9f - 0.0001f;
            baseEfficiency = 0f;
            displayEfficiency = false;
            craftEffect = Fx.turbinegenerate;
            drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawBlurSpin("-rotator", 6f), new DrawRegion("-mid"), new DrawLiquidTile(Liquids.water, 38f / 4f), new DrawBlock());
            drawer.iconOverride = new String[]{"-bottom", "-rotator", ""};
            craftTime = 120f;
            size = 3;
            ambientSound = Sounds.hum;
            ambientSoundVolume = 0.06f;
            hasLiquids = true;
            boostScale = 1f / 9f;
            outputLiquid = new LiquidStack(Liquids.water, 30f / 60f);
            consumePower(0.5f);
            liquidCapacity = 60f;
        }};

        cliffCrusher = new WallCrafter("cliff-crusher"){{
            requirements(Category.production, with(Items.graphite, 25, Items.beryllium, 20));

            consumePower(11 / 60f);

            drillTime = 110f;
            size = 2;
            attribute = Attribute.sand;
            output = Items.sand;
            researchCost = with(Items.beryllium, 100, Items.graphite, 40);
        }};

        plasmaBore = new BeamDrill("plasma-bore"){{
            requirements(Category.production, with(Items.beryllium, 40));
            consumePower(0.15f);
            drillTime = 160f;
            tier = 3;
            size = 2;
            range = 4;
            researchCost = with(Items.beryllium, 10);

            consumeLiquid(Liquids.hydrogen, 0.25f / 60f).boost();
        }};

        //TODO awful name
        largePlasmaBore = new BeamDrill("large-plasma-bore"){{
            //TODO requirements
            requirements(Category.production, with(Items.silicon, 100, Items.oxide, 30, Items.beryllium, 100, Items.tungsten, 70));
            consumePower(0.8f);
            drillTime = 120f;
            tier = 5;
            size = 3;
            range = 6;
            laserWidth = 0.7f;
            itemCapacity = 20;

            consumeLiquid(Liquids.hydrogen, 0.5f / 60f);
            consumeLiquid(Liquids.nitrogen, 4f / 60f).boost();
        }};

        //TODO should be crusher or something
        impactDrill = new BurstDrill("impact-drill"){{
            requirements(Category.production, with(Items.silicon, 70, Items.beryllium, 90, Items.graphite, 60));
            drillTime = 60f * 12f;
            size = 4;
            hasPower = true;
            tier = 6;
            drillEffect = new MultiEffect(Fx.mineImpact, Fx.drillSteam, Fx.mineImpactWave.wrap(Pal.redLight, 40f));
            shake = 4f;
            itemCapacity = 40;
            //can't mine thorium for balance reasons, needs better drill
            blockedItem = Items.thorium;

            consumePower(160f / 60f);
            consumeLiquid(Liquids.water, 0.2f);
        }};

        //TODO bad name
        eruptionDrill = new BurstDrill("eruption-drill"){{
            requirements(Category.production, with(Items.silicon, 200, Items.beryllium, 250, Items.oxide, 80, Items.tungsten, 200, Items.carbide, 120));
            drillTime = 60f * 9f;
            size = 5;
            hasPower = true;
            tier = 7;
            //TODO better effect
            drillEffect = new MultiEffect(
                Fx.mineImpact,
                Fx.drillSteam,
                Fx.dynamicSpikes.wrap(Liquids.hydrogen.color, 30f),
                Fx.mineImpactWave.wrap(Liquids.hydrogen.color, 45f)
            );
            shake = 4f;
            itemCapacity = 50;
            arrowOffset = 2f;
            arrowSpacing = 5f;
            arrows = 2;
            glowColor.a = 0.6f;

            //TODO different requirements
            consumePower(6f);
            consumeLiquids(LiquidStack.with(Liquids.water, 0.4f, Liquids.hydrogen, 4f / 60f));
        }};

        //endregion
        //region storage

        coreShard = new CoreBlock("core-shard"){{
            requirements(Category.effect, BuildVisibility.editorOnly, with(Items.copper, 1000, Items.lead, 800));
            alwaysUnlocked = true;

            isFirstTier = true;
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

        coreBastion = new CoreBlock("core-bastion"){{
            //TODO cost
            requirements(Category.effect, with(Items.graphite, 1000, Items.silicon, 1000, Items.beryllium, 800));

            isFirstTier = true;
            unitType = UnitTypes.evoke;
            health = 7000;
            itemCapacity = 2000;
            size = 4;
            thrusterLength = 34/4f;
            armor = 5f;
            alwaysUnlocked = true;

            //TODO should this be higher?
            buildCostMultiplier = 0.75f;

            unitCapModifier = 30;
            researchCostMultiplier = 0.07f;
        }};

        coreCitadel = new CoreBlock("core-citadel"){{
            requirements(Category.effect, with(Items.silicon, 7000, Items.beryllium, 7000, Items.tungsten, 4000, Items.oxide, 2500));

            unitType = UnitTypes.incite;
            health = 18000;
            itemCapacity = 3000;
            size = 5;
            thrusterLength = 40/4f;
            armor = 10f;

            unitCapModifier = 30;
            researchCostMultipliers.put(Items.silicon, 0.4f);
            researchCostMultiplier = 0.14f;
        }};

        coreAcropolis = new CoreBlock("core-acropolis"){{
            //TODO cost
            requirements(Category.effect, with(Items.beryllium, 12000, Items.silicon, 11000, Items.tungsten, 9000, Items.carbide, 10000, Items.oxide, 8000));

            unitType = UnitTypes.emanate;
            health = 30000;
            itemCapacity = 4000;
            size = 6;
            thrusterLength = 48/4f;
            armor = 15f;

            unitCapModifier = 30;
            researchCostMultipliers.put(Items.silicon, 0.3f);
            researchCostMultiplier = 0.2f;
        }};

        container = new StorageBlock("container"){{
            requirements(Category.effect, with(Items.titanium, 100));
            size = 2;
            itemCapacity = 300;
            scaledHealth = 55;
        }};

        vault = new StorageBlock("vault"){{
            requirements(Category.effect, with(Items.titanium, 250, Items.thorium, 125));
            size = 3;
            itemCapacity = 1000;
            scaledHealth = 55;
        }};

        //TODO move tabs?
        unloader = new Unloader("unloader"){{
            requirements(Category.effect, with(Items.titanium, 25, Items.silicon, 30));
            speed = 60f / 11f;
            group = BlockGroup.transportation;
        }};

        reinforcedContainer = new StorageBlock("reinforced-container"){{
            requirements(Category.effect, with(Items.tungsten, 80, Items.graphite, 80));
            size = 2;
            itemCapacity = 250;
            scaledHealth = 120;
            coreMerge = false;
        }};

        reinforcedVault = new StorageBlock("reinforced-vault"){{
            requirements(Category.effect, with(Items.tungsten, 125, Items.thorium, 70, Items.beryllium, 100));
            size = 3;
            itemCapacity = 900;
            scaledHealth = 120;
            coreMerge = false;
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

            shoot = new ShootAlternate(){{
                spread = 3.5f;
            }};

            shootY = 3f;
            reload = 20f;
            restitution = 0.03f;
            range = 110;
            shootCone = 15f;
            ammoUseEffect = Fx.casing1;
            health = 250;
            inaccuracy = 2f;
            rotateSpeed = 10f;
            coolant = consume(new ConsumeCoolant(0.1f));

            limitRange();
        }};

        scatter = new ItemTurret("scatter"){{
            requirements(Category.turret, with(Items.copper, 85, Items.lead, 45));
            ammo(
                Items.scrap, Bullets.flakScrap,
                Items.lead, Bullets.flakLead,
                Items.metaglass, Bullets.flakGlass
            );
            reload = 18f;
            range = 220f;
            size = 2;
            targetGround = false;

            shoot.shotDelay = 5f;
            shoot.shots = 2;

            recoilAmount = 2f;
            rotateSpeed = 15f;
            inaccuracy = 17f;
            shootCone = 35f;

            scaledHealth = 200;
            shootSound = Sounds.shootSnap;
            coolant = consume(new ConsumeCoolant(0.2f));

            limitRange(2);
        }};

        scorch = new ItemTurret("scorch"){{
            requirements(Category.turret, with(Items.copper, 25, Items.graphite, 22));
            ammo(
                Items.coal, Bullets.basicFlame,
                Items.pyratite, Bullets.pyraFlame
            );
            recoilAmount = 0f;
            reload = 6f;
            coolantMultiplier = 1.5f;
            range = 60f;
            shootCone = 50f;
            targetAir = false;
            ammoUseEffect = Fx.none;
            health = 400;
            shootSound = Sounds.flame;
            coolant = consume(new ConsumeCoolant(0.1f));
        }};

        hail = new ItemTurret("hail"){{
            requirements(Category.turret, with(Items.copper, 40, Items.graphite, 17));
            ammo(
                Items.graphite, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artilleryIncendiary
            );
            targetAir = false;
            reload = 60f;
            recoilAmount = 2f;
            range = 235f;
            inaccuracy = 1f;
            shootCone = 10f;
            health = 260;
            shootSound = Sounds.bang;
            coolant = consume(new ConsumeCoolant(0.1f));
            limitRange(0f);
        }};

        wave = new LiquidTurret("wave"){{
            requirements(Category.turret, with(Items.metaglass, 45, Items.lead, 75, Items.copper, 25));
            ammo(
                Liquids.water, Bullets.waterShot,
                Liquids.slag, Bullets.slagShot,
                Liquids.cryofluid, Bullets.cryoShot,
                Liquids.oil, Bullets.oilShot
            );
            size = 2;
            recoilAmount = 0f;
            reload = 3f;
            inaccuracy = 5f;
            shootCone = 50f;
            liquidCapacity = 10f;
            shootEffect = Fx.shootLiquid;
            range = 110f;
            scaledHealth = 250;
            flags = EnumSet.of(BlockFlag.turret, BlockFlag.extinguisher);
        }};

        //TODO these may work in space, but what's the point?
        lancer = new PowerTurret("lancer"){{
            requirements(Category.turret, with(Items.copper, 60, Items.lead, 70, Items.silicon, 50));
            range = 165f;

            shoot.firstShotDelay = 40f;

            recoilAmount = 2f;
            reload = 80f;
            cooldown = 0.03f;
            shootShake = 2f;
            shootEffect = Fx.lancerLaserShoot;
            smokeEffect = Fx.none;
            heatColor = Color.red;
            size = 2;
            scaledHealth = 280;
            targetAir = false;
            moveWhileCharging = false;
            shootSound = Sounds.laser;
            coolant = consume(new ConsumeCoolant(0.2f));

            consumePower(6f);

            shootType = new LaserBulletType(140){{
                colors = new Color[]{Pal.lancerLaser.cpy().a(0.4f), Pal.lancerLaser, Color.white};
                //TODO merge
                chargeEffect = new MultiEffect(Fx.lancerLaserCharge, Fx.lancerLaserChargeBegin);

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
            reload = 35f;
            shootCone = 40f;
            rotateSpeed = 8f;
            targetAir = false;
            range = 90f;
            shootEffect = Fx.lightningShoot;
            heatColor = Color.red;
            recoilAmount = 1f;
            size = 1;
            health = 260;
            shootSound = Sounds.spark;
            consumePower(3.3f);
            coolant = consume(new ConsumeCoolant(0.1f));
        }};

        parallax = new TractorBeamTurret("parallax"){{
            requirements(Category.turret, with(Items.silicon, 120, Items.titanium, 90, Items.graphite, 30));

            hasPower = true;
            size = 2;
            force = 12f;
            scaledForce = 6f;
            range = 240f;
            damage = 0.3f;
            scaledHealth = 160;
            rotateSpeed = 10;

            consumePower(3f);
        }};

        swarmer = new ItemTurret("swarmer"){{
            requirements(Category.turret, with(Items.graphite, 35, Items.titanium, 35, Items.plastanium, 45, Items.silicon, 30));
            ammo(
                Items.blastCompound, Bullets.missileExplosive,
                Items.pyratite, Bullets.missileIncendiary,
                Items.surgeAlloy, Bullets.missileSurge
            );

            shoot = new ShootAlternate(){{
                shots = 4;
                barrels = 3;
                spread = 3.5f;
                shotDelay = 5f;
            }};

            shootY = 7f;
            reload = 30f;
            inaccuracy = 10f;
            range = 240f;
            size = 2;
            scaledHealth = 300;
            shootSound = Sounds.missile;
            envEnabled |= Env.space;

            limitRange(5f);
            coolant = consume(new ConsumeCoolant(0.3f));
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
            reload = 31f;
            restitution = 0.03f;
            ammoEjectBack = 3f;
            cooldown = 0.03f;
            recoilAmount = 3f;
            shootShake = 1f;
            shoot.shots = 4;
            shoot.shotDelay = 3f;

            ammoUseEffect = Fx.casing2;
            scaledHealth = 240;
            shootSound = Sounds.shootBig;

            limitRange();
            coolant = consume(new ConsumeCoolant(0.2f));
        }};

        segment = new PointDefenseTurret("segment"){{
            requirements(Category.turret, with(Items.silicon, 130, Items.thorium, 80, Items.phaseFabric, 40, Items.titanium, 40));

            scaledHealth = 250;
            range = 180f;
            hasPower = true;
            consumePower(8f);
            size = 2;
            shootLength = 5f;
            bulletDamage = 30f;
            reload = 8f;
            envEnabled |= Env.space;
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
            reload = 3f;
            shoot.shots = 2;
            velocityInaccuracy = 0.1f;
            inaccuracy = 4f;
            recoilAmount = 1f;
            restitution = 0.04f;
            shootCone = 45f;
            liquidCapacity = 40f;
            shootEffect = Fx.shootLiquid;
            range = 190f;
            scaledHealth = 250;
            flags = EnumSet.of(BlockFlag.turret, BlockFlag.extinguisher);
        }};

        fuse = new ItemTurret("fuse"){{
            requirements(Category.turret, with(Items.copper, 225, Items.graphite, 225, Items.thorium, 100));

            reload = 35f;
            shootShake = 4f;
            range = 90f;
            recoilAmount = 5f;

            shoot = new ShootSpread(){{
                shots = 3;
                spread = 20f;
            }};

            restitution = 0.1f;
            shootCone = 30;
            size = 3;
            envEnabled |= Env.space;

            scaledHealth = 220;
            shootSound = Sounds.shotgun;
            coolant = consume(new ConsumeCoolant(0.3f));

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
            shoot.shots = 4;
            inaccuracy = 12f;
            reload = 60f;
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
            coolant = consume(new ConsumeCoolant(0.3f));

            scaledHealth = 130;
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
            shootY = 8.75f;
            shoot = new ShootBarrel(){{
                barrels = new float[]{
                0f, 1f, 0f,
                3f, 0f, 0f,
                -3f, 0f, 0f,
                };
            }};
            reload = 8f;
            range = 200f;
            size = 3;
            recoilAmount = 3f;
            rotateSpeed = 10f;
            inaccuracy = 10f;
            shootCone = 30f;
            shootSound = Sounds.shootSnap;
            coolant = consume(new ConsumeCoolant(0.3f));

            scaledHealth = 145;
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
            reload = 200f;
            ammoUseEffect = Fx.casing3Double;
            recoilAmount = 5f;
            restitution = 0.009f;
            cooldown = 0.009f;
            shootShake = 4f;
            size = 4;
            shootCone = 2f;
            shootSound = Sounds.railgun;
            unitSort = UnitSorts.strongest;
            envEnabled |= Env.space;

            coolantMultiplier = 0.4f;
            scaledHealth = 150;

            coolant = consume(new ConsumeCoolant(1f));
            consumePower(10f);
        }};

        spectre = new ItemTurret("spectre"){{
            requirements(Category.turret, with(Items.copper, 900, Items.graphite, 300, Items.surgeAlloy, 250, Items.plastanium, 175, Items.thorium, 250));
            ammo(
                Items.graphite, Bullets.standardDenseBig,
                Items.pyratite, Bullets.standardIncendiaryBig,
                Items.thorium, Bullets.standardThoriumBig
            );
            reload = 7f;
            coolantMultiplier = 0.5f;
            restitution = 0.1f;
            ammoUseEffect = Fx.casing3;
            range = 260f;
            inaccuracy = 3f;
            recoilAmount = 3f;
            shoot = new ShootAlternate(){{
                spread = 8f;
            }};
            shootShake = 2f;
            size = 4;
            shootCone = 24f;
            shootSound = Sounds.shootBig;

            scaledHealth = 160;
            coolant = consume(new ConsumeCoolant(1f));

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
            reload = 90f;
            firingMoveFract = 0.5f;
            shootDuration = 230f;
            shootSound = Sounds.laserbig;
            loopSound = Sounds.beam;
            loopSoundVolume = 2f;
            envEnabled |= Env.space;

            shootType = new ContinuousLaserBulletType(78){{
                length = 200f;
                hitEffect = Fx.hitMeltdown;
                hitColor = Pal.meltdownHit;
                status = StatusEffects.melting;
                drawSize = 420f;

                incendChance = 0.4f;
                incendSpread = 5f;
                incendAmount = 1;
                ammoMultiplier = 1f;
            }};

            scaledHealth = 200;
            coolant = consume(new ConsumeCoolant(0.5f));
            consumePower(17f);
        }};

        breach = new ItemTurret("breach"){{
            requirements(Category.turret, with(Items.beryllium, 150, Items.silicon, 150, Items.graphite, 300));

            Effect sfe = new MultiEffect(Fx.shootBigColor, Fx.colorSparkBig);

            ammo(
            Items.beryllium, new BasicBulletType(7.5f, 90){{
                width = 12f;
                height = 20f;
                shootEffect = sfe;
                smokeEffect = Fx.shootBigSmoke;
                ammoMultiplier = 1;
                pierce = true;
                pierceBuilding = true;
                hitColor = backColor = trailColor = Pal.berylShot;
                frontColor = Color.white;
                trailWidth = 2.1f;
                trailLength = 10;
                hitEffect = despawnEffect = Fx.hitBulletColor;
            }},
            Items.tungsten, new BasicBulletType(8f, 180){{
                width = 13f;
                height = 19f;
                shootEffect = sfe;
                smokeEffect = Fx.shootBigSmoke;
                ammoMultiplier = 1;
                reloadMultiplier = 1f;
                pierce = true;
                pierceBuilding = true;
                hitColor = backColor = trailColor = Pal.tungstenShot;
                frontColor = Color.white;
                trailWidth = 2.2f;
                trailLength = 11;
                hitEffect = despawnEffect = Fx.hitBulletColor;
                rangeChange = 40f;
            }}
            );

            coolantMultiplier = 6f;

            shootShake = 1f;
            ammoPerShot = 5;
            draw = new DrawTurret("reinforced-");
            shootY = -2;
            outlineColor = Pal.darkOutline;
            size = 3;
            envEnabled |= Env.space;
            reload = 40f;
            recoilAmount = 2f;
            restitution = 0.03f;
            range = 190;
            shootCone = 3f;
            scaledHealth = 280;
            rotateSpeed = 1.6f;
            researchCostMultiplier = 0.3f;

            coolant = consume(new ConsumeLiquid(Liquids.water, 15f / 60f));
            limitRange();
        }};

        //TODO bad name
        sublimate = new ContinuousLiquidTurret("sublimate"){{
            //TODO requirements
            requirements(Category.turret, with(Items.tungsten, 150, Items.silicon, 200, Items.oxide, 40, Items.beryllium, 400));

            draw = new DrawTurret("reinforced-"){{

                Color heatc = Color.valueOf("fa2859");
                heatColor = heatc;

                parts.addAll(
                new RegionPart("-back"){{
                    progress = PartProgress.warmup;
                    mirror = true;
                    moveRot = 40f;
                    x = 22 / 4f;
                    y = -1f / 4f;
                    moveY = 6f / 4f;
                    under = true;
                    heatColor = heatc;
                }},
                new RegionPart("-front"){{
                    progress = PartProgress.warmup;
                    mirror = true;
                    moveRot = 40f;
                    x = 20 / 4f;
                    y = 17f / 4f;
                    moveX = 1f;
                    moveY = 1f;
                    under = true;
                    heatColor = heatc;
                }},
                new RegionPart("-nozzle"){{
                    progress = PartProgress.warmup;
                    mirror = true;
                    moveX = 8f / 4f;
                    heatColor = Color.valueOf("f03b0e");
                }});
            }};
            outlineColor = Pal.darkOutline;

            liquidConsumed = 4f / 60f;

            float r = range = 130f;

            //TODO balance, set up, where is liquid/sec displayed? status effects maybe?
            ammo(
            Liquids.ozone, new ContinuousFlameBulletType(){{
                damage = 90f;
                length = r;
                knockback = 1f;

                colors = new Color[]{Color.valueOf("eb7abe").a(0.55f), Color.valueOf("e189f5").a(0.7f), Color.valueOf("907ef7").a(0.8f), Color.valueOf("91a4ff"), Color.white};
            }},
            Liquids.cyanogen, new ContinuousFlameBulletType(){{
                damage = 200f;
                rangeChange = 70f;
                length = r + rangeChange;
                knockback = 2f;

                colors = new Color[]{Color.valueOf("465ab8").a(0.55f), Color.valueOf("66a6d2").a(0.7f), Color.valueOf("89e8b6").a(0.8f), Color.valueOf("cafcbe"), Color.white};
                flareColor = Color.valueOf("89e8b6");

                lightColor = hitColor = flareColor;
            }}
            );

            scaledHealth = 330;
            shootY = 7f;
            size = 3;
        }};

        titan = new ItemTurret("titan"){{
            requirements(Category.turret, with(Items.thorium, 300, Items.tungsten, 250, Items.silicon, 300, Items.beryllium, 400));

            ammo(
            //TODO 1 more ammo type, decide on base type
            Items.thorium, new ArtilleryBulletType(2.5f, 300, "shell"){{
                hitEffect = new MultiEffect(Fx.titanExplosion, Fx.titanSmoke);
                despawnEffect = Fx.none;
                knockback = 2f;
                lifetime = 140f;
                height = 19f;
                width = 17f;
                splashDamageRadius = 65f;
                splashDamage = 350f;
                scaledSplashDamage = true;
                backColor = hitColor = trailColor = Color.valueOf("ea8878").lerp(Color.valueOf("feb380"), 0.5f);
                frontColor = Color.white;
                ammoMultiplier = 1f;

                status = StatusEffects.blasted;

                trailLength = 32;
                trailWidth = 3.35f;
                trailSinScl = 2.5f;
                trailSinMag = 0.5f;
                trailEffect = Fx.none;
                despawnShake = 7f;

                shootEffect = Fx.shootTitan;
                smokeEffect = Fx.shootSmokeTitan;

                trailInterp = v -> Math.max(Mathf.slope(v), 0.8f);
                shrinkX = 0.2f;
                shrinkY = 0.1f;
            }}
            );

            ammoPerShot = 8;
            maxAmmo = ammoPerShot * 3;
            targetAir = false;
            shootShake = 4f;
            recoilAmount = 1f;
            reload = 60f * 2.3f;
            shootY = 7f;
            rotateSpeed = 1.4f;
            minWarmup = 0.85f;
            shootWarmupSpeed = 0.07f;

            coolant = consume(new ConsumeLiquid(Liquids.water, 30f / 60f));
            coolantMultiplier = 1.5f;

            draw = new DrawTurret("reinforced-"){{
                parts.addAll(
                new RegionPart("-barrel"){{
                    progress = PartProgress.reload.curve(Interp.pow2In);
                    moveY = -5f * 4f / 3f;
                    heatColor = Color.valueOf("f03b0e");
                    mirror = false;
                }},
                new RegionPart("-side"){{
                    heatProgress = PartProgress.warmup;
                    progress = PartProgress.warmup;
                    mirror = true;
                    moveX = 2f * 4f / 3f;
                    moveY = -0.5f;
                    moveRot = -40f;
                    under = true;
                    heatColor = Color.red.cpy();
                }});
            }};

            restitution = 0.02f;
            shootWarmupSpeed = 0.08f;

            outlineColor = Pal.darkOutline;

            consumeLiquid(Liquids.hydrogen, 5f / 60f);

            scaledHealth = 300;
            range = 390f;
            size = 4;
        }};

        disperse = new ItemTurret("disperse"){{
            requirements(Category.turret, with(Items.carbide, 50, Items.oxide, 150, Items.silicon, 200, Items.beryllium, 350));

            ammo(Items.tungsten, new BasicBulletType(){{
                damage = 40;
                speed = 8.5f;
                width = height = 16;
                shrinkY = 0.3f;
                backSprite = "large-bomb-back";
                sprite = "mine-bullet";
                velocityInaccuracy = 0.11f;
                collidesGround = false;
                collidesTiles = false;
                shootEffect = Fx.shootBig2;
                smokeEffect = Fx.shootSmokeDisperse;
                frontColor = Color.white;
                backColor = trailColor = hitColor = Color.sky;
                trailChance = 0.44f;

                lifetime = 34f;
                rotationOffset = 90f;
                trailRotation = true;
                trailEffect = Fx.disperseTrail;

                hitEffect = despawnEffect = Fx.hitBulletColor;
            }});


            reload = 9f;
            shootY = 15f;
            rotateSpeed = 5f;
            shootCone = 30f;

            coolant = consume(new ConsumeLiquid(Liquids.water, 30f / 60f));

            draw = new DrawTurret("reinforced-"){{
                parts.add(new RegionPart("-side"){{
                    mirror = true;
                    under = true;
                    moveX = 1.75f;
                    moveY = -0.5f;
                }});
                parts.add(new RegionPart("-mid"){{
                    under = true;
                    moveY = -1.5f;
                    progress = PartProgress.reload;
                    heatProgress = PartProgress.reload.add(0.25f).min(PartProgress.warmup);
                    heatColor = Color.sky.cpy().a(0.9f);
                }});
                parts.add(new RegionPart("-blade"){{
                    heatProgress = PartProgress.warmup;
                    heatColor = Color.sky.cpy().a(0.9f);
                    mirror = true;
                    under = true;
                    moveY = 1f;

                    //lame
                    moveX = 0.5f;

                    //wackier variant
                    moveX = 1.5f;
                    moveRot = 8;
                }});
            }};

            shoot = new ShootAlternate(){{
                spread = 4.7f;
                shots = 4;
                barrels = 4;
            }};

            targetGround = false;
            inaccuracy = 8f;

            restitution = 0.11f;
            shootWarmupSpeed = 0.08f;

            outlineColor = Pal.darkOutline;

            scaledHealth = 420;
            range = 270f;
            size = 4;

            limitRange(-5f);
        }};

        //endregion
        //region units

        groundFactory = new UnitFactory("ground-factory"){{
            requirements(Category.units, with(Items.copper, 50, Items.lead, 120, Items.silicon, 80));
            plans = Seq.with(
                new UnitPlan(UnitTypes.dagger, 60f * 15, with(Items.silicon, 10, Items.lead, 10)),
                new UnitPlan(UnitTypes.crawler, 60f * 10, with(Items.silicon, 8, Items.coal, 10)),
                new UnitPlan(UnitTypes.nova, 60f * 40, with(Items.silicon, 30, Items.lead, 20, Items.titanium, 20))
            );
            size = 3;
            consumePower(1.2f);
        }};

        airFactory = new UnitFactory("air-factory"){{
            requirements(Category.units, with(Items.copper, 60, Items.lead, 70));
            plans = Seq.with(
                new UnitPlan(UnitTypes.flare, 60f * 15, with(Items.silicon, 15)),
                new UnitPlan(UnitTypes.mono, 60f * 35, with(Items.silicon, 30, Items.lead, 15))
            );
            size = 3;
            consumePower(1.2f);
        }};

        navalFactory = new UnitFactory("naval-factory"){{
            requirements(Category.units, with(Items.copper, 150, Items.lead, 130, Items.metaglass, 120));
            plans = Seq.with(
                new UnitPlan(UnitTypes.risso, 60f * 45f, with(Items.silicon, 20, Items.metaglass, 35)),
                new UnitPlan(UnitTypes.retusa, 60f * 50f, with(Items.silicon, 15, Items.metaglass, 25, Items.titanium, 20))
            );
            size = 3;
            consumePower(1.2f);
            floating = true;
        }};

        additiveReconstructor = new Reconstructor("additive-reconstructor"){{
            requirements(Category.units, with(Items.copper, 200, Items.lead, 120, Items.silicon, 90));

            size = 3;
            consumePower(3f);
            consumeItems(with(Items.silicon, 40, Items.graphite, 40));

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
            consumePower(6f);
            consumeItems(with(Items.silicon, 130, Items.titanium, 80, Items.metaglass, 40));

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
            consumePower(13f);
            consumeItems(with(Items.silicon, 850, Items.titanium, 750, Items.plastanium, 650));
            consumeLiquid(Liquids.cryofluid, 1f);

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
            consumePower(25f);
            consumeItems(with(Items.silicon, 1000, Items.plastanium, 600, Items.surgeAlloy, 500, Items.phaseFabric, 350));
            consumeLiquid(Liquids.cryofluid, 3f);

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
        //region units - erekir

        fabricator = new UnitFactory("fabricator"){{
            requirements(Category.units, with(Items.silicon, 200, Items.beryllium, 250));
            size = 3;
            configurable = false;
            plans.add(new UnitPlan(UnitTypes.stell, 60f * 60f, with(Items.beryllium, 100f, Items.silicon, 60f)));
            researchCost = with(Items.beryllium, 200, Items.graphite, 80, Items.silicon, 80);
            regionSuffix = "-dark";
            consumePower(2f);
        }};

        tankReconstructor = new Reconstructor("tank-reconstructor"){{
            requirements(Category.units, with(Items.graphite, 250, Items.tungsten, 120, Items.silicon, 200));
            regionSuffix = "-dark";

            size = 3;
            consumePower(3f);
            consumeLiquid(Liquids.hydrogen, 3f / 60f);
            consumeItems(with(Items.silicon, 100, Items.tungsten, 100));

            constructTime = 60f * 20f;

            upgrades.addAll(
            new UnitType[]{UnitTypes.stell, UnitTypes.locus}
            );
        }};

        mechReconstructor = new Reconstructor("mech-reconstructor"){{
            requirements(Category.units, with(Items.graphite, 250, Items.tungsten, 120, Items.silicon, 200));
            regionSuffix = "-dark";

            size = 3;
            consumePower(3f);
            consumeLiquid(Liquids.hydrogen, 3f / 60f);
            consumeItems(with(Items.silicon, 100, Items.tungsten, 100));

            constructTime = 60f * 20f;

            upgrades.addAll(
            new UnitType[]{UnitTypes.stell, UnitTypes.latum}
            );
        }};

        shipReconstructor = new Reconstructor("ship-reconstructor"){{
            requirements(Category.units, with(Items.graphite, 250, Items.tungsten, 120, Items.silicon, 200));
            regionSuffix = "-dark";

            size = 3;
            consumePower(3f);
            consumeLiquid(Liquids.hydrogen, 3f / 60f);
            consumeItems(with(Items.silicon, 100, Items.tungsten, 100));

            constructTime = 60f * 20f;

            upgrades.addAll(
            new UnitType[]{UnitTypes.stell, UnitTypes.avert}
            );
        }};

        tankAssembler = new UnitAssembler("tank-assembler"){{
            requirements(Category.units, with(Items.graphite, 600, Items.beryllium, 600, Items.oxide, 250, Items.tungsten, 400, Items.silicon, 500));
            regionSuffix = "-dark";
            size = 5;
            plans.add(new AssemblerUnitPlan(UnitTypes.vanquish, 60f * 50f, PayloadStack.list(UnitTypes.stell, 4, Blocks.tungstenWallLarge, 10)));
            consumePower(3f);
            areaSize = 13;
            researchCostMultiplier = 0.4f;

            consumeLiquid(Liquids.nitrogen, 24f / 60f);
        }};

        //TODO requirements
        shipAssembler = new UnitAssembler("ship-assembler"){{
            requirements(Category.units, with(Items.beryllium, 700, Items.oxide, 300, Items.tungsten, 500, Items.silicon, 800));
            regionSuffix = "-dark";
            size = 5;
            plans.add(new AssemblerUnitPlan(UnitTypes.quell, 60f * 60f, PayloadStack.list(UnitTypes.avert, 2, Blocks.berylliumWallLarge, 8)));
            consumePower(3f);
            areaSize = 13;
            researchCostMultiplier = 0.4f;

            consumeLiquid(Liquids.nitrogen, 24f / 60f);
        }};

        //TODO requirements
        mechAssembler = new UnitAssembler("mech-assembler"){{
            requirements(Category.units, with(Items.graphite, 500, Items.thorium, 600, Items.oxide, 200, Items.tungsten, 500, Items.silicon, 900));
            regionSuffix = "-dark";
            size = 5;
            //TODO different reqs
            plans.add(new AssemblerUnitPlan(UnitTypes.bulwark, 60f * 60f, PayloadStack.list(UnitTypes.latum, 2, Blocks.tungstenWallLarge, 10)));
            consumePower(3f);
            areaSize = 13;
            researchCostMultiplier = 0.4f;

            consumeLiquid(Liquids.nitrogen, 24f / 60f);
        }};

        //TODO 5x5
        if(false)
        basicAssemblerModule = new UnitAssemblerModule("basic-assembler-module"){{
            requirements(Category.units, with(Items.graphite, 10));
            consumePower(0.5f);
            regionSuffix = "-dark";

            size = 3;
        }};

        //TODO setup, sprite, balance... or just scrap it completely.
        if(false)
        droneCenter = new DroneCenter("drone-center"){{
            requirements(Category.units, with(Items.graphite, 10));

            size = 3;
            consumePower(3f);

            droneType = UnitTypes.effectDrone;
        }};


        //endregion
        //region payloads

        payloadConveyor = new PayloadConveyor("payload-conveyor"){{
            requirements(Category.units, with(Items.graphite, 10, Items.copper, 10));
            canOverdrive = false;
        }};

        payloadRouter = new PayloadRouter("payload-router"){{
            requirements(Category.units, with(Items.graphite, 15, Items.copper, 10));
            canOverdrive = false;
        }};

        reinforcedPayloadConveyor = new PayloadConveyor("reinforced-payload-conveyor"){{
            requirements(Category.units, with(Items.tungsten, 10));
            moveTime = 35f;
            canOverdrive = false;
            health = 800;
            researchCostMultiplier = 4f;
        }};

        reinforcedPayloadRouter = new PayloadRouter("reinforced-payload-router"){{
            requirements(Category.units, with(Items.tungsten, 15));
            moveTime = 35f;
            health = 800;
            canOverdrive = false;
            researchCostMultiplier = 4f;
        }};

        payloadMassDriver = new PayloadMassDriver("payload-mass-driver"){{
            requirements(Category.units, with(Items.tungsten, 120, Items.silicon, 120, Items.oxide, 70));
            regionSuffix = "-dark";
            size = 3;
            reloadTime = 130f;
            chargeTime = 90f;
            range = 500f;
            maxPayloadSize = 2.5f;
            consumePower(0.5f);
        }};

        payloadPropulsionTower = new PayloadMassDriver("payload-propulsion-tower"){{
            requirements(Category.units, with(Items.thorium, 300, Items.silicon, 200, Items.plastanium, 200, Items.phaseFabric, 50));
            regionSuffix = "-dark";
            size = 5;
            reloadTime = 130f;
            chargeTime = 100f;
            range = 1100f;
            maxPayloadSize = 3.5f;
            consumePower(6f);
        }};

        smallDeconstructor = new PayloadDeconstructor("small-deconstructor"){{
            requirements(Category.units, with(Items.beryllium, 100, Items.silicon, 100, Items.oxide, 40, Items.graphite, 80));
            regionSuffix = "-dark";
            itemCapacity = 100;
            consumePower(1f);
            size = 3;
            deconstructSpeed = 1f;
        }};

        deconstructor = new PayloadDeconstructor("deconstructor"){{
            requirements(Category.units, with(Items.beryllium, 250, Items.oxide, 100, Items.silicon, 250, Items.carbide, 250));
            regionSuffix = "-dark";
            itemCapacity = 250;
            consumePower(3f);
            size = 5;
            deconstructSpeed = 2f;
        }};

        constructor = new Constructor("constructor"){{
            requirements(Category.units, with(Items.silicon, 100, Items.beryllium, 150, Items.tungsten, 80));
            regionSuffix = "-dark";
            hasPower = true;
            buildSpeed = 0.3f;
            consumePower(2f);
            size = 3;
            //TODO expand this list
            filter = Seq.with(Blocks.tungstenWallLarge, Blocks.berylliumWallLarge, Blocks.reinforcedLiquidContainer, Blocks.reinforcedContainer, Blocks.beamNode);
        }};

        //yes this block is pretty much useless
        largeConstructor = new Constructor("large-constructor"){{
            requirements(Category.units, with(Items.silicon, 150, Items.oxide, 150, Items.tungsten, 200, Items.phaseFabric, 40));
            regionSuffix = "-dark";
            hasPower = true;
            buildSpeed = 0.3f;
            maxBlockSize = 4;
            minBlockSize = 3;
            size = 5;

            consumePower(2f);
        }};

        payloadLoader = new PayloadLoader("payload-loader"){{
            requirements(Category.units, with(Items.graphite, 50, Items.silicon, 50, Items.tungsten, 80));
            regionSuffix = "-dark";
            hasPower = true;
            consumePower(2f);
            size = 3;
        }};

        payloadUnloader = new PayloadUnloader("payload-unloader"){{
            requirements(Category.units, with(Items.graphite, 50, Items.silicon, 50, Items.oxide, 30));
            regionSuffix = "-dark";
            hasPower = true;
            consumePower(2f);
            size = 3;
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
            alwaysUnlocked = true;
        }};

        payloadVoid = new PayloadVoid("payload-void"){{
            requirements(Category.units, BuildVisibility.sandboxOnly, with());
            size = 5;
            alwaysUnlocked = true;
        }};

        //TODO move
        illuminator = new LightBlock("illuminator"){{
            requirements(Category.effect, BuildVisibility.lightingOnly, with(Items.graphite, 12, Items.silicon, 8, Items.lead, 8));
            brightness = 0.75f;
            radius = 140f;
            consumePower(0.05f);
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

        new LegacyCommandCenter("command-center"){{
            size = 2;
        }};

        //endregion
        //region campaign

        launchPad = new LaunchPad("launch-pad"){{
            requirements(Category.effect, BuildVisibility.campaignOnly, with(Items.copper, 350, Items.silicon, 140, Items.lead, 200, Items.titanium, 150));
            size = 3;
            itemCapacity = 100;
            launchTime = 60f * 20;
            hasPower = true;
            consumePower(4f);
        }};

        interplanetaryAccelerator = new Accelerator("interplanetary-accelerator"){{
            requirements(Category.effect, BuildVisibility.hidden, with(Items.copper, 16000, Items.silicon, 11000, Items.thorium, 13000, Items.titanium, 12000, Items.surgeAlloy, 6000, Items.phaseFabric, 5000));
            researchCostMultiplier = 0.1f;
            size = 7;
            hasPower = true;
            consumePower(10f);
            buildCostMultiplier = 0.5f;
            scaledHealth = 80;
        }};

        //endregion campaign
        //region logic

        message = new MessageBlock("message"){{
            requirements(Category.logic, with(Items.graphite, 5, Items.copper, 5));
        }};

        switchBlock = new SwitchBlock("switch"){{
            requirements(Category.logic, with(Items.graphite, 5, Items.copper, 5));
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

            consumeLiquid(Liquids.cryofluid, 0.08f);
            hasLiquids = true;

            instructionsPerTick = 25;
            range = 8 * 42;
            size = 3;
        }};

        memoryCell = new MemoryBlock("memory-cell"){{
            requirements(Category.logic, with(Items.graphite, 30, Items.silicon, 30, Items.copper, 30));

            memoryCapacity = 64;
        }};

        memoryBank = new MemoryBlock("memory-bank"){{
            requirements(Category.logic, with(Items.graphite, 80, Items.silicon, 80, Items.phaseFabric, 30, Items.copper, 30));

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

        worldProcessor = new LogicBlock("world-processor"){{
            requirements(Category.logic, BuildVisibility.editorOnly, with());

            targetable = false;
            instructionsPerTick = 8;
            forceDark = true;
            privileged = true;
            size = 1;
            maxInstructionsPerTick = 30;
        }};

        worldCell = new MemoryBlock("world-cell"){{
            requirements(Category.logic, BuildVisibility.editorOnly, with());

            privileged = true;
            memoryCapacity = 128;
        }};

        //endregion
    }
}