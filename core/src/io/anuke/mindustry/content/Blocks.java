package io.anuke.mindustry.content;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.CacheLayer;
import io.anuke.mindustry.type.Category;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.blocks.defense.*;
import io.anuke.mindustry.world.blocks.defense.turrets.*;
import io.anuke.mindustry.world.blocks.distribution.*;
import io.anuke.mindustry.world.blocks.power.*;
import io.anuke.mindustry.world.blocks.production.*;
import io.anuke.mindustry.world.blocks.sandbox.*;
import io.anuke.mindustry.world.blocks.storage.CoreBlock;
import io.anuke.mindustry.world.blocks.storage.LaunchPad;
import io.anuke.mindustry.world.blocks.storage.Unloader;
import io.anuke.mindustry.world.blocks.storage.Vault;
import io.anuke.mindustry.world.blocks.units.MechPad;
import io.anuke.mindustry.world.blocks.units.RepairPoint;
import io.anuke.mindustry.world.blocks.units.UnitFactory;
import io.anuke.mindustry.world.consumers.ConsumeItemFilter;
import io.anuke.mindustry.world.consumers.ConsumeLiquidFilter;

import static io.anuke.mindustry.Vars.*;

public class Blocks implements ContentList{
    public static Block

    //environment
    air, part, spawn, deepwater, water, tar, stone, craters, charr, sand, ice, snow,
    holostone, rocks, icerocks, cliffs, pine, whiteTree, whiteTreeDead, sporeCluster,
    iceSnow, sandWater, duneRocks, sandRocks, stainedRocks, moss, stainedRocksRed, stainedStoneRed, stainedRocksYellow, stainedStoneYellow, stainedBoulder,  grass, salt,
    metalFloor, metalFloorDamaged, metalFloor2, metalFloor3, metalFloor5, ignarock, magmarock, hotrock, snowrocks,

    //crafting
    siliconSmelter, kiln, graphitePress, plastaniumCompressor, multiPress, phaseWeaver, surgeSmelter, pyratiteMixer, blastMixer, cryofluidMixer,
    melter, separator, sporePress, pulverizer, incinerator,

    //sandbox
    powerVoid, powerSource, itemSource, liquidSource, itemVoid,

    //defense
    scrapWall, scrapWallLarge, scrapWallHuge, scrapWallGigantic, thruster, //ok, these names are getting ridiculous, but at least I don't have humongous walls yet
    copperWall, copperWallLarge, titaniumWall, titaniumWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
    phaseWall, phaseWallLarge, surgeWall, surgeWallLarge, mendProjector, overdriveProjector, forceProjector, shockMine,

    //transport
    conveyor, titaniumConveyor, distributor, junction, itemBridge, phaseConveyor, sorter, router, overflowGate, massDriver,

    //liquids
    mechanicalPump, rotaryPump, thermalPump, conduit, pulseConduit, liquidRouter, liquidTank, liquidJunction, bridgeConduit, phaseConduit,

    //power
    combustionGenerator, thermalGenerator, turbineGenerator, differentialGenerator, rtgGenerator, solarPanel, largeSolarPanel, thoriumReactor,
    impactReactor, battery, batteryLarge, powerNode, powerNodeLarge, surgeTower,

    //production
    mechanicalDrill, pneumaticDrill, laserDrill, blastDrill, waterExtractor, oilExtractor, cultivator,

    //storage
    coreShard, coreFoundation, coreNucleus, vault, container, unloader, launchPad,

    //turrets
    duo, hail, arc, wave, lancer, swarmer, salvo, fuse, ripple, cyclone, spectre, meltdown,

    //units
    spiritFactory, phantomFactory, wraithFactory, ghoulFactory, revenantFactory, daggerFactory, crawlerFactory, titanFactory,
    fortressFactory, repairPoint,

    //upgrades
    alphaDartPad, deltaPad, tauPad, omegaPad, javelinPad, tridentPad, glaivePad;

