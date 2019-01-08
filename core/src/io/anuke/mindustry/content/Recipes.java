package io.anuke.mindustry.content;

import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.game.GameMode;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.type.Recipe.RecipeVisibility;

import static io.anuke.mindustry.type.Category.*;

public class Recipes implements ContentList{

    @Override
    public void load(){
        //DEBUG
        new Recipe(distribution, Blocks.itemSource).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(distribution, Blocks.itemVoid).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(liquid, Blocks.liquidSource).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(power, Blocks.powerVoid).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);
        new Recipe(power, Blocks.powerSource).setMode(GameMode.sandbox).setHidden(true).setAlwaysUnlocked(true);

        //DEFENSE

        //walls
        new Recipe(defense, Blocks.copperWall, new ItemStack(Items.copper, 12)).setAlwaysUnlocked(true);
        new Recipe(defense, Blocks.copperWallLarge, new ItemStack(Items.copper, 12 * 4)).setAlwaysUnlocked(true);

        new Recipe(defense, Blocks.titaniumWall, new ItemStack(Items.titanium, 12));
        new Recipe(defense, Blocks.titaniumWallLarge, new ItemStack(Items.titanium, 12 * 4));

        new Recipe(defense, Blocks.door, new ItemStack(Items.titanium, 12), new ItemStack(Items.silicon, 8));
        new Recipe(defense, Blocks.doorLarge, new ItemStack(Items.titanium, 12 * 4), new ItemStack(Items.silicon, 8 * 4));

        new Recipe(defense, Blocks.thoriumWall, new ItemStack(Items.thorium, 12));
        new Recipe(defense, Blocks.thoriumWallLarge, new ItemStack(Items.thorium, 12 * 4));

        new Recipe(defense, Blocks.phaseWall, new ItemStack(Items.phasefabric, 12));
        new Recipe(defense, Blocks.phaseWallLarge, new ItemStack(Items.phasefabric, 12 * 4));

        new Recipe(defense, Blocks.surgeWall, new ItemStack(Items.surgealloy, 12));
        new Recipe(defense, Blocks.surgeWallLarge, new ItemStack(Items.surgealloy, 12 * 4));

        new Recipe(effect, Blocks.container, new ItemStack(Items.titanium, 200));
        new Recipe(effect, Blocks.vault, new ItemStack(Items.titanium, 500), new ItemStack(Items.thorium, 250));

        new Recipe(effect, Blocks.core,
            new ItemStack(Items.copper, 2000), new ItemStack(Items.titanium, 2000),
            new ItemStack(Items.silicon, 1750), new ItemStack(Items.thorium, 1000),
            new ItemStack(Items.surgealloy, 500), new ItemStack(Items.phasefabric, 750)
        );

