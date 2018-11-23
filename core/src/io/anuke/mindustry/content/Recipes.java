package io.anuke.mindustry.content;

import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.type.Recipe.RecipeVisibility;

import static io.anuke.mindustry.type.Category.*;

public class Recipes implements ContentList{

    @Override
    public void load(){
        //DEBUG
        new Recipe(distribution, DebugBlocks.itemSource).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(distribution, DebugBlocks.itemVoid).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(liquid, DebugBlocks.liquidSource).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(power, DebugBlocks.powerVoid).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(power, DebugBlocks.powerInfinite).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);

        //DEFENSE

        //walls
        new Recipe(defense, DefenseBlocks.copperWall, new ItemStack(Items.copper, 12)).setAlwaysUnlocked(true);
        new Recipe(defense, DefenseBlocks.copperWallLarge, new ItemStack(Items.copper, 12 * 4)).setAlwaysUnlocked(true);

        new Recipe(defense, DefenseBlocks.denseAlloyWall, new ItemStack(Items.densealloy, 12));
        new Recipe(defense, DefenseBlocks.denseAlloyWallLarge, new ItemStack(Items.densealloy, 12 * 4));

        new Recipe(defense, DefenseBlocks.door, new ItemStack(Items.densealloy, 12), new ItemStack(Items.silicon, 8));
        new Recipe(defense, DefenseBlocks.doorLarge, new ItemStack(Items.densealloy, 12 * 4), new ItemStack(Items.silicon, 8 * 4));

        new Recipe(defense, DefenseBlocks.thoriumWall, new ItemStack(Items.thorium, 12));
        new Recipe(defense, DefenseBlocks.thoriumWallLarge, new ItemStack(Items.thorium, 12 * 4));

        new Recipe(defense, DefenseBlocks.phaseWall, new ItemStack(Items.phasefabric, 12));
        new Recipe(defense, DefenseBlocks.phaseWallLarge, new ItemStack(Items.phasefabric, 12 * 4));

        new Recipe(defense, DefenseBlocks.surgeWall, new ItemStack(Items.surgealloy, 12));
        new Recipe(defense, DefenseBlocks.surgeWallLarge, new ItemStack(Items.surgealloy, 12 * 4));