    @Override
    public void load(){
        //region environment

        air = new Floor("air"){{
                alwaysReplace = true;
            }

            public void draw(Tile tile){}
            public void load(){}
            public void init(){}
            public boolean isHidden(){ return true; }
            public TextureRegion[] variantRegions(){
                if(variantRegions == null){
                    variantRegions = new TextureRegion[]{Core.atlas.find("clear")};
                }
                return variantRegions;
            }
        };

        part = new BlockPart();

        spawn = new Block("spawn"){
            public void drawShadow(Tile tile){}
        };

        //Registers build blocks from size 1-6
        //no reference is needed here since they can be looked up by name later
        for(int i = 1; i <= 6; i++){
            new BuildBlock("build" + i);
        }

        deepwater = new Floor("deepwater"){{
            liquidColor = Color.valueOf("4d5ca4");
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            isLiquid = true;
            status = StatusEffects.wet;
            statusDuration = 120f;
            drownTime = 140f;
            cacheLayer = CacheLayer.water;
        }};

        water = new Floor("water"){{
            liquidColor = Color.valueOf("596ab8");
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
        }};

        tar = new Floor("tar"){{
            drownTime = 150f;
            liquidColor = Color.valueOf("292929");
            status = StatusEffects.tarred;
            statusDuration = 240f;
            speedMultiplier = 0.19f;
            variants = 0;
            liquidDrop = Liquids.oil;
            isLiquid = true;
            cacheLayer = CacheLayer.tar;
        }};

        stone = new Floor("stone"){{
            hasOres = true;
        }};

        craters = new Floor("craters"){{
            variants = 6;
            blendGroup = stone;
        }};

        charr = new Floor("char"){{
            blendGroup = stone;
        }};

        sandWater = new Floor("sand-water"){{
            liquidColor = Color.valueOf("596ab8");
            speedMultiplier = 0.8f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 50f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
        }};

        sand = new Floor("sand"){{
            itemDrop = Items.sand;
            hasOres = true;
            playerUnmineable = true;
        }};

        holostone = new Floor("holostone"){{
            hasOres = true;
            edgeStyle = "blocky";
        }};

        grass = new Floor("grass"){{

        }};

        salt = new Floor("salt"){{
            variants = 0;
        }};

        iceSnow = new Floor("ice-snow"){{
            variants = 3;
        }};

        snow = new Floor("snow"){{
        }};

        ice = new Floor("ice"){{
            //TODO fix
            dragMultiplier = 1f;
            speedMultiplier = 1f;
        }};

        cliffs = new StaticWall("cliffs"){{
            variants = 1;
            fillsTile = false;
        }};

        rocks = new StaticWall("rocks"){{
            variants = 2;
        }};

        icerocks = new StaticWall("icerocks"){{
            variants = 2;
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

        pine = new StaticWall("pine"){{
            //fillsTile = false;
            variants = 0;
        }};

        whiteTreeDead = new TreeBlock("white-tree-dead"){{
        }};

        whiteTree = new TreeBlock("white-tree"){{
        }};

        sporeCluster = new Rock("spore-cluster"){{
            variants = 3;
        }};

        stainedRocks = new StaticWall("stained-rocks"){{
            variants = 2;
        }};

        moss = new Floor("moss"){{
            variants = 3;
        }};

        stainedRocksRed = new StaticWall("stained-rocks-red"){{
            variants = 2;
        }};

        stainedStoneRed = new Floor("stained-stone-red"){{
            hasOres = true;
            variants = 1;
        }};

        stainedRocksYellow = new StaticWall("stained-rocks-yellow"){{
            variants = 2;
        }};

        stainedStoneYellow = new Floor("stained-stone-yellow"){{
            variants = 1;
        }};

        stainedBoulder = new Rock("stained-boulder"){{
            variants = 2;
        }};

        metalFloor = new Floor("metal-floor"){{
            variants = 0;
        }};

        metalFloorDamaged = new Floor("metal-floor-damaged"){{
            variants = 6;
            blendGroup = metalFloor;
        }};

        metalFloor2 = new Floor("metal-floor-2"){{
            variants = 0;
            blendGroup = metalFloor;
        }};

        metalFloor3 = new Floor("metal-floor-3"){{
            variants = 0;
            blendGroup = metalFloor;
        }};

        metalFloor5 = new Floor("metal-floor-5"){{
            variants = 0;
            blendGroup = metalFloor;
        }};

        ignarock = new Floor("ignarock"){{

        }};

        hotrock = new Floor("hotrock"){{
            heat = 0.5f;
            blendGroup = ignarock;
        }};

        magmarock = new Floor("magmarock"){{
            heat = 0.75f;
            updateEffect = Fx.magmasmoke;
            blendGroup = ignarock;
        }};

        //endregion
        //region crafting

        graphitePress = new GenericCrafter("graphite-press"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 150, Items.lead, 60));

            craftEffect = Fx.pulverizeMedium;
            output = Items.graphite;
            craftTime = 90f;
            size = 2;
            hasItems = true;

            consumes.item(Items.coal, 2);
        }};

        multiPress = new GenericCrafter("multi-press"){{
            requirements(Category.crafting, ItemStack.with(Items.titanium, 200, Items.silicon, 50, Items.lead, 200, Items.graphite, 100));

            craftEffect = Fx.pulverizeMedium;
            output = Items.graphite;
            craftTime = 30f;
            size = 3;
            hasItems = true;
            hasLiquids = true;
            hasPower = true;

            consumes.power(2f);
            consumes.item(Items.coal, 2);
            consumes.liquid(Liquids.water, 0.1f);
        }};