        //projectors
        new Recipe(effect, Blocks.mendProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.titanium, 150), new ItemStack(Items.titanium, 50), new ItemStack(Items.silicon, 180));
        new Recipe(effect, Blocks.overdriveProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.titanium, 150), new ItemStack(Items.titanium, 150), new ItemStack(Items.silicon, 250));
        new Recipe(effect, Blocks.forceProjector, new ItemStack(Items.lead, 200), new ItemStack(Items.titanium, 150), new ItemStack(Items.titanium, 150), new ItemStack(Items.silicon, 250));

        new Recipe(effect, Blocks.shockMine, new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 25));

        //TURRETS
        new Recipe(turret, Blocks.duo, new ItemStack(Items.copper, 40)).setAlwaysUnlocked(true);
        new Recipe(turret, Blocks.arc, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 30), new ItemStack(Items.silicon, 20));
        new Recipe(turret, Blocks.hail, new ItemStack(Items.copper, 60), new ItemStack(Items.graphite, 35));
        new Recipe(turret, Blocks.lancer, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 90));
        new Recipe(turret, Blocks.wave, new ItemStack(Items.titanium, 70), new ItemStack(Items.lead, 150));
        new Recipe(turret, Blocks.salvo, new ItemStack(Items.copper, 210), new ItemStack(Items.graphite, 190), new ItemStack(Items.thorium, 130));
        new Recipe(turret, Blocks.swarmer, new ItemStack(Items.graphite, 70), new ItemStack(Items.titanium, 70), new ItemStack(Items.plastanium, 90), new ItemStack(Items.silicon, 60));
        new Recipe(turret, Blocks.ripple, new ItemStack(Items.copper, 300), new ItemStack(Items.graphite, 220), new ItemStack(Items.thorium, 120));
        new Recipe(turret, Blocks.cyclone, new ItemStack(Items.copper, 400), new ItemStack(Items.surgealloy, 200), new ItemStack(Items.plastanium, 150));
        new Recipe(turret, Blocks.fuse, new ItemStack(Items.copper, 450), new ItemStack(Items.graphite, 450), new ItemStack(Items.surgealloy, 250));
        new Recipe(turret, Blocks.spectre, new ItemStack(Items.copper, 700), new ItemStack(Items.graphite, 600), new ItemStack(Items.surgealloy, 500), new ItemStack(Items.plastanium, 350), new ItemStack(Items.thorium, 500));
        new Recipe(turret, Blocks.meltdown, new ItemStack(Items.copper, 500), new ItemStack(Items.lead, 700), new ItemStack(Items.graphite, 600), new ItemStack(Items.surgealloy, 650), new ItemStack(Items.silicon, 650));

        //DISTRIBUTION
        new Recipe(distribution, Blocks.conveyor, new ItemStack(Items.copper, 1)).setAlwaysUnlocked(true);
        new Recipe(distribution, Blocks.titaniumConveyor, new ItemStack(Items.copper, 2), new ItemStack(Items.titanium, 1));
        new Recipe(distribution, Blocks.phaseConveyor, new ItemStack(Items.phasefabric, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.graphite, 20));

        //starter transport
        new Recipe(distribution, Blocks.junction, new ItemStack(Items.copper, 2)).setAlwaysUnlocked(true);
        new Recipe(distribution, Blocks.router, new ItemStack(Items.copper, 6)).setAlwaysUnlocked(true);

        //more advanced transport
        new Recipe(distribution, Blocks.distributor, new ItemStack(Items.titanium, 8), new ItemStack(Items.copper, 8));
        new Recipe(distribution, Blocks.sorter, new ItemStack(Items.titanium, 4), new ItemStack(Items.copper, 4));
        new Recipe(distribution, Blocks.overflowGate, new ItemStack(Items.titanium, 4), new ItemStack(Items.copper, 8));
        new Recipe(distribution, Blocks.itemBridge, new ItemStack(Items.titanium, 8), new ItemStack(Items.copper, 8));
        new Recipe(distribution, Blocks.unloader, new ItemStack(Items.titanium, 50), new ItemStack(Items.silicon, 60));
        new Recipe(distribution, Blocks.massDriver, new ItemStack(Items.titanium, 250), new ItemStack(Items.silicon, 150), new ItemStack(Items.lead, 250), new ItemStack(Items.thorium, 100));

        //CRAFTING

        //smelting
        new Recipe(crafting, Blocks.siliconSmelter, new ItemStack(Items.copper, 60), new ItemStack(Items.lead, 50));

        //advanced fabrication
        new Recipe(crafting, Blocks.plastaniumCompressor, new ItemStack(Items.silicon, 160), new ItemStack(Items.lead, 230), new ItemStack(Items.graphite, 120), new ItemStack(Items.titanium, 160));
        new Recipe(crafting, Blocks.phaseWeaver, new ItemStack(Items.silicon, 260), new ItemStack(Items.lead, 240), new ItemStack(Items.thorium, 150));
        new Recipe(crafting, Blocks.surgeSmelter, new ItemStack(Items.silicon, 160), new ItemStack(Items.lead, 160), new ItemStack(Items.thorium, 140));

        //misc
        new Recipe(crafting, Blocks.pulverizer, new ItemStack(Items.copper, 60), new ItemStack(Items.lead, 50));
        new Recipe(crafting, Blocks.pyratiteMixer, new ItemStack(Items.copper, 100), new ItemStack(Items.lead, 50));
        new Recipe(crafting, Blocks.blastMixer, new ItemStack(Items.lead, 60), new ItemStack(Items.titanium, 40));
        new Recipe(crafting, Blocks.cryofluidMixer, new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 80), new ItemStack(Items.thorium, 90));

        new Recipe(crafting, Blocks.melter, new ItemStack(Items.copper, 60), new ItemStack(Items.lead, 70), new ItemStack(Items.graphite, 90));
        new Recipe(crafting, Blocks.incinerator, new ItemStack(Items.graphite, 10), new ItemStack(Items.lead, 30));

        //processing
        new Recipe(crafting, Blocks.biomatterCompressor, new ItemStack(Items.lead, 70), new ItemStack(Items.silicon, 60));
        new Recipe(crafting, Blocks.separator, new ItemStack(Items.copper, 60), new ItemStack(Items.titanium, 50));

        //POWER
        new Recipe(power, Blocks.powerNode, new ItemStack(Items.copper, 2), new ItemStack(Items.lead, 6));
        new Recipe(power, Blocks.powerNodeLarge, new ItemStack(Items.titanium, 10), new ItemStack(Items.lead, 20), new ItemStack(Items.silicon, 6));
        new Recipe(power, Blocks.battery, new ItemStack(Items.copper, 8), new ItemStack(Items.lead, 30), new ItemStack(Items.silicon, 4));
        new Recipe(power, Blocks.batteryLarge, new ItemStack(Items.titanium, 40), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 30));

        //generators - combustion
        new Recipe(power, Blocks.combustionGenerator, new ItemStack(Items.copper, 50), new ItemStack(Items.lead, 30));
        new Recipe(power, Blocks.turbineGenerator, new ItemStack(Items.copper, 70), new ItemStack(Items.graphite, 50), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 60));
        new Recipe(power, Blocks.thermalGenerator, new ItemStack(Items.copper, 80), new ItemStack(Items.graphite, 70), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 70), new ItemStack(Items.thorium, 70));

        //generators - solar
        new Recipe(power, Blocks.solarPanel, new ItemStack(Items.lead, 20), new ItemStack(Items.silicon, 30));
        new Recipe(power, Blocks.largeSolarPanel, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 290), new ItemStack(Items.phasefabric, 30));

        //generators - nuclear
        new Recipe(power, Blocks.thoriumReactor, new ItemStack(Items.lead, 600), new ItemStack(Items.silicon, 400), new ItemStack(Items.graphite, 300), new ItemStack(Items.thorium, 300));
        new Recipe(power, Blocks.rtgGenerator, new ItemStack(Items.lead, 200), new ItemStack(Items.silicon, 150), new ItemStack(Items.phasefabric, 50), new ItemStack(Items.plastanium, 150), new ItemStack(Items.thorium, 100));

        //DRILLS, PRODUCERS
        new Recipe(production, Blocks.mechanicalDrill, new ItemStack(Items.copper, 45)).setAlwaysUnlocked(true);
        new Recipe(production, Blocks.pneumaticDrill, new ItemStack(Items.copper, 60), new ItemStack(Items.graphite, 50));
        new Recipe(production, Blocks.laserDrill, new ItemStack(Items.copper, 70), new ItemStack(Items.graphite, 90), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 50));
        new Recipe(production, Blocks.blastDrill, new ItemStack(Items.copper, 130), new ItemStack(Items.silicon, 120), new ItemStack(Items.titanium, 100), new ItemStack(Items.thorium, 60));

        new Recipe(production, Blocks.waterExtractor, new ItemStack(Items.copper, 50), new ItemStack(Items.graphite, 50), new ItemStack(Items.lead, 40));
        new Recipe(production, Blocks.cultivator, new ItemStack(Items.copper, 20), new ItemStack(Items.lead, 50), new ItemStack(Items.silicon, 20));
        new Recipe(production, Blocks.oilExtractor, new ItemStack(Items.copper, 300), new ItemStack(Items.graphite, 350), new ItemStack(Items.lead, 230), new ItemStack(Items.thorium, 230), new ItemStack(Items.silicon, 150));

        //UNITS

        //upgrades
        new Recipe(upgrade, Blocks.dartPad, new ItemStack(Items.lead, 150), new ItemStack(Items.copper, 150), new ItemStack(Items.silicon, 200), new ItemStack(Items.titanium, 240)).setVisible(RecipeVisibility.desktopOnly);
        new Recipe(upgrade, Blocks.tridentPad, new ItemStack(Items.lead, 250), new ItemStack(Items.copper, 250), new ItemStack(Items.silicon, 250), new ItemStack(Items.titanium, 300), new ItemStack(Items.plastanium, 200));
        new Recipe(upgrade, Blocks.javelinPad, new ItemStack(Items.lead, 350), new ItemStack(Items.silicon, 450), new ItemStack(Items.titanium, 500), new ItemStack(Items.plastanium, 400), new ItemStack(Items.phasefabric, 200));
        new Recipe(upgrade, Blocks.glaivePad, new ItemStack(Items.lead, 450), new ItemStack(Items.silicon, 650), new ItemStack(Items.titanium, 700), new ItemStack(Items.plastanium, 600), new ItemStack(Items.surgealloy, 200));

        new Recipe(upgrade, Blocks.alphaPad, new ItemStack(Items.lead, 200), new ItemStack(Items.graphite, 100), new ItemStack(Items.copper, 150)).setVisible(RecipeVisibility.mobileOnly);
        new Recipe(upgrade, Blocks.tauPad, new ItemStack(Items.lead, 250), new ItemStack(Items.titanium, 250), new ItemStack(Items.copper, 250), new ItemStack(Items.silicon, 250));
        new Recipe(upgrade, Blocks.deltaPad, new ItemStack(Items.lead, 350), new ItemStack(Items.titanium, 350), new ItemStack(Items.copper, 400), new ItemStack(Items.silicon, 450), new ItemStack(Items.thorium, 300));
        new Recipe(upgrade, Blocks.omegaPad, new ItemStack(Items.lead, 450), new ItemStack(Items.graphite, 550), new ItemStack(Items.silicon, 650), new ItemStack(Items.thorium, 600), new ItemStack(Items.surgealloy, 240));

        //unit factories
        new Recipe(units, Blocks.spiritFactory, new ItemStack(Items.copper, 70), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 130));
        new Recipe(units, Blocks.phantomFactory, new ItemStack(Items.titanium, 90), new ItemStack(Items.thorium, 80), new ItemStack(Items.lead, 110), new ItemStack(Items.silicon, 210));

        new Recipe(units, Blocks.daggerFactory, new ItemStack(Items.lead, 90), new ItemStack(Items.silicon, 70));
        new Recipe(units, Blocks.titanFactory, new ItemStack(Items.thorium, 90), new ItemStack(Items.lead, 140), new ItemStack(Items.silicon, 90));
        new Recipe(units, Blocks.fortressFactory, new ItemStack(Items.thorium, 200), new ItemStack(Items.lead, 220), new ItemStack(Items.silicon, 150), new ItemStack(Items.surgealloy, 100), new ItemStack(Items.phasefabric, 50));

        new Recipe(units, Blocks.wraithFactory, new ItemStack(Items.titanium, 60), new ItemStack(Items.lead, 80), new ItemStack(Items.silicon, 90));
        new Recipe(units, Blocks.ghoulFactory, new ItemStack(Items.plastanium, 80), new ItemStack(Items.titanium, 100), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 220));
        new Recipe(units, Blocks.revenantFactory, new ItemStack(Items.plastanium, 300), new ItemStack(Items.titanium, 400), new ItemStack(Items.lead, 300), new ItemStack(Items.silicon, 400), new ItemStack(Items.surgealloy, 100));

        new Recipe(units, Blocks.repairPoint, new ItemStack(Items.lead, 30), new ItemStack(Items.copper, 30), new ItemStack(Items.silicon, 30));

        //removed for testing MOBA-style unit production
        //new Recipe(units, Blocks.commandCenter, new ItemStack(Items.lead, 100), new ItemStack(Items.densealloy, 100), new ItemStack(Items.silicon, 200));

        //LIQUIDS
        new Recipe(liquid, Blocks.conduit, new ItemStack(Items.lead, 1));
        new Recipe(liquid, Blocks.pulseConduit, new ItemStack(Items.titanium, 1), new ItemStack(Items.lead, 1));
        new Recipe(liquid, Blocks.phaseConduit, new ItemStack(Items.phasefabric, 10), new ItemStack(Items.silicon, 15), new ItemStack(Items.lead, 20), new ItemStack(Items.titanium, 20));

        new Recipe(liquid, Blocks.liquidRouter, new ItemStack(Items.titanium, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, Blocks.liquidTank, new ItemStack(Items.titanium, 50), new ItemStack(Items.lead, 50));
        new Recipe(liquid, Blocks.liquidJunction, new ItemStack(Items.titanium, 4), new ItemStack(Items.lead, 4));
        new Recipe(liquid, Blocks.bridgeConduit, new ItemStack(Items.titanium, 8), new ItemStack(Items.lead, 8));

        new Recipe(liquid, Blocks.mechanicalPump, new ItemStack(Items.copper, 30), new ItemStack(Items.lead, 20));
        new Recipe(liquid, Blocks.rotaryPump, new ItemStack(Items.copper, 140), new ItemStack(Items.lead, 100), new ItemStack(Items.silicon, 40), new ItemStack(Items.titanium, 70));
        new Recipe(liquid, Blocks.thermalPump, new ItemStack(Items.copper, 160), new ItemStack(Items.lead, 130), new ItemStack(Items.silicon, 60), new ItemStack(Items.titanium, 80), new ItemStack(Items.thorium, 70));
    }
}