        //projectors
        new Recipe(effect, DefenseBlocks.mendProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 150), new ItemStack(Items.titanium, 50), new ItemStack(Items.silicon, 180));
        new Recipe(effect, DefenseBlocks.overdriveProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 150), new ItemStack(Items.titanium, 150), new ItemStack(Items.silicon, 250));
        new Recipe(effect, DefenseBlocks.forceProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 150), new ItemStack(Items.titanium, 150), new ItemStack(Items.silicon, 250));

        new Recipe(effect, StorageBlocks.unloader, new ItemStack(Items.densealloy, 50), new ItemStack(Items.silicon, 60));
        new Recipe(effect, StorageBlocks.container, new ItemStack(Items.densealloy, 200));
        new Recipe(effect, StorageBlocks.vault, new ItemStack(Items.densealloy, 500), new ItemStack(Items.thorium, 250));

        new Recipe(effect, DefenseBlocks.shockMine, new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 25))
            .setDependencies(Items.blastCompound);

        //TURRETS
        new Recipe(turret, TurretBlocks.duo, new ItemStack(Items.copper, 40)).setAlwaysUnlocked(true);
        new Recipe(turret, TurretBlocks.arc, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 30), new ItemStack(Items.silicon, 20));
        new Recipe(turret, TurretBlocks.hail, new ItemStack(Items.copper, 60), new ItemStack(Items.densealloy, 35));
        new Recipe(turret, TurretBlocks.lancer, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 90));
        new Recipe(turret, TurretBlocks.wave, new ItemStack(Items.densealloy, 60), new ItemStack(Items.titanium, 70), new ItemStack(Items.lead, 150));
        new Recipe(turret, TurretBlocks.salvo, new ItemStack(Items.copper, 210), new ItemStack(Items.densealloy, 190), new ItemStack(Items.thorium, 130));
        new Recipe(turret, TurretBlocks.swarmer, new ItemStack(Items.densealloy, 70), new ItemStack(Items.titanium, 70), new ItemStack(Items.plastanium, 90), new ItemStack(Items.silicon, 60));
        new Recipe(turret, TurretBlocks.ripple, new ItemStack(Items.copper, 300), new ItemStack(Items.densealloy, 220), new ItemStack(Items.thorium, 120));
        new Recipe(turret, TurretBlocks.cyclone, new ItemStack(Items.copper, 400), new ItemStack(Items.densealloy, 400), new ItemStack(Items.surgealloy, 200), new ItemStack(Items.plastanium, 150));
        new Recipe(turret, TurretBlocks.fuse, new ItemStack(Items.copper, 450), new ItemStack(Items.densealloy, 450), new ItemStack(Items.surgealloy, 250));
        new Recipe(turret, TurretBlocks.spectre, new ItemStack(Items.copper, 700), new ItemStack(Items.densealloy, 600), new ItemStack(Items.surgealloy, 500), new ItemStack(Items.plastanium, 350), new ItemStack(Items.thorium, 500));
        new Recipe(turret, TurretBlocks.meltdown, new ItemStack(Items.copper, 500), new ItemStack(Items.lead, 700), new ItemStack(Items.densealloy, 600), new ItemStack(Items.surgealloy, 650), new ItemStack(Items.silicon, 650));

        //DISTRIBUTION
        new Recipe(distribution, DistributionBlocks.conveyor, new ItemStack(Items.copper, 1)).setAlwaysUnlocked(true);
        new Recipe(distribution, DistributionBlocks.titaniumconveyor, new ItemStack(Items.copper, 2), new ItemStack(Items.titanium, 1));
        new Recipe(distribution, DistributionBlocks.phaseConveyor, new ItemStack(Items.phasefabric, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.densealloy, 20));

        //starter transport
        new Recipe(distribution, DistributionBlocks.junction, new ItemStack(Items.copper, 2)).setAlwaysUnlocked(true);
        new Recipe(distribution, DistributionBlocks.router, new ItemStack(Items.copper, 6)).setAlwaysUnlocked(true);

        //advanced densealloy transporat
        new Recipe(distribution, DistributionBlocks.distributor, new ItemStack(Items.densealloy, 8), new ItemStack(Items.copper, 8));
        new Recipe(distribution, DistributionBlocks.sorter, new ItemStack(Items.densealloy, 4), new ItemStack(Items.copper, 4));
        new Recipe(distribution, DistributionBlocks.overflowGate, new ItemStack(Items.densealloy, 4), new ItemStack(Items.copper, 8));
        new Recipe(distribution, DistributionBlocks.itemBridge, new ItemStack(Items.densealloy, 8), new ItemStack(Items.copper, 8));
        new Recipe(distribution, DistributionBlocks.massDriver, new ItemStack(Items.densealloy, 250), new ItemStack(Items.silicon, 150), new ItemStack(Items.lead, 250), new ItemStack(Items.thorium, 100));

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
        new Recipe(power, PowerBlocks.largeSolarPanel, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 290), new ItemStack(Items.phasefabric, 30));

        //generators - nuclear
        new Recipe(power, PowerBlocks.thoriumReactor, new ItemStack(Items.lead, 600), new ItemStack(Items.silicon, 400), new ItemStack(Items.densealloy, 300), new ItemStack(Items.thorium, 300));
        new Recipe(power, PowerBlocks.rtgGenerator, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 150), new ItemStack(Items.phasefabric, 50), new ItemStack(Items.plastanium, 150), new ItemStack(Items.thorium, 100));

        //core disabled due to being broken
        /*new Recipe(distribution, StorageBlocks.core,
            new ItemStack(Items.copper, 2000), new ItemStack(Items.densealloy, 1500),
            new ItemStack(Items.silicon, 1500), new ItemStack(Items.thorium, 500),
            new ItemStack(Items.surgealloy, 500), new ItemStack(Items.phasefabric, 750)
        );*/

        //DRILLS, PRODUCERS
        new Recipe(production, ProductionBlocks.mechanicalDrill, new ItemStack(Items.copper, 45)).setAlwaysUnlocked(true);
        new Recipe(production, ProductionBlocks.pneumaticDrill, new ItemStack(Items.copper, 60), new ItemStack(Items.densealloy, 50));
        new Recipe(production, ProductionBlocks.laserDrill, new ItemStack(Items.copper, 70), new ItemStack(Items.densealloy, 90), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 50));
        new Recipe(production, ProductionBlocks.blastDrill, new ItemStack(Items.copper, 130), new ItemStack(Items.densealloy, 180), new ItemStack(Items.silicon, 120), new ItemStack(Items.titanium, 100), new ItemStack(Items.thorium, 60));

        new Recipe(production, ProductionBlocks.waterExtractor, new ItemStack(Items.copper, 50), new ItemStack(Items.densealloy, 50), new ItemStack(Items.lead, 40));
        new Recipe(production, ProductionBlocks.cultivator, new ItemStack(Items.copper, 20), new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 20));
        new Recipe(production, ProductionBlocks.oilExtractor, new ItemStack(Items.copper, 300), new ItemStack(Items.densealloy, 350), new ItemStack(Items.lead, 230), new ItemStack(Items.thorium, 230), new ItemStack(Items.silicon, 150));

        //UNITS

        //upgrades
        new Recipe(upgrade, UpgradeBlocks.dartPad, new ItemStack(Items.lead, 150), new ItemStack(Items.copper, 150), new ItemStack(Items.silicon, 200), new ItemStack(Items.titanium, 240)).setVisible(RecipeVisibility.desktopOnly);
        new Recipe(upgrade, UpgradeBlocks.tridentPad, new ItemStack(Items.lead, 250), new ItemStack(Items.copper, 250), new ItemStack(Items.silicon, 250), new ItemStack(Items.titanium, 300), new ItemStack(Items.plastanium, 200));
        new Recipe(upgrade, UpgradeBlocks.javelinPad, new ItemStack(Items.lead, 350), new ItemStack(Items.silicon, 450), new ItemStack(Items.titanium, 500), new ItemStack(Items.plastanium, 400), new ItemStack(Items.phasefabric, 200));
        new Recipe(upgrade, UpgradeBlocks.glaivePad, new ItemStack(Items.lead, 450), new ItemStack(Items.silicon, 650), new ItemStack(Items.titanium, 700), new ItemStack(Items.plastanium, 600), new ItemStack(Items.surgealloy, 200));

        new Recipe(upgrade, UpgradeBlocks.alphaPad, new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 100), new ItemStack(Items.copper, 150)).setVisible(RecipeVisibility.mobileOnly);
        new Recipe(upgrade, UpgradeBlocks.tauPad, new ItemStack(Items.lead, 250), new ItemStack(Items.densealloy, 250), new ItemStack(Items.copper, 250), new ItemStack(Items.silicon, 250));
        new Recipe(upgrade, UpgradeBlocks.deltaPad, new ItemStack(Items.lead, 350), new ItemStack(Items.densealloy, 350), new ItemStack(Items.copper, 400), new ItemStack(Items.silicon, 450), new ItemStack(Items.thorium, 300));
        new Recipe(upgrade, UpgradeBlocks.omegaPad, new ItemStack(Items.lead, 450), new ItemStack(Items.densealloy, 550), new ItemStack(Items.silicon, 650), new ItemStack(Items.thorium, 600), new ItemStack(Items.surgealloy, 240));

        //actual unit related stuff
        new Recipe(units, UnitBlocks.spiritFactory, new ItemStack(Items.copper, 70), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 130));
        new Recipe(units, UnitBlocks.phantomFactory, new ItemStack(Items.densealloy, 90), new ItemStack(Items.thorium, 80), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 210));

        new Recipe(units, UnitBlocks.daggerFactory, new ItemStack(Items.lead, 90), new ItemStack(Items.silicon, 70));
        new Recipe(units, UnitBlocks.titanFactory, new ItemStack(Items.thorium, 90), new ItemStack(Items.lead, 140), new ItemStack(Items.silicon, 90));
        new Recipe(units, UnitBlocks.fortressFactory, new ItemStack(Items.thorium, 200), new ItemStack(Items.lead, 220), new ItemStack(Items.silicon, 150), new ItemStack(Items.surgealloy, 100), new ItemStack(Items.phasefabric, 50));

        new Recipe(units, UnitBlocks.wraithFactory, new ItemStack(Items.titanium, 60), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 90));
        new Recipe(units, UnitBlocks.ghoulFactory, new ItemStack(Items.plastanium, 80), new ItemStack(Items.titanium, 100), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 220));
        new Recipe(units, UnitBlocks.revenantFactory, new ItemStack(Items.plastanium, 300), new ItemStack(Items.titanium, 400), new ItemStack(Items.lead, 300), new ItemStack(Items.silicon, 400), new ItemStack(Items.surgealloy, 100));

        new Recipe(units, UnitBlocks.repairPoint, new ItemStack(Items.lead, 30), new ItemStack(Items.copper, 30), new ItemStack(Items.silicon, 30));
        new Recipe(units, UnitBlocks.commandCenter, new ItemStack(Items.lead, 100), new ItemStack(Items.densealloy, 100), new ItemStack(Items.silicon, 200));

        //LIQUIDS
        new Recipe(liquid, LiquidBlocks.conduit, new ItemStack(Items.lead, 1)).setDependencies(CraftingBlocks.smelter);
        new Recipe(liquid, LiquidBlocks.pulseConduit, new ItemStack(Items.titanium, 1), new ItemStack(Items.lead, 1));
        new Recipe(liquid, LiquidBlocks.phaseConduit, new ItemStack(Items.phasefabric, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.titanium, 20));

        new Recipe(liquid, LiquidBlocks.liquidRouter, new ItemStack(Items.titanium, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, LiquidBlocks.liquidtank, new ItemStack(Items.titanium, 50), new ItemStack(Items.lead, 50));
        new Recipe(liquid, LiquidBlocks.liquidJunction, new ItemStack(Items.titanium, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, LiquidBlocks.bridgeConduit, new ItemStack(Items.titanium, 8), new ItemStack(Items.lead, 8));

        new Recipe(liquid, LiquidBlocks.mechanicalPump, new ItemStack(Items.copper, 30), new ItemStack(Items.lead, 20)).setDependencies(CraftingBlocks.smelter);
        new Recipe(liquid, LiquidBlocks.rotaryPump, new ItemStack(Items.copper, 140), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 40), new ItemStack(Items.titanium, 70));
        new Recipe(liquid, LiquidBlocks.thermalPump, new ItemStack(Items.copper, 160), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 80), new ItemStack(Items.thorium, 70));
    }

    @Override
    public ContentType type(){
        return ContentType.recipe;
    }
}
