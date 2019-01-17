package io.anuke.mindustry.content;

import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.graphics.CacheLayer;
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
import io.anuke.mindustry.world.blocks.storage.SortedUnloader;
import io.anuke.mindustry.world.blocks.storage.Vault;
import io.anuke.mindustry.world.blocks.units.*;

import static io.anuke.mindustry.Vars.content;

public class Blocks implements ContentList{
    public static Block

    //environment
    air, blockpart, spawn, space, metalfloor, deepwater, water, tar, stone, blackstone, dirt, sand, ice, snow,
    grass, shrub, rock, icerock, blackrock, rocks,

    //crafting
    siliconSmelter, plastaniumCompressor, phaseWeaver, surgeSmelter, pyratiteMixer, blastMixer, cryofluidMixer,
    melter, separator, centrifuge, biomatterCompressor, pulverizer, incinerator,

    //sandbox
    powerVoid, powerSource, itemSource, liquidSource, itemVoid,

    //defense
    copperWall, copperWallLarge, titaniumWall, titaniumWallLarge, thoriumWall, thoriumWallLarge, door, doorLarge,
    phaseWall, phaseWallLarge, surgeWall, surgeWallLarge, mendProjector, overdriveProjector, forceProjector, shockMine,

    //transport
    conveyor, titaniumConveyor, distributor, junction, itemBridge, phaseConveyor, sorter, router, overflowGate, massDriver,

    //liquids
    mechanicalPump, rotaryPump, thermalPump, conduit, pulseConduit, liquidRouter, liquidTank, liquidJunction, bridgeConduit, phaseConduit,

    //power
    combustionGenerator, thermalGenerator, turbineGenerator, rtgGenerator, solarPanel, largeSolarPanel, thoriumReactor,
    fusionReactor, battery, batteryLarge, powerNode, powerNodeLarge,

    //production
    mechanicalDrill, pneumaticDrill, laserDrill, blastDrill, plasmaDrill, waterExtractor, oilExtractor, cultivator,

    //storage
    core, vault, container, unloader, launchPad,

    //turrets
    duo, scorch, hail, wave, lancer, arc, swarmer, salvo, fuse, ripple, cyclone, spectre, meltdown,

    //units
    spiritFactory, phantomFactory, wraithFactory, ghoulFactory, revenantFactory, daggerFactory, titanFactory,
    fortressFactory, reconstructor, repairPoint,

    //upgrades
    alphaPad, deltaPad, tauPad, omegaPad, dartPad, javelinPad, tridentPad, glaivePad;