        siliconSmelter = new PowerSmelter("silicon-smelter"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 60, Items.lead, 50));
            craftEffect = Fx.smeltsmoke;
            output = Items.silicon;
            craftTime = 40f;
            size = 2;
            hasLiquids = false;
            flameColor = Color.valueOf("ffef99");

            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.sand, 2));
            consumes.power(0.50f);
        }};

        kiln = new PowerSmelter("kiln"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 120, Items.graphite, 60, Items.lead, 60));
            craftEffect = Fx.smeltsmoke;
            output = Items.metaglass;
            craftTime = 30f;
            size = 2;
            hasPower = hasItems = true;
            flameColor = Color.valueOf("ffc099");

            consumes.items(new ItemStack(Items.lead, 1), new ItemStack(Items.sand, 1));
            consumes.power(0.60f);
        }};

        plastaniumCompressor = new PlastaniumCompressor("plastanium-compressor"){{
            requirements(Category.crafting, ItemStack.with(Items.silicon, 160, Items.lead, 230, Items.graphite, 120, Items.titanium, 160));
            hasItems = true;
            liquidCapacity = 60f;
            craftTime = 60f;
            output = Items.plastanium;
            size = 2;
            health = 320;
            hasPower = hasLiquids = true;
            craftEffect = Fx.formsmoke;
            updateEffect = Fx.plasticburn;

            consumes.liquid(Liquids.oil, 0.25f);
            consumes.power(3f);
            consumes.item(Items.titanium, 2);
        }};

        phaseWeaver = new PhaseWeaver("phase-weaver"){{
            requirements(Category.crafting, ItemStack.with(Items.silicon, 260, Items.lead, 240, Items.thorium, 150));
            craftEffect = Fx.smeltsmoke;
            output = Items.phasefabric;
            craftTime = 120f;
            size = 2;

            consumes.items(new ItemStack(Items.thorium, 4), new ItemStack(Items.sand, 10));
            consumes.power(5f);
        }};

        surgeSmelter = new PowerSmelter("alloy-smelter"){{
            requirements(Category.crafting, ItemStack.with(Items.silicon, 160, Items.lead, 160, Items.thorium, 140));
            craftEffect = Fx.smeltsmoke;
            output = Items.surgealloy;
            craftTime = 75f;
            size = 3;

            consumes.power(4f);
            consumes.items(new ItemStack(Items.titanium, 2), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.copper, 3));
        }};

        cryofluidMixer = new LiquidMixer("cryofluidmixer"){{
            requirements(Category.crafting, ItemStack.with(Items.lead, 130, Items.silicon, 80, Items.thorium, 90));
            outputLiquid = Liquids.cryofluid;
            liquidPerItem = 50f;
            size = 2;
            hasPower = true;

            consumes.power(1f);
            consumes.item(Items.titanium);
            consumes.liquid(Liquids.water, 0.3f);
        }};

        blastMixer = new GenericCrafter("blast-mixer"){{
            requirements(Category.crafting, ItemStack.with(Items.lead, 60, Items.titanium, 40));
            hasItems = true;
            hasPower = true;
            output = Items.blastCompound;
            size = 2;

            consumes.items(new ItemStack(Items.pyratite, 1), new ItemStack(Items.sporePod, 1));
            consumes.power(0.40f);
        }};

        pyratiteMixer = new PowerSmelter("pyratite-mixer"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 100, Items.lead, 50));
            flameColor = Color.CLEAR;
            hasItems = true;
            hasPower = true;
            output = Items.pyratite;

            size = 2;

            consumes.power(0.20f);
            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.lead, 2), new ItemStack(Items.sand, 2));
        }};

        melter = new PowerCrafter("melter"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 60, Items.lead, 70, Items.graphite, 90));
            health = 200;
            outputLiquid = Liquids.slag;
            outputLiquidAmount = 2f;
            craftTime = 10f;
            hasLiquids = hasPower = true;

            consumes.power(1f);
            consumes.item(Items.scrap, 1);
        }};

        separator = new Separator("separator"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 60, Items.titanium, 50));
            results = ItemStack.with(
                Items.copper, 5,
                Items.lead, 3,
                Items.graphite, 2,
                Items.titanium, 2
            );
            hasPower = true;
            filterTime = 35f;
            spinnerLength = 1.5f;
            spinnerRadius = 3.5f;
            spinnerThickness = 1.5f;
            spinnerSpeed = 3f;
            size = 2;

            consumes.power(1f);
            consumes.liquid(Liquids.slag, 0.07f);
        }};

        cultivator = new Cultivator("cultivator"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 20, Items.lead, 50, Items.silicon, 20));
            output = Items.sporePod;
            craftTime = 200;
            size = 2;
            hasLiquids = true;
            hasPower = true;
            hasItems = true;

            consumes.power(0.80f);
            consumes.liquid(Liquids.water, 0.15f);
        }};

        sporePress = new Compressor("spore-press"){{
            requirements(Category.crafting, ItemStack.with(Items.lead, 70, Items.silicon, 60));
            liquidCapacity = 60f;
            craftTime = 20f;
            outputLiquid = Liquids.oil;
            outputLiquidAmount = 2.5f;
            size = 2;
            health = 320;
            hasLiquids = true;

            consumes.item(Items.sporePod, 1);
            consumes.power(0.60f);
        }};

        pulverizer = new Pulverizer("pulverizer"){{
            requirements(Category.crafting, ItemStack.with(Items.copper, 60, Items.lead, 50));
            output = Items.sand;
            craftEffect = Fx.pulverize;
            craftTime = 40f;
            updateEffect = Fx.pulverizeSmall;
            hasItems = hasPower = true;

            consumes.item(Items.scrap, 1);
            consumes.power(0.50f);
        }};

        incinerator = new Incinerator("incinerator"){{
            requirements(Category.crafting, ItemStack.with(Items.graphite, 10, Items.lead, 30));
            health = 90;
            consumes.power(0.50f);
        }};

        //endregion
        //region sandbox

        powerVoid = new PowerVoid("power-void"){{
            requirements(Category.power, () -> state.rules.infiniteResources, ItemStack.with());
            alwaysUnlocked = true;
        }};
        powerSource = new PowerSource("power-source"){{
            requirements(Category.power, () -> state.rules.infiniteResources, ItemStack.with());
            alwaysUnlocked = true;
        }};
        itemSource = new ItemSource("item-source"){{
            requirements(Category.distribution, () -> state.rules.infiniteResources, ItemStack.with());
            alwaysUnlocked = true;
        }};
        itemVoid = new ItemVoid("item-void"){{
            requirements(Category.distribution, () -> state.rules.infiniteResources, ItemStack.with());
            alwaysUnlocked = true;
        }};
        liquidSource = new LiquidSource("liquid-source"){{
            requirements(Category.liquid, () -> state.rules.infiniteResources, ItemStack.with());
            alwaysUnlocked = true;
        }};

        //endregion
        //region defense

        int wallHealthMultiplier = 3;

        scrapWall = new Wall("scrap-wall"){{
            health = 60 * wallHealthMultiplier;
            variants = 5;
        }};

        scrapWallLarge = new Wall("scrap-wall-large"){{
            health = 60 * 4 * wallHealthMultiplier;
            size = 2;
            variants = 4;
        }};

        scrapWallHuge = new Wall("scrap-wall-huge"){{
            health = 60 * 9 * wallHealthMultiplier;
            size = 3;
            variants = 3;
        }};

        scrapWallGigantic = new Wall("scrap-wall-gigantic"){{
            health = 60 * 16 * wallHealthMultiplier;
            size = 4;
        }};

        thruster = new Wall("thruster"){{
            health = 55 * 16 * wallHealthMultiplier;
            size = 4;
        }};

        copperWall = new Wall("copper-wall"){{
            requirements(Category.defense, ItemStack.with(Items.copper, 12));
            health = 80 * wallHealthMultiplier;
        }};

        copperWallLarge = new Wall("copper-wall-large"){{
            requirements(Category.defense, ItemStack.with(Items.copper, 12 * 4));
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        titaniumWall = new Wall("titanium-wall"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 12));
            health = 110 * wallHealthMultiplier;
        }};

        titaniumWallLarge = new Wall("titanium-wall-large"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 12 * 4));
            health = 110 * wallHealthMultiplier * 4;
            size = 2;
        }};

        thoriumWall = new Wall("thorium-wall"){{
            requirements(Category.defense, ItemStack.with(Items.thorium, 12));
            health = 200 * wallHealthMultiplier;
        }};

        thoriumWallLarge = new Wall("thorium-wall-large"){{
            requirements(Category.defense, ItemStack.with(Items.thorium, 12 * 4));
            health = 200 * wallHealthMultiplier * 4;
            size = 2;
        }};

        phaseWall = new DeflectorWall("phase-wall"){{
            requirements(Category.defense, ItemStack.with(Items.phasefabric, 12));
            health = 150 * wallHealthMultiplier;
        }};

        phaseWallLarge = new DeflectorWall("phase-wall-large"){{
            requirements(Category.defense, ItemStack.with(Items.phasefabric, 12 * 4));
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        surgeWall = new SurgeWall("surge-wall"){{
            requirements(Category.defense, ItemStack.with(Items.surgealloy, 12));
            health = 230 * wallHealthMultiplier;
        }};

        surgeWallLarge = new SurgeWall("surge-wall-large"){{
            requirements(Category.defense, ItemStack.with(Items.surgealloy, 12 * 4));
            health = 230 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        door = new Door("door"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 12, Items.silicon, 8));
            health = 100 * wallHealthMultiplier;
        }};

        doorLarge = new Door("door-large"){{
            requirements(Category.defense, ItemStack.with(Items.titanium, 12 * 4, Items.silicon, 8 * 4));
            openfx = Fx.dooropenlarge;
            closefx = Fx.doorcloselarge;
            health = 100 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        mendProjector = new MendProjector("mend-projector"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 200, Items.titanium, 50, Items.silicon, 180));
            consumes.power(2f);
            size = 2;
            consumes.item(Items.phasefabric).optional(true);
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 200, Items.titanium, 150, Items.silicon, 150, Items.plastanium, 60));
            consumes.power(3.50f);
            size = 2;
            consumes.item(Items.phasefabric).optional(true).boost(true);
        }};

        forceProjector = new ForceProjector("force-projector"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 200, Items.titanium, 150, Items.silicon, 250));
            size = 3;
            consumes.item(Items.phasefabric).optional(true).boost(true);
        }};

        shockMine = new ShockMine("shock-mine"){{
            requirements(Category.effect, ItemStack.with(Items.lead, 50, Items.silicon, 25));
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
        }};

        titaniumConveyor = new Conveyor("titanium-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 2, Items.lead, 1, Items.titanium, 1));
            health = 65;
            speed = 0.07f;
        }};

        junction = new Junction("junction"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 3));
            speed = 26;
            capacity = 32;
            health = 25;
        }};

        itemBridge = new BufferedItemBridge("bridge-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 8, Items.copper, 8));
            range = 4;
            speed = 60f;
            bufferCapacity = 15;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            requirements(Category.distribution, ItemStack.with(Items.phasefabric, 10, Items.silicon, 15, Items.lead, 20, Items.graphite, 20));
            range = 12;
            hasPower = true;
            consumes.power(0.30f);
        }};

        sorter = new Sorter("sorter"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 4, Items.copper, 4));

        }};

        router = new Router("router"){{
            requirements(Category.distribution, ItemStack.with(Items.copper, 6));

        }};

        distributor = new Router("distributor"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 8, Items.copper, 8));
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate"){{
            requirements(Category.distribution, ItemStack.with(Items.lead, 4, Items.copper, 8));
        }};

        massDriver = new MassDriver("mass-driver"){{
            requirements(Category.distribution, ItemStack.with(Items.titanium, 250, Items.silicon, 150, Items.lead, 250, Items.thorium, 100));
            size = 3;
            itemCapacity = 120;
            reloadTime = 200f;
            range = 440f;
        }};

        //endregion
        //region liquid

        mechanicalPump = new Pump("mechanical-pump"){{
            requirements(Category.liquid, ItemStack.with(Items.copper, 30, Items.lead, 20));
            pumpAmount = 0.1f;
        }};

        rotaryPump = new Pump("rotary-pump"){{
            requirements(Category.liquid, ItemStack.with(Items.copper, 140, Items.lead, 100, Items.silicon, 40, Items.titanium, 70));
            pumpAmount = 0.8f;
            consumes.power(0.15f);
            liquidCapacity = 30f;
            hasPower = true;
            size = 2;
        }};

        thermalPump = new Pump("thermal-pump"){{
            requirements(Category.liquid, ItemStack.with(Items.copper, 160, Items.lead, 130, Items.silicon, 60, Items.titanium, 80, Items.thorium, 70));
            pumpAmount = 1.95f;
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
            requirements(Category.liquid, ItemStack.with(Items.titanium, 1, Items.metaglass, 2));
            liquidCapacity = 16f;
            liquidFlowFactor = 4.9f;
            health = 90;
        }};

        liquidRouter = new LiquidRouter("liquid-router"){{
            requirements(Category.liquid, ItemStack.with(Items.titanium, 4, Items.metaglass, 4));
            liquidCapacity = 20f;
        }};

        liquidTank = new LiquidTank("liquid-tank"){{
            requirements(Category.liquid, ItemStack.with(Items.titanium, 50, Items.metaglass, 50));
            size = 3;
            liquidCapacity = 1500f;
            health = 500;
        }};

        liquidJunction = new LiquidJunction("liquid-junction"){{
            requirements(Category.liquid, ItemStack.with(Items.titanium, 4, Items.metaglass, 4));
        }};

        bridgeConduit = new LiquidExtendingBridge("bridge-conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.titanium, 8, Items.metaglass, 8));
            range = 4;
            hasPower = false;
        }};

        phaseConduit = new LiquidBridge("phase-conduit"){{
            requirements(Category.liquid, ItemStack.with(Items.phasefabric, 10, Items.silicon, 15, Items.metaglass, 40, Items.titanium, 20));
            range = 12;
            hasPower = true;
            consumes.power(0.30f);
        }};

        //endregion
        //region power

        powerNode = new PowerNode("power-node"){{
            requirements(Category.power, ItemStack.with(Items.copper, 2, Items.lead, 6));
            maxNodes = 4;
            laserRange = 6;
        }};

        powerNodeLarge = new PowerNode("power-node-large"){{
            requirements(Category.power, ItemStack.with(Items.titanium, 10, Items.lead, 20, Items.silicon, 6));
            size = 2;
            maxNodes = 6;
            laserRange = 9.5f;
        }};

        surgeTower = new PowerNode("surge-tower"){{
            requirements(Category.power, ItemStack.with(Items.titanium, 15, Items.lead, 20, Items.silicon, 30, Items.surgealloy, 30));
            size = 2;
            maxNodes = 2;
            laserRange = 30f;
        }};

        battery = new Battery("battery"){{
            requirements(Category.power, ItemStack.with(Items.copper, 8, Items.lead, 30, Items.silicon, 4));
            consumes.powerBuffered(3200f, 1f);
        }};

        batteryLarge = new Battery("battery-large"){{
            requirements(Category.power, ItemStack.with(Items.titanium, 40, Items.lead, 80, Items.silicon, 30));
            size = 3;
            consumes.powerBuffered(20000f, 1f);
        }};

        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 50, Items.lead, 30));
            powerProduction = 1f;
            itemDuration = 40f;
        }};

        thermalGenerator = new ThermalGenerator("thermal-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 80, Items.graphite, 70, Items.lead, 100, Items.silicon, 70, Items.metaglass, 80));
            powerProduction = 1.8f;
            generateEffect = Fx.redgeneratespark;
            size = 2;
        }};

        turbineGenerator = new TurbineGenerator("turbine-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 70, Items.graphite, 50, Items.lead, 80, Items.silicon, 60));
            powerProduction = 6f;
            itemDuration = 30f;
            consumes.liquid(Liquids.water, 0.05f);
            hasLiquids = true;
            size = 2;
        }};

        differentialGenerator = new DifferentialGenerator("differential-generator"){{
            requirements(Category.power, ItemStack.with(Items.copper, 140, Items.titanium, 100, Items.lead, 200, Items.silicon, 130, Items.metaglass, 100));
            powerProduction = 13f;
            itemDuration = 50f;
            consumes.remove(ConsumeItemFilter.class);
            consumes.remove(ConsumeLiquidFilter.class);
            consumes.item(Items.pyratite);
            consumes.liquid(Liquids.cryofluid, 0.2f);
            size = 3;
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            requirements(Category.power, ItemStack.with(Items.lead, 200, Items.silicon, 150, Items.phasefabric, 50, Items.plastanium, 150, Items.thorium, 100));
            size = 2;
            powerProduction = 3f;
            itemDuration = 220f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            requirements(Category.power, ItemStack.with(Items.lead, 20, Items.silicon, 30));
            powerProduction = 0.045f;
        }};

        largeSolarPanel = new SolarGenerator("solar-panel-large"){{
            requirements(Category.power, ItemStack.with(Items.lead, 200, Items.silicon, 290, Items.phasefabric, 30));
            size = 3;
            powerProduction = 0.55f;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            requirements(Category.power, ItemStack.with(Items.lead, 600, Items.silicon, 400, Items.graphite, 300, Items.thorium, 300));
            size = 3;
            health = 700;
            powerProduction = 14f;
            consumes.item(Items.thorium);
            consumes.liquid(Liquids.cryofluid, 0.1f);
        }};

        impactReactor = new ImpactReactor("impact-reactor"){{
            requirements(Category.power, ItemStack.with(Items.lead, 1000, Items.silicon, 600, Items.graphite, 800, Items.thorium, 200, Items.surgealloy, 500, Items.metaglass, 500));
            size = 4;
            health = 900;
            powerProduction = 80f;
            useTime = 40f;
            consumes.power(23f);
            consumes.item(Items.blastCompound);
            consumes.liquid(Liquids.cryofluid, 0.8f);
        }};

        //endregion power
        //region production

        mechanicalDrill = new Drill("mechanical-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 25), true);
            tier = 2;
            drillTime = 600;
            size = 2;
            drawMineItem = true;
        }};

        pneumaticDrill = new Drill("pneumatic-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 60, Items.graphite, 50));
            tier = 3;
            drillTime = 480;
            size = 2;
            drawMineItem = true;
            consumes.liquid(Liquids.water, 0.06f).optional(true);
        }};

        laserDrill = new Drill("laser-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 70, Items.graphite, 90, Items.silicon, 60, Items.titanium, 50));
            drillTime = 280;
            size = 3;
            hasPower = true;
            tier = 4;
            updateEffect = Fx.pulverizeMedium;
            drillEffect = Fx.mineBig;

            consumes.power(1.10f);
            consumes.liquid(Liquids.water, 0.08f).optional(true);
        }};

        blastDrill = new Drill("blast-drill"){{
            requirements(Category.production, ItemStack.with(Items.copper, 130, Items.silicon, 120, Items.titanium, 100, Items.thorium, 60));
            drillTime = 200;
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
            consumes.liquid(Liquids.water, 0.1f).optional(true);
        }};

        waterExtractor = new SolidPump("water-extractor"){{
            requirements(Category.production, ItemStack.with(Items.copper, 50, Items.graphite, 50, Items.lead, 40));
            result = Liquids.water;
            pumpAmount = 0.13f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;

            consumes.power(0.90f);
        }};

        oilExtractor = new Fracker("oil-extractor"){{
            requirements(Category.production, ItemStack.with(Items.copper, 300, Items.graphite, 350, Items.lead, 230, Items.thorium, 230, Items.silicon, 150));
            result = Liquids.oil;
            updateEffect = Fx.pulverize;
            liquidCapacity = 50f;
            updateEffectChance = 0.05f;
            pumpAmount = 0.25f;
            size = 3;
            liquidCapacity = 30f;

            consumes.item(Items.sand);
            consumes.power(3f);
            consumes.liquid(Liquids.water, 0.15f);
        }};

        //endregion
        //region storage

        coreShard = new CoreBlock("core-shard"){{
            requirements(Category.effect, () -> false, ItemStack.with(Items.titanium, 2000));
            alwaysUnlocked = true;

            health = 1100;
            itemCapacity = 5000;
            size = 3;
        }};

        coreFoundation = new CoreBlock("core-foundation"){{
            requirements(Category.effect, () -> false, ItemStack.with(Items.titanium, 3000, Items.silicon, 2000));

            health = 2000;
            itemCapacity = 9000;
            size = 4;
        }};

        coreNucleus = new CoreBlock("core-nucleus"){{
            requirements(Category.effect, () -> false, ItemStack.with(Items.titanium, 8000, Items.silicon, 4000, Items.surgealloy, 2000));

            health = 4000;
            itemCapacity = 13000;
            size = 5;
        }};

        vault = new Vault("vault"){{
            requirements(Category.effect, ItemStack.with(Items.titanium, 500, Items.thorium, 250));
            size = 3;
            itemCapacity = 1000;
        }};

        container = new Vault("container"){{
            requirements(Category.effect, ItemStack.with(Items.titanium, 200));
            size = 2;
            itemCapacity = 300;
        }};

        unloader = new Unloader("unloader"){{
            requirements(Category.distribution, ItemStack.with(Items.titanium, 50, Items.silicon, 60));
            speed = 7f;
        }};

        launchPad = new LaunchPad("launch-pad"){{
            requirements(Category.effect, () -> world.isZone(), ItemStack.with(Items.copper, 500, Items.titanium, 200, Items.silicon, 200, Items.lead, 200));
            size = 3;
            itemCapacity = 100;
            launchTime = 60f * 8;
            hasPower = true;
            consumes.power(1f);
        }};

        //endregion
        //region turrets

        duo = new DoubleTurret("duo"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 70), true);
            ammo(
                Items.copper, Bullets.standardCopper,
                Items.graphite, Bullets.standardDense,
                Items.pyratite, Bullets.standardIncendiary,
                Items.silicon, Bullets.standardHoming
            );
            reload = 20f;
            restitution = 0.03f;
            range = 95f;
            shootCone = 15f;
            ammoUseEffect = Fx.shellEjectSmall;
            health = 210;
            inaccuracy = 2f;
            rotatespeed = 10f;
        }};

        hail = new ArtilleryTurret("hail"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 60, Items.graphite, 35));
            ammo(
                Items.graphite, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artlleryIncendiary
            );
            reload = 60f;
            recoil = 2f;
            range = 230f;
            inaccuracy = 1f;
            shootCone = 10f;
            health = 120;
        }};

        wave = new LiquidTurret("wave"){{
            requirements(Category.turret, ItemStack.with(Items.metaglass, 90, Items.lead, 150));
            ammo(
                Liquids.water, Bullets.waterShot,
                Liquids.slag, Bullets.slagShot,
                Liquids.cryofluid, Bullets.cryoShot,
                Liquids.oil, Bullets.oilShot
            );
            size = 2;
            recoil = 0f;
            reload = 3f;
            inaccuracy = 5f;
            shootCone = 50f;
            shootEffect = Fx.shootLiquid;
            range = 100f;
            health = 360;

            drawer = (tile, entity) -> {
                Draw.rect(region, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);

                Draw.color(entity.liquids.current().color);
                Draw.alpha(entity.liquids.total() / liquidCapacity);
                Draw.rect(name + "-liquid", tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
                Draw.color();
            };
        }};

        lancer = new ChargeTurret("lancer"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 50, Items.lead, 100, Items.silicon, 90));
            range = 155f;
            chargeTime = 50f;
            chargeMaxDelay = 30f;
            chargeEffects = 7;
            shootType = Bullets.lancerLaser;
            recoil = 2f;
            reload = 90f;
            cooldown = 0.03f;
            powerUsed = 1 / 3f;
            consumes.powerBuffered(600f);
            shootShake = 2f;
            shootEffect = Fx.lancerLaserShoot;
            smokeEffect = Fx.lancerLaserShootSmoke;
            chargeEffect = Fx.lancerLaserCharge;
            chargeBeginEffect = Fx.lancerLaserChargeBegin;
            heatColor = Color.RED;
            size = 2;
            health = 320;
            targetAir = false;
        }};

        arc = new PowerTurret("arc"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 70, Items.lead, 70));
            shootType = Bullets.arc;
            reload = 20f;
            shootCone = 40f;
            rotatespeed = 8f;
            powerUsed = 1f / 2f;
            consumes.powerBuffered(80f);
            range = 80f;
            shootEffect = Fx.lightningShoot;
            heatColor = Color.RED;
            recoil = 1f;
            size = 1;
        }};

        swarmer = new BurstTurret("swarmer"){{
            requirements(Category.turret, ItemStack.with(Items.graphite, 70, Items.titanium, 70, Items.plastanium, 90, Items.silicon, 60));
            ammo(
                Items.blastCompound, Bullets.missileExplosive,
                Items.pyratite, Bullets.missileIncendiary,
                Items.surgealloy, Bullets.missileSurge
            );
            reload = 50f;
            shots = 4;
            burstSpacing = 5;
            inaccuracy = 10f;
            range = 140f;
            xRand = 6f;
            size = 2;
            health = 380;
        }};

        salvo = new BurstTurret("salvo"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 210, Items.graphite, 190, Items.titanium, 120));
            ammo(
                Items.copper, Bullets.standardCopper,
                Items.graphite, Bullets.standardDense,
                Items.pyratite, Bullets.standardIncendiary,
                Items.silicon, Bullets.standardHoming,
                Items.thorium, Bullets.standardThorium
            );

            size = 2;
            range = 120f;
            reload = 35f;
            restitution = 0.03f;
            ammoEjectBack = 3f;
            cooldown = 0.03f;
            recoil = 3f;
            shootShake = 2f;
            burstSpacing = 4;
            shots = 3;
            ammoUseEffect = Fx.shellEjectBig;
            health = 360;
        }};

        ripple = new ArtilleryTurret("ripple"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 300, Items.graphite, 270, Items.titanium, 120));
            ammo(
                Items.graphite, Bullets.artilleryDense,
                Items.silicon, Bullets.artilleryHoming,
                Items.pyratite, Bullets.artlleryIncendiary,
                Items.blastCompound, Bullets.artilleryExplosive,
                Items.plastanium, Bullets.arilleryPlastic
            );
            size = 3;
            shots = 4;
            inaccuracy = 12f;
            reload = 60f;
            ammoEjectBack = 5f;
            ammoUseEffect = Fx.shellEjectBig;
            cooldown = 0.03f;
            velocityInaccuracy = 0.2f;
            restitution = 0.02f;
            recoil = 6f;
            shootShake = 2f;
            range = 290f;

            health = 550;
        }};

        cyclone = new ItemTurret("cyclone"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 400, Items.titanium, 250, Items.plastanium, 160));
            ammo(
                Items.blastCompound, Bullets.flakExplosive,
                Items.plastanium, Bullets.flakPlastic,
                Items.surgealloy, Bullets.flakSurge
            );
            xRand = 4f;
            reload = 8f;
            range = 145f;
            size = 3;
            recoil = 3f;
            rotatespeed = 10f;
            inaccuracy = 13f;
            shootCone = 30f;

            health = 145 * size * size;
        }};

        fuse = new ItemTurret("fuse"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 450, Items.graphite, 450, Items.surgealloy, 250));
            ammo(Items.graphite, Bullets.fuseShot);
            reload = 40f;
            shootShake = 4f;
            range = 80f;
            recoil = 5f;
            restitution = 0.1f;
            size = 3;

            health = 165 * size * size;
        }};

        spectre = new DoubleTurret("spectre"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 700, Items.graphite, 600, Items.surgealloy, 500, Items.plastanium, 350, Items.thorium, 500));
            ammo(
                Items.graphite, Bullets.standardDenseBig,
                Items.pyratite, Bullets.standardIncendiaryBig,
                Items.thorium, Bullets.standardThoriumBig
            );
            reload = 6f;
            coolantMultiplier = 0.5f;
            maxCoolantUsed = 1.5f;
            restitution = 0.1f;
            ammoUseEffect = Fx.shellEjectBig;
            range = 200f;
            inaccuracy = 3f;
            recoil = 3f;
            xRand = 3f;
            shotWidth = 4f;
            shootShake = 2f;
            shots = 2;
            size = 4;
            shootCone = 24f;

            health = 155 * size * size;
        }};

        meltdown = new LaserTurret("meltdown"){{
            requirements(Category.turret, ItemStack.with(Items.copper, 500, Items.lead, 700, Items.graphite, 600, Items.surgealloy, 650, Items.silicon, 650));
            shootType = Bullets.meltdownLaser;
            shootEffect = Fx.shootBigSmoke2;
            shootCone = 40f;
            recoil = 4f;
            size = 4;
            shootShake = 2f;
            powerUsed = 0.5f;
            consumes.powerBuffered(1200f);
            range = 160f;
            reload = 200f;
            firingMoveFract = 0.1f;
            shootDuration = 220f;

            health = 165 * size * size;
        }};

        //endregion
        //region units

        spiritFactory = new UnitFactory("spirit-factory"){{
            requirements(Category.units, ItemStack.with(Items.copper, 70, Items.lead, 110, Items.silicon, 130));
            type = UnitTypes.spirit;
            produceTime = 5700;
            size = 2;
            consumes.power(0.80f);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30));
        }};

        phantomFactory = new UnitFactory("phantom-factory"){{
            requirements(Category.units, ItemStack.with(Items.titanium, 90, Items.thorium, 80, Items.lead, 110, Items.silicon, 210));
            type = UnitTypes.phantom;
            produceTime = 7300;
            size = 2;
            consumes.power(2f);
            consumes.items(new ItemStack(Items.silicon, 70), new ItemStack(Items.lead, 80), new ItemStack(Items.titanium, 80));
        }};

        wraithFactory = new UnitFactory("wraith-factory"){{
            requirements(Category.units, ItemStack.with(Items.titanium, 60, Items.lead, 80, Items.silicon, 90));
            type = UnitTypes.wraith;
            produceTime = 1800;
            size = 2;
            consumes.power(1f);
            consumes.items(new ItemStack(Items.silicon, 10), new ItemStack(Items.titanium, 10));
        }};

        ghoulFactory = new UnitFactory("ghoul-factory"){{
            requirements(Category.units, ItemStack.with(Items.plastanium, 80, Items.titanium, 100, Items.lead, 130, Items.silicon, 220));
            type = UnitTypes.ghoul;
            produceTime = 3600;
            size = 3;
            consumes.power(2f);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.titanium, 30), new ItemStack(Items.plastanium, 20));
        }};

        revenantFactory = new UnitFactory("revenant-factory"){{
            requirements(Category.units, ItemStack.with(Items.plastanium, 300, Items.titanium, 400, Items.lead, 300, Items.silicon, 400, Items.surgealloy, 100));
            type = UnitTypes.revenant;
            produceTime = 8000;
            size = 4;
            consumes.power(3f);
            consumes.items(new ItemStack(Items.silicon, 80), new ItemStack(Items.titanium, 80), new ItemStack(Items.plastanium, 50));
        }};

        daggerFactory = new UnitFactory("dagger-factory"){{
            requirements(Category.units, ItemStack.with(Items.lead, 90, Items.silicon, 70));
            type = UnitTypes.dagger;
            produceTime = 1700;
            size = 2;
            consumes.power(0.50f);
            consumes.items(new ItemStack(Items.silicon, 10));
        }};

        crawlerFactory = new UnitFactory("crawler-factory"){{
            requirements(Category.units, ItemStack.with(Items.lead, 100, Items.silicon, 80));
            type = UnitTypes.crawler;
            produceTime = 1200;
            size = 2;
            consumes.power(0.4f);
            consumes.items(new ItemStack(Items.blastCompound, 10));
        }};

        titanFactory = new UnitFactory("titan-factory"){{
            requirements(Category.units, ItemStack.with(Items.thorium, 90, Items.lead, 140, Items.silicon, 90));
            type = UnitTypes.titan;
            produceTime = 3400;
            size = 3;
            consumes.power(1.50f);
            consumes.items(new ItemStack(Items.silicon, 20), new ItemStack(Items.thorium, 30));
        }};

        fortressFactory = new UnitFactory("fortress-factory"){{
            requirements(Category.units, ItemStack.with(Items.thorium, 200, Items.lead, 220, Items.silicon, 150, Items.surgealloy, 100, Items.phasefabric, 50));
            type = UnitTypes.fortress;
            produceTime = 5000;
            size = 3;
            consumes.power(2f);
            consumes.items(new ItemStack(Items.silicon, 40), new ItemStack(Items.thorium, 50));
        }};

        repairPoint = new RepairPoint("repair-point"){{
            requirements(Category.units, ItemStack.with(Items.lead, 30, Items.copper, 30, Items.silicon, 30));
            repairSpeed = 0.1f;
        }};

        //endregion
        //region upgrades

        alphaDartPad = new MechPad("alpha-dart-mech-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 200, Items.graphite, 100, Items.copper, 150));
            mech = Mechs.alpha;
            size = 2;
            consumes.powerBuffered(50f);
        }};

        deltaPad = new MechPad("delta-mech-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 350, Items.titanium, 350, Items.copper, 400, Items.silicon, 450, Items.thorium, 300));
            mech = Mechs.delta;
            size = 2;
            consumes.powerBuffered(70f);
        }};

        tauPad = new MechPad("tau-mech-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 250, Items.titanium, 250, Items.copper, 250, Items.silicon, 250));
            mech = Mechs.tau;
            size = 2;
            consumes.powerBuffered(100f);
        }};

        omegaPad = new MechPad("omega-mech-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 450, Items.graphite, 550, Items.silicon, 650, Items.thorium, 600, Items.surgealloy, 240));
            mech = Mechs.omega;
            size = 3;
            consumes.powerBuffered(120f);
        }};

        javelinPad = new MechPad("javelin-ship-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 350, Items.silicon, 450, Items.titanium, 500, Items.plastanium, 400, Items.phasefabric, 200));
            mech = Mechs.javelin;
            size = 2;
            consumes.powerBuffered(80f);
        }};

        tridentPad = new MechPad("trident-ship-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 250, Items.copper, 250, Items.silicon, 250, Items.titanium, 300, Items.plastanium, 200));
            mech = Mechs.trident;
            size = 2;
            consumes.powerBuffered(100f);
        }};

        glaivePad = new MechPad("glaive-ship-pad"){{
            requirements(Category.upgrade, ItemStack.with(Items.lead, 450, Items.silicon, 650, Items.titanium, 700, Items.plastanium, 600, Items.surgealloy, 200));
            mech = Mechs.glaive;
            size = 3;
            consumes.powerBuffered(120f);
        }};

        //endregion
        //region ores

        //create ores for every floor and item combination necessary
        for(Item item : content.items()){
            if(!item.genOre) continue;

            for(Block block : content.blocks()){
                if(block instanceof Floor && ((Floor) block).hasOres){
                    new OreBlock(item, (Floor) block);
                }
            }
        }

        //special variants
        new OreBlock(Items.scrap, (Floor)snow);

        //endregion
    }
}
