package io.anuke.mindustry.content;

import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;

import static io.anuke.mindustry.type.Category.*;

public class Recipes implements ContentList{

    @Override
    public void load(){
        //DEFENSE

        //walls
        new Recipe(defense, DefenseBlocks.copperWall, new ItemStack(Items.copper, 12));
        new Recipe(defense, DefenseBlocks.copperWallLarge, new ItemStack(Items.copper, 12 * 4));

        new Recipe(defense, DefenseBlocks.denseAlloyWall, new ItemStack(Items.densealloy, 12));
        new Recipe(defense, DefenseBlocks.denseAlloyWallLarge, new ItemStack(Items.densealloy, 12 * 4));

        new Recipe(defense, DefenseBlocks.door, new ItemStack(Items.densealloy, 12), new ItemStack(Items.silicon, 8));
        new Recipe(defense, DefenseBlocks.doorLarge, new ItemStack(Items.densealloy, 12 * 4), new ItemStack(Items.silicon, 8 * 4));

        new Recipe(defense, DefenseBlocks.thoriumWall, new ItemStack(Items.thorium, 12));
        new Recipe(defense, DefenseBlocks.thoriumWallLarge, new ItemStack(Items.thorium, 12 * 4));

        new Recipe(defense, DefenseBlocks.phaseWall, new ItemStack(Items.phasematter, 12));
        new Recipe(defense, DefenseBlocks.phaseWallLarge, new ItemStack(Items.phasematter, 12 * 4));

        new Recipe(defense, DefenseBlocks.surgeWall, new ItemStack(Items.surgealloy, 12));
        new Recipe(defense, DefenseBlocks.surgeWallLarge, new ItemStack(Items.surgealloy, 12 * 4));

        //projectors
        new Recipe(defense, DefenseBlocks.mendProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 150), new ItemStack(Items.titanium, 50), new ItemStack(Items.silicon, 250));
        new Recipe(defense, DefenseBlocks.overdriveProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 150), new ItemStack(Items.titanium, 150), new ItemStack(Items.silicon, 250));
        new Recipe(defense, DefenseBlocks.forceProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 150), new ItemStack(Items.titanium, 150), new ItemStack(Items.silicon, 250));

        //extra blocks
        new Recipe(defense, DefenseBlocks.shockMine, new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 25))
            .setDependencies(Items.blastCompound);

        //TURRETS
        new Recipe(weapon, TurretBlocks.duo, new ItemStack(Items.copper, 40));
        new Recipe(weapon, TurretBlocks.arc, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 30), new ItemStack(Items.silicon, 20));
        new Recipe(weapon, TurretBlocks.hail, new ItemStack(Items.copper, 60), new ItemStack(Items.densealloy, 35));
        new Recipe(weapon, TurretBlocks.lancer, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 90));
        new Recipe(weapon, TurretBlocks.wave, new ItemStack(Items.densealloy, 60), new ItemStack(Items.titanium, 70), new ItemStack(Items.lead, 150));
        new Recipe(weapon, TurretBlocks.salvo, new ItemStack(Items.copper, 210), new ItemStack(Items.densealloy, 190), new ItemStack(Items.thorium, 130));
        new Recipe(weapon, TurretBlocks.swarmer, new ItemStack(Items.densealloy, 70), new ItemStack(Items.titanium, 70), new ItemStack(Items.plastanium, 90), new ItemStack(Items.silicon, 60));
        new Recipe(weapon, TurretBlocks.ripple, new ItemStack(Items.copper, 300), new ItemStack(Items.densealloy, 220), new ItemStack(Items.thorium, 120));
        new Recipe(weapon, TurretBlocks.cyclone, new ItemStack(Items.copper, 400), new ItemStack(Items.densealloy, 400), new ItemStack(Items.surgealloy, 200), new ItemStack(Items.plastanium, 150));
        new Recipe(weapon, TurretBlocks.fuse, new ItemStack(Items.copper, 450), new ItemStack(Items.densealloy, 450), new ItemStack(Items.surgealloy, 250));
        new Recipe(weapon, TurretBlocks.spectre, new ItemStack(Items.copper, 450), new ItemStack(Items.densealloy, 450), new ItemStack(Items.surgealloy, 250));
        new Recipe(weapon, TurretBlocks.meltdown, new ItemStack(Items.copper, 450), new ItemStack(Items.densealloy, 450), new ItemStack(Items.surgealloy, 250));

        //DISTRIBUTION
        new Recipe(distribution, DistributionBlocks.conveyor, new ItemStack(Items.copper, 1));
        new Recipe(distribution, DistributionBlocks.titaniumconveyor, new ItemStack(Items.copper, 2), new ItemStack(Items.titanium, 1));
        new Recipe(distribution, DistributionBlocks.phaseConveyor, new ItemStack(Items.phasematter, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.densealloy, 20));

        //starter lead transportation
        new Recipe(distribution, DistributionBlocks.junction, new ItemStack(Items.copper, 2));
        new Recipe(distribution, DistributionBlocks.router, new ItemStack(Items.copper, 6));

        //advanced densealloy transporation
        new Recipe(distribution, DistributionBlocks.distributor, new ItemStack(Items.densealloy, 8), new ItemStack(Items.copper, 8));
        new Recipe(distribution, DistributionBlocks.sorter, new ItemStack(Items.densealloy, 4), new ItemStack(Items.copper, 4));
        new Recipe(distribution, DistributionBlocks.overflowGate, new ItemStack(Items.densealloy, 4), new ItemStack(Items.copper, 8));
        new Recipe(distribution, DistributionBlocks.bridgeConveyor, new ItemStack(Items.densealloy, 8), new ItemStack(Items.copper, 8));
        new Recipe(distribution, DistributionBlocks.massDriver, new ItemStack(Items.densealloy, 400), new ItemStack(Items.silicon, 300), new ItemStack(Items.lead, 400), new ItemStack(Items.thorium, 250));

        //CRAFTING

        //smelting
        new Recipe(crafting, CraftingBlocks.smelter, new ItemStack(Items.copper, 100));
        new Recipe(crafting, CraftingBlocks.arcsmelter, new ItemStack(Items.copper, 110), new ItemStack(Items.densealloy, 70), new ItemStack(Items.lead, 50));
        new Recipe(crafting, CraftingBlocks.siliconsmelter, new ItemStack(Items.copper, 60), new ItemStack(Items.lead, 50));

        //advanced fabrication
        new Recipe(crafting, CraftingBlocks.plastaniumCompressor, new ItemStack(Items.silicon, 160), new ItemStack(Items.lead, 230), new ItemStack(Items.densealloy, 120), new ItemStack(Items.titanium, 160));
        new Recipe(crafting, CraftingBlocks.phaseWeaver, new ItemStack(Items.silicon, 260), new ItemStack(Items.lead, 240), new ItemStack(Items.thorium, 150));

        new Recipe(crafting, CraftingBlocks.alloySmelter, new ItemStack(Items.silicon, 160), new ItemStack(Items.lead, 160), new ItemStack(Items.thorium, 140));

        //misc
        new Recipe(crafting, CraftingBlocks.pulverizer, new ItemStack(Items.copper, 60), new ItemStack(Items.lead, 50));
        new Recipe(crafting, CraftingBlocks.pyratiteMixer, new ItemStack(Items.copper, 100), new ItemStack(Items.lead, 50));
        new Recipe(crafting, CraftingBlocks.blastMixer, new ItemStack(Items.lead, 60), new ItemStack(Items.densealloy, 40));
        new Recipe(crafting, CraftingBlocks.cryofluidmixer, new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 80), new ItemStack(Items.thorium, 90));

        new Recipe(crafting, CraftingBlocks.solidifier, new ItemStack(Items.densealloy, 30), new ItemStack(Items.copper, 20));
        new Recipe(crafting, CraftingBlocks.melter, new ItemStack(Items.copper, 60), new ItemStack(Items.lead, 70), new ItemStack(Items.densealloy, 90));
        new Recipe(crafting, CraftingBlocks.incinerator, new ItemStack(Items.densealloy, 10), new ItemStack(Items.lead, 30));

        //processing
        new Recipe(crafting, CraftingBlocks.biomatterCompressor, new ItemStack(Items.lead, 70), new ItemStack(Items.silicon, 60));
        new Recipe(crafting, CraftingBlocks.separator, new ItemStack(Items.copper, 60), new ItemStack(Items.densealloy, 50));
        new Recipe(crafting, CraftingBlocks.centrifuge, new ItemStack(Items.copper, 130), new ItemStack(Items.densealloy, 130), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 50));

        //POWER
        new Recipe(power, PowerBlocks.powerNode, new ItemStack(Items.copper, 2), new ItemStack(Items.lead, 6))
                .setDependencies(PowerBlocks.combustionGenerator);
        new Recipe(power, PowerBlocks.powerNodeLarge, new ItemStack(Items.densealloy, 10), new ItemStack(Items.lead, 20), new ItemStack(Items.silicon, 6))
                .setDependencies(PowerBlocks.powerNode);
        new Recipe(power, PowerBlocks.battery, new ItemStack(Items.copper, 8), new ItemStack(Items.lead, 30), new ItemStack(Items.silicon, 4))
                .setDependencies(PowerBlocks.powerNode);
        new Recipe(power, PowerBlocks.batteryLarge, new ItemStack(Items.densealloy, 40), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 30))
                .setDependencies(PowerBlocks.powerNode);

        //generators - combustion
        new Recipe(power, PowerBlocks.combustionGenerator, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 30));
        new Recipe(power, PowerBlocks.turbineGenerator, new ItemStack(Items.copper, 70), new ItemStack(Items.densealloy, 50), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 60));
        new Recipe(power, PowerBlocks.thermalGenerator, new ItemStack(Items.copper, 80), new ItemStack(Items.densealloy, 70), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 70), new ItemStack(Items.thorium, 70));

        //generators - solar
        new Recipe(power, PowerBlocks.solarPanel, new ItemStack(Items.lead, 20), new ItemStack(Items.silicon, 30));
        new Recipe(power, PowerBlocks.largeSolarPanel, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 290), new ItemStack(Items.phasematter, 30));

        //generators - nuclear
        new Recipe(power, PowerBlocks.thoriumReactor, new ItemStack(Items.lead, 600), new ItemStack(Items.silicon, 400), new ItemStack(Items.densealloy, 300), new ItemStack(Items.thorium, 300));
        new Recipe(power, PowerBlocks.rtgGenerator, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 150), new ItemStack(Items.phasematter, 50), new ItemStack(Items.plastanium, 150), new ItemStack(Items.thorium, 100));

        new Recipe(distribution, StorageBlocks.unloader, new ItemStack(Items.densealloy, 40), new ItemStack(Items.silicon, 50));
        new Recipe(distribution, StorageBlocks.vault, new ItemStack(Items.densealloy, 500), new ItemStack(Items.thorium, 350));
        new Recipe(distribution, StorageBlocks.core,
            new ItemStack(Items.copper, 2000), new ItemStack(Items.densealloy, 1500),
            new ItemStack(Items.silicon, 1500), new ItemStack(Items.thorium, 500),
            new ItemStack(Items.surgealloy, 500), new ItemStack(Items.phasematter, 750)
        );

        //DRILLS, PRODUCERS
        new Recipe(production, ProductionBlocks.mechanicalDrill, new ItemStack(Items.copper, 50));
        new Recipe(production, ProductionBlocks.pneumaticDrill, new ItemStack(Items.copper, 60), new ItemStack(Items.densealloy, 50));
        new Recipe(production, ProductionBlocks.laserdrill, new ItemStack(Items.copper, 70), new ItemStack(Items.densealloy, 90), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 50));
        new Recipe(production, ProductionBlocks.blastdrill, new ItemStack(Items.copper, 130), new ItemStack(Items.densealloy, 180), new ItemStack(Items.silicon, 120), new ItemStack(Items.titanium, 100), new ItemStack(Items.thorium, 60));

        new Recipe(production, ProductionBlocks.waterextractor, new ItemStack(Items.copper, 50), new ItemStack(Items.densealloy, 50), new ItemStack(Items.lead, 40));
        new Recipe(production, ProductionBlocks.cultivator, new ItemStack(Items.copper, 20), new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 20));
        new Recipe(production, ProductionBlocks.oilextractor, new ItemStack(Items.copper, 300), new ItemStack(Items.densealloy, 350), new ItemStack(Items.lead, 230), new ItemStack(Items.thorium, 230), new ItemStack(Items.silicon, 150));

        //UNITS

        //bodies
        new Recipe(units, UpgradeBlocks.dartFactory, new ItemStack(Items.lead, 150), new ItemStack(Items.copper, 150), new ItemStack(Items.silicon, 200), new ItemStack(Items.titanium, 240)).setDesktop(); //dart is desktop only, because it's the starter mobile ship
        new Recipe(units, UpgradeBlocks.tridentFactory, new ItemStack(Items.lead, 250), new ItemStack(Items.copper, 250), new ItemStack(Items.silicon, 250), new ItemStack(Items.titanium, 300), new ItemStack(Items.plastanium, 200));
        new Recipe(units, UpgradeBlocks.javelinFactory, new ItemStack(Items.lead, 350), new ItemStack(Items.silicon, 450), new ItemStack(Items.titanium, 500), new ItemStack(Items.plastanium, 400), new ItemStack(Items.phasematter, 200));
        new Recipe(units, UpgradeBlocks.glaiveFactory, new ItemStack(Items.lead, 450), new ItemStack(Items.silicon, 650), new ItemStack(Items.titanium, 700), new ItemStack(Items.plastanium, 600), new ItemStack(Items.surgealloy, 200));

        new Recipe(units, UpgradeBlocks.tauFactory, new ItemStack(Items.lead, 250), new ItemStack(Items.densealloy, 250), new ItemStack(Items.copper, 250), new ItemStack(Items.silicon, 250)).setDesktop();
        new Recipe(units, UpgradeBlocks.deltaFactory, new ItemStack(Items.lead, 350), new ItemStack(Items.densealloy, 350), new ItemStack(Items.copper, 400), new ItemStack(Items.silicon, 450), new ItemStack(Items.thorium, 300)).setDesktop();
        new Recipe(units, UpgradeBlocks.omegaFactory, new ItemStack(Items.lead, 450), new ItemStack(Items.densealloy, 550), new ItemStack(Items.silicon, 650), new ItemStack(Items.thorium, 600), new ItemStack(Items.surgealloy, 240)).setDesktop();

        //actual unit related stuff
        new Recipe(units, UnitBlocks.dronePad, new ItemStack(Items.copper, 70), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 130));
        new Recipe(units, UnitBlocks.fabricatorPad, new ItemStack(Items.densealloy, 90), new ItemStack(Items.thorium, 80), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 210));

        new Recipe(units, UnitBlocks.daggerPad, new ItemStack(Items.lead, 90), new ItemStack(Items.silicon, 70));
        new Recipe(units, UnitBlocks.titanPad, new ItemStack(Items.thorium, 90), new ItemStack(Items.lead, 140), new ItemStack(Items.silicon, 90));

        new Recipe(units, UnitBlocks.interceptorPad, new ItemStack(Items.titanium, 60), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 90));
        new Recipe(units, UnitBlocks.monsoonPad, new ItemStack(Items.plastanium, 80), new ItemStack(Items.titanium, 100), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 220));

        new Recipe(units, UnitBlocks.repairPoint, new ItemStack(Items.lead, 30), new ItemStack(Items.copper, 30), new ItemStack(Items.silicon, 30));
        new Recipe(units, UnitBlocks.commandCenter, new ItemStack(Items.lead, 100), new ItemStack(Items.densealloy, 100), new ItemStack(Items.silicon, 200));

        //LIQUIDS
        new Recipe(liquid, LiquidBlocks.conduit, new ItemStack(Items.lead, 1)).setDependencies(CraftingBlocks.smelter);
        new Recipe(liquid, LiquidBlocks.pulseConduit, new ItemStack(Items.titanium, 1), new ItemStack(Items.lead, 1));
        new Recipe(liquid, LiquidBlocks.phaseConduit, new ItemStack(Items.phasematter, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.titanium, 20));

        new Recipe(liquid, LiquidBlocks.liquidRouter, new ItemStack(Items.titanium, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, LiquidBlocks.liquidtank, new ItemStack(Items.titanium, 50), new ItemStack(Items.lead, 50));
        new Recipe(liquid, LiquidBlocks.liquidJunction, new ItemStack(Items.titanium, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, LiquidBlocks.bridgeConduit, new ItemStack(Items.titanium, 8), new ItemStack(Items.lead, 8));

        new Recipe(liquid, LiquidBlocks.mechanicalPump, new ItemStack(Items.copper, 30), new ItemStack(Items.lead, 20)).setDependencies(CraftingBlocks.smelter);
        new Recipe(liquid, LiquidBlocks.rotaryPump, new ItemStack(Items.copper, 140), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 40), new ItemStack(Items.titanium, 70));
        new Recipe(liquid, LiquidBlocks.thermalPump, new ItemStack(Items.copper, 160), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 80), new ItemStack(Items.thorium, 70));

        //DEBUG
        new Recipe(units, DebugBlocks.itemSource).setMode(GameMode.sandbox).setHidden(true);
        new Recipe(units, DebugBlocks.itemVoid).setMode(GameMode.sandbox).setHidden(true);
        new Recipe(units, DebugBlocks.liquidSource).setMode(GameMode.sandbox).setHidden(true);
        new Recipe(units, DebugBlocks.powerVoid).setMode(GameMode.sandbox).setHidden(true);
        new Recipe(units, DebugBlocks.powerInfinite).setMode(GameMode.sandbox).setHidden(true);
    }

    @Override
    public ContentType type(){
        return ContentType.recipe;
    }
}