    @Override
    public void load(){
        //region environment

        air = new Floor("air"){
            {
                blend = false;
                alwaysReplace = true;
            }

            public void draw(Tile tile){}
            public void load(){}
            public void init(){}
        };

        blockpart = new BlockPart();

        spawn = new Block("spawn"){

            public void drawShadow(Tile tile){}

            public void draw(Tile tile){
                Draw.color(Color.SCARLET);
                Lines.circle(tile.worldx(), tile.worldy(), 4f +Mathf.absin(Time.time(), 6f, 6f));
                Draw.color();
            }
        };

        //Registers build blocks from size 1-6
        //no reference is needed here since they can be looked up by name later
        for(int i = 1; i <= 6; i++){
            new BuildBlock("build" + i);
        }

        space = new Floor("space"){{
            placeableOn = false;
            variants = 0;
            cacheLayer = CacheLayer.space;
            solid = true;
            blend = false;
            minimapColor = Color.valueOf("000001");
        }};

        metalfloor = new Floor("metalfloor"){{
            variants = 6;
        }};

        deepwater = new Floor("deepwater"){{
            liquidColor = Color.valueOf("546bb3");
            speedMultiplier = 0.2f;
            variants = 0;
            liquidDrop = Liquids.water;
            isLiquid = true;
            status = StatusEffects.wet;
            statusDuration = 120f;
            drownTime = 140f;
            cacheLayer = CacheLayer.water;
            minimapColor = Color.valueOf("465a96");
        }};

        water = new Floor("water"){{
            liquidColor = Color.valueOf("546bb3");
            speedMultiplier = 0.5f;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            minimapColor = Color.valueOf("506eb4");
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
            cacheLayer = CacheLayer.oil;
            minimapColor = Color.valueOf("292929");
        }};

        stone = new Floor("stone"){{
            hasOres = true;
            blends = block -> block != this && !(block instanceof OreBlock);
            minimapColor = Color.valueOf("323232");
            playerUnmineable = true;
        }};

        blackstone = new Floor("blackstone"){{
            minimapColor = Color.valueOf("252525");
            playerUnmineable = true;
            hasOres = true;
        }};

        dirt = new Floor("dirt"){{
            minimapColor = Color.valueOf("6e501e");
        }};

        sand = new Floor("sand"){{
            drops = new ItemStack(Items.sand, 1);
            minimapColor = Color.valueOf("988a67");
            hasOres = true;
            playerUnmineable = true;
        }};

        ice = new Floor("ice"){{
            dragMultiplier = 0.2f;
            speedMultiplier = 0.4f;
            minimapColor = Color.valueOf("b8eef8");
            hasOres = true;
        }};

        snow = new Floor("snow"){{
            minimapColor = Color.valueOf("c2d1d2");
            hasOres = true;
        }};

        grass = new Floor("grass"){{
            hasOres = true;
            minimapColor = Color.valueOf("549d5b");
        }};

        shrub = new Rock("shrub");

        rock = new Rock("rock"){{
            variants = 2;
        }};

        icerock = new Rock("icerock"){{
            variants = 2;
        }};

        blackrock = new Rock("blackrock"){{
            variants = 1;
        }};

        rocks = new Rock("rocks"){{
           variants = 2;
           breakable = alwaysReplace = false;
           solid = true;
        }};
        
        //endregion
        //region crafting

        siliconSmelter = new PowerSmelter("silicon-smelter"){{
            health = 90;
            craftEffect = Fx.smeltsmoke;
            result = Items.silicon;
            craftTime = 40f;
            size = 2;
            hasLiquids = false;
            flameColor = Color.valueOf("ffef99");

            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.sand, 2));
            consumes.power(0.05f);
        }};

        plastaniumCompressor = new PlastaniumCompressor("plastanium-compressor"){{
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
            consumes.power(0.3f);
            consumes.item(Items.titanium, 2);
        }};

        phaseWeaver = new PhaseWeaver("phase-weaver"){{
            craftEffect = Fx.smeltsmoke;
            result = Items.phasefabric;
            craftTime = 120f;
            size = 2;

            consumes.items(new ItemStack(Items.thorium, 4), new ItemStack(Items.sand, 10));
            consumes.power(0.5f);
        }};

        surgeSmelter = new PowerSmelter("alloy-smelter"){{
            craftEffect = Fx.smeltsmoke;
            result = Items.surgealloy;
            craftTime = 75f;
            size = 2;

            useFlux = true;
            fluxNeeded = 3;

            consumes.power(0.4f);
            consumes.items(new ItemStack(Items.titanium, 2), new ItemStack(Items.lead, 4), new ItemStack(Items.silicon, 3), new ItemStack(Items.copper, 3));
        }};

        cryofluidMixer = new LiquidMixer("cryofluidmixer"){{
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
            consumes.items(new ItemStack(Items.coal, 1), new ItemStack(Items.lead, 2), new ItemStack(Items.sand, 2));
        }};

        melter = new PowerCrafter("melter"){{
            health = 200;
            outputLiquid = Liquids.slag;
            outputLiquidAmount = 1.5f;
            craftTime = 10f;
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

            consumes.liquid(Liquids.slag, 0.3f);
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
            craftEffect = Fx.pulverize;
            craftTime = 40f;
            updateEffect = Fx.pulverizeSmall;
            hasItems = hasPower = true;

            consumes.item(Items.scrap, 1);
            consumes.power(0.05f);
        }};

        incinerator = new Incinerator("incinerator"){{
            health = 90;
        }};
        
        //endregion
        //region sandbox
        
        powerVoid = new PowerVoid("power-void");
        powerSource = new PowerSource("power-source");
        itemSource = new ItemSource("item-source");
        itemVoid = new ItemVoid("item-void");
        liquidSource = new LiquidSource("liquid-source");
        
        //endregion
        //region defense

        int wallHealthMultiplier = 3;

        copperWall = new Wall("copper-wall"){{
            health = 80 * wallHealthMultiplier;
        }};

        copperWallLarge = new Wall("copper-wall-large"){{
            health = 80 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        titaniumWall = new Wall("titanium-wall"){{
            health = 110 * wallHealthMultiplier;
        }};

        titaniumWallLarge = new Wall("titanium-wall-large"){{
            health = 110 * wallHealthMultiplier * 4;
            size = 2;
        }};

        thoriumWall = new Wall("thorium-wall"){{
            health = 200 * wallHealthMultiplier;
        }};

        thoriumWallLarge = new Wall("thorium-wall-large"){{
            health = 200 * wallHealthMultiplier * 4;
            size = 2;
        }};

        phaseWall = new DeflectorWall("phase-wall"){{
            health = 150 * wallHealthMultiplier;
        }};

        phaseWallLarge = new DeflectorWall("phase-wall-large"){{
            health = 150 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        surgeWall = new SurgeWall("surge-wall"){{
            health = 230 * wallHealthMultiplier;
        }};

        surgeWallLarge = new SurgeWall("surge-wall-large"){{
            health = 230 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        door = new Door("door"){{
            health = 100 * wallHealthMultiplier;
        }};

        doorLarge = new Door("door-large"){{
            openfx = Fx.dooropenlarge;
            closefx = Fx.doorcloselarge;
            health = 100 * 4 * wallHealthMultiplier;
            size = 2;
        }};

        mendProjector = new MendProjector("mend-projector"){{
            consumes.power(0.2f, 1.0f);
            size = 2;
            consumes.item(Items.phasefabric).optional(true);
        }};

        overdriveProjector = new OverdriveProjector("overdrive-projector"){{
            consumes.power(0.35f, 1.0f);
            size = 2;
            consumes.item(Items.phasefabric).optional(true);
        }};

        forceProjector = new ForceProjector("force-projector"){{
            size = 3;
            consumes.item(Items.phasefabric).optional(true);
        }};

        shockMine = new ShockMine("shock-mine"){{
            health = 40;
            damage = 11;
            tileDamage = 7f;
            length = 10;
            tendrils = 5;
        }};
        
        //endregion
        //region distribution

        conveyor = new Conveyor("conveyor"){{
            health = 45;
            speed = 0.03f;
        }};

        titaniumConveyor = new Conveyor("titanium-conveyor"){{
            health = 65;
            speed = 0.07f;
        }};

        junction = new Junction("junction"){{
            speed = 26;
            capacity = 32;
        }};

        itemBridge = new BufferedItemBridge("bridge-conveyor"){{
            range = 4;
            speed = 60f;
            bufferCapacity = 15;
        }};

        phaseConveyor = new ItemBridge("phase-conveyor"){{
            range = 12;
            hasPower = true;
            consumes.power(0.03f, 1.0f);
        }};

        sorter = new Sorter("sorter");

        router = new Router("router");

        distributor = new Router("distributor"){{
            size = 2;
        }};

        overflowGate = new OverflowGate("overflow-gate");

        massDriver = new MassDriver("mass-driver"){{
            size = 3;
            itemCapacity = 60;
            range = 440f;
        }};
        
        //endregion
        //region liquid

        mechanicalPump = new Pump("mechanical-pump"){{
            pumpAmount = 0.1f;
            tier = 0;
        }};

        rotaryPump = new Pump("rotary-pump"){{
            pumpAmount = 0.2f;
            consumes.power(0.015f);
            liquidCapacity = 30f;
            hasPower = true;
            size = 2;
            tier = 1;
        }};

        thermalPump = new Pump("thermal-pump"){{
            pumpAmount = 0.275f;
            consumes.power(0.03f);
            liquidCapacity = 40f;
            hasPower = true;
            size = 2;
            tier = 2;
        }};

        conduit = new Conduit("conduit"){{
            health = 45;
        }};

        pulseConduit = new Conduit("pulse-conduit"){{
            liquidCapacity = 16f;
            liquidFlowFactor = 4.9f;
            health = 90;
        }};

        liquidRouter = new LiquidRouter("liquid-router"){{
            liquidCapacity = 20f;
        }};

        liquidTank = new LiquidTank("liquid-tank"){{
            size = 3;
            liquidCapacity = 1500f;
            health = 500;
        }};

        liquidJunction = new LiquidJunction("liquid-junction");

        bridgeConduit = new LiquidExtendingBridge("bridge-conduit"){{
            range = 4;
            hasPower = false;
        }};

        phaseConduit = new LiquidBridge("phase-conduit"){{
            range = 12;
            hasPower = true;
            consumes.power(0.03f, 1.0f);
        }};
        
        //endregion
        //region power

        combustionGenerator = new BurnerGenerator("combustion-generator"){{
            powerProduction = 0.09f;
            itemDuration = 40f;
        }};

        thermalGenerator = new LiquidHeatGenerator("thermal-generator"){{
            maxLiquidGenerate = 2f;
            powerProduction = 2f;
            generateEffect = Fx.redgeneratespark;
            size = 2;
        }};

        turbineGenerator = new TurbineGenerator("turbine-generator"){{
            powerProduction = 0.28f;
            itemDuration = 30f;
            consumes.liquid(Liquids.water, 0.05f);
            size = 2;
        }};

        rtgGenerator = new DecayGenerator("rtg-generator"){{
            size = 2;
            powerProduction = 0.3f;
            itemDuration = 220f;
        }};

        solarPanel = new SolarGenerator("solar-panel"){{
            powerProduction = 0.0045f;
        }};

        largeSolarPanel = new SolarGenerator("solar-panel-large"){{
            size = 3;
            powerProduction = 0.055f;
        }};

        thoriumReactor = new NuclearReactor("thorium-reactor"){{
            size = 3;
            health = 700;
            powerProduction = 1.1f;
        }};

        fusionReactor = new FusionReactor("fusion-reactor"){{
            size = 4;
            health = 600;
        }};

        battery = new Battery("battery"){{
            consumes.powerBuffered(320f, 1f);
        }};

        batteryLarge = new Battery("battery-large"){{
            size = 3;
            consumes.powerBuffered(2000f, 1f);
        }};

        powerNode = new PowerNode("power-node"){{
            maxNodes = 4;
            laserRange = 6;
        }};

        powerNodeLarge = new PowerNode("power-node-large"){{
            size = 2;
            maxNodes = 6;
            laserRange = 9.5f;
        }};
        
        //endregion power
        //region production

        mechanicalDrill = new Drill("mechanical-drill"){{
            tier = 2;
            drillTime = 300;
            size = 2;
            drawMineItem = true;
        }};

        pneumaticDrill = new Drill("pneumatic-drill"){{
            tier = 3;
            drillTime = 240;
            size = 2;
            drawMineItem = true;
        }};

        laserDrill = new Drill("laser-drill"){{
            drillTime = 140;
            size = 2;
            hasPower = true;
            tier = 4;
            updateEffect = Fx.pulverizeMedium;
            drillEffect = Fx.mineBig;

            consumes.power(0.11f);
        }};

        blastDrill = new Drill("blast-drill"){{
            drillTime = 60;
            size = 3;
            drawRim = true;
            hasPower = true;
            tier = 5;
            updateEffect = Fx.pulverizeRed;
            updateEffectChance = 0.03f;
            drillEffect = Fx.mineHuge;
            rotateSpeed = 6f;
            warmupSpeed = 0.01f;

            consumes.power(0.3f);
        }};

        plasmaDrill = new Drill("plasma-drill"){{
            heatColor = Color.valueOf("ff461b");
            drillTime = 50;
            size = 4;
            hasLiquids = true;
            hasPower = true;
            tier = 5;
            rotateSpeed = 9f;
            drawRim = true;
            updateEffect = Fx.pulverizeRedder;
            updateEffectChance = 0.04f;
            drillEffect = Fx.mineHuge;
            warmupSpeed = 0.005f;

            consumes.power(0.7f);
        }};

        waterExtractor = new SolidPump("water-extractor"){{
            result = Liquids.water;
            pumpAmount = 0.065f;
            size = 2;
            liquidCapacity = 30f;
            rotateSpeed = 1.4f;

            consumes.power(0.09f);
        }};

        oilExtractor = new Fracker("oil-extractor"){{
            result = Liquids.oil;
            updateEffect = Fx.pulverize;
            liquidCapacity = 50f;
            updateEffectChance = 0.05f;
            pumpAmount = 0.09f;
            size = 3;
            liquidCapacity = 30f;

            consumes.item(Items.sand);
            consumes.power(0.3f);
            consumes.liquid(Liquids.water, 0.15f);
        }};

        cultivator = new Cultivator("cultivator"){{
            result = Items.biomatter;
            drillTime = 200;
            size = 2;
            hasLiquids = true;
            hasPower = true;

            consumes.power(0.08f);
            consumes.liquid(Liquids.water, 0.15f);
        }};
        
        //endregion
        //region storage

        core = new CoreBlock("core"){{
            health = 1100;
            itemCapacity = 2000;
        }};

        vault = new Vault("vault"){{
            size = 3;
            itemCapacity = 1000;
        }};

        container = new Vault("container"){{
            size = 2;
            itemCapacity = 300;
        }};

        unloader = new SortedUnloader("unloader"){{
            speed = 7f;
        }};

        launchPad = new LaunchPad("launch-pad"){{
            size = 3;
            itemCapacity = 100;
            launchTime = 60f * 6;
            consumes.power(0.1f);
        }};
        
        //endregion
        //region turrets

        duo = new DoubleTurret("duo"){{
            ammo(
                Items.copper, Bullets.standardCopper,
                Items.graphite, Bullets.standardDense,
                Items.pyratite, Bullets.standardIncendiary,
                Items.silicon, Bullets.standardHoming
            );
            reload = 25f;
            restitution = 0.03f;
            range = 90f;
            shootCone = 15f;
            ammoUseEffect = Fx.shellEjectSmall;
            health = 80;
            inaccuracy = 2f;
            rotatespeed = 10f;
        }};

        hail = new ArtilleryTurret("hail"){{
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

        scorch = new LiquidTurret("scorch"){{
            ammo(Liquids.oil, Bullets.basicFlame);
            recoil = 0f;
            reload = 4f;
            shootCone = 50f;
            ammoUseEffect = Fx.shellEjectSmall;
            health = 160;
        }};

        wave = new LiquidTurret("wave"){{
            ammo(
                Liquids.water, Bullets.waterShot,
                Liquids.slag, Bullets.slagShot,
                Liquids.cryofluid, Bullets.cryoShot,
                Liquids.oil, Bullets.oilShot
            );
            size = 2;
            recoil = 0f;
            reload = 4f;
            inaccuracy = 5f;
            shootCone = 50f;
            shootEffect = Fx.shootLiquid;
            range = 90f;
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
            range = 90f;
            chargeTime = 60f;
            chargeMaxDelay = 30f;
            chargeEffects = 7;
            shootType = Bullets.lancerLaser;
            recoil = 2f;
            reload = 100f;
            cooldown = 0.03f;
            powerUsed = 1 / 3f;
            consumes.powerBuffered(60f);
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
            shootType = Bullets.arc;
            reload = 85f;
            shootShake = 1f;
            shootCone = 40f;
            rotatespeed = 8f;
            powerUsed = 1f / 3f;
            consumes.powerBuffered(30f);
            range = 150f;
            shootEffect = Fx.lightningShoot;
            heatColor = Color.RED;
            recoil = 1f;
            size = 1;
        }};

        swarmer = new BurstTurret("swarmer"){{
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
            range = 320f;

            health = 550;
        }};

        cyclone = new ItemTurret("cyclone"){{
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
            ammo(Items.graphite, Bullets.fuseShot);
            reload = 50f;
            shootShake = 4f;
            range = 80f;
            recoil = 5f;
            restitution = 0.1f;
            size = 3;

            health = 155 * size * size;
        }};

        spectre = new DoubleTurret("spectre"){{
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
            shootType = Bullets.meltdownLaser;
            shootEffect = Fx.shootBigSmoke2;
            shootCone = 40f;
            recoil = 4f;
            size = 4;
            shootShake = 2f;
            powerUsed = 0.5f;
            consumes.powerBuffered(120f);
            range = 160f;
            reload = 200f;
            firingMoveFract = 0.1f;
            shootDuration = 220f;

            health = 165 * size * size;
        }};
        
        //endregion
        //region units

        spiritFactory = new UnitFactory("spirit-factory"){{
            type = UnitTypes.spirit;
            produceTime = 5700;
            size = 2;
            consumes.power(0.08f);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.lead, 30));
        }};

        phantomFactory = new UnitFactory("phantom-factory"){{
            type = UnitTypes.phantom;
            produceTime = 7300;
            size = 2;
            consumes.power(0.2f);
            consumes.items(new ItemStack(Items.silicon, 70), new ItemStack(Items.lead, 80), new ItemStack(Items.titanium, 80));
        }};

        wraithFactory = new UnitFactory("wraith-factory"){{
            type = UnitTypes.wraith;
            produceTime = 1800;
            size = 2;
            consumes.power(0.1f);
            consumes.items(new ItemStack(Items.silicon, 10), new ItemStack(Items.titanium, 10));
        }};

        ghoulFactory = new UnitFactory("ghoul-factory"){{
            type = UnitTypes.ghoul;
            produceTime = 3600;
            size = 3;
            consumes.power(0.2f);
            consumes.items(new ItemStack(Items.silicon, 30), new ItemStack(Items.titanium, 30), new ItemStack(Items.plastanium, 20));
        }};

        revenantFactory = new UnitFactory("revenant-factory"){{
            type = UnitTypes.revenant;
            produceTime = 8000;
            size = 4;
            consumes.power(0.3f);
            consumes.items(new ItemStack(Items.silicon, 80), new ItemStack(Items.titanium, 80), new ItemStack(Items.plastanium, 50));
        }};

        daggerFactory = new UnitFactory("dagger-factory"){{
            type = UnitTypes.dagger;
            produceTime = 1700;
            size = 2;
            consumes.power(0.05f);
            consumes.items(new ItemStack(Items.silicon, 10));
        }};

        titanFactory = new UnitFactory("titan-factory"){{
            type = UnitTypes.titan;
            produceTime = 3400;
            size = 3;
            consumes.power(0.15f);
            consumes.items(new ItemStack(Items.silicon, 20), new ItemStack(Items.thorium, 30));
        }};

        fortressFactory = new UnitFactory("fortress-factory"){{
            type = UnitTypes.fortress;
            produceTime = 5000;
            size = 3;
            consumes.power(0.2f);
            consumes.items(new ItemStack(Items.silicon, 40), new ItemStack(Items.thorium, 50));
        }};

        repairPoint = new RepairPoint("repair-point"){{
            repairSpeed = 0.1f;
        }};

        reconstructor = new Reconstructor("reconstructor"){{
            size = 2;
        }};
        
        //endregion
        //region upgrades

        alphaPad = new MechPad("alpha-mech-pad"){{
            mech = Mechs.alpha;
            size = 2;
            consumes.powerBuffered(50f);
        }};

        deltaPad = new MechPad("delta-mech-pad"){{
            mech = Mechs.delta;
            size = 2;
            consumes.powerBuffered(70f);
        }};

        tauPad = new MechPad("tau-mech-pad"){{
            mech = Mechs.tau;
            size = 2;
            consumes.powerBuffered(100f);
        }};

        omegaPad = new MechPad("omega-mech-pad"){{
            mech = Mechs.omega;
            size = 3;
            consumes.powerBuffered(120f);
        }};

        dartPad = new MechPad("dart-ship-pad"){{
            mech = Mechs.dart;
            size = 2;
            consumes.powerBuffered(50f);
        }};

        javelinPad = new MechPad("javelin-ship-pad"){{
            mech = Mechs.javelin;
            size = 2;
            consumes.powerBuffered(80f);
        }};

        tridentPad = new MechPad("trident-ship-pad"){{
            mech = Mechs.trident;
            size = 2;
            consumes.powerBuffered(100f);
        }};

        glaivePad = new MechPad("glaive-ship-pad"){{
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

        //endregion
    }
}
