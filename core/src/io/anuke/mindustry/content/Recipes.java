package io.anuke.mindustry.content;

import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import static io.anuke.mindustry.type.Section.*;

public class Recipes {
	static {
        new Recipe(defense, DefenseBlocks.ironwall, new ItemStack(Items.iron, 12));
        new Recipe(defense, DefenseBlocks.steelwall, new ItemStack(Items.steel, 12));
        new Recipe(defense, DefenseBlocks.titaniumwall, new ItemStack(Items.titanium, 12));
        new Recipe(defense, DefenseBlocks.diriumwall, new ItemStack(Items.densealloy, 12));
        new Recipe(defense, DefenseBlocks.steelwalllarge, new ItemStack(Items.steel, 12 * 4));
        new Recipe(defense, DefenseBlocks.titaniumwalllarge, new ItemStack(Items.titanium, 12 * 4));
        new Recipe(defense, DefenseBlocks.diriumwall, new ItemStack(Items.densealloy, 12 * 4));
        new Recipe(defense, DefenseBlocks.door, new ItemStack(Items.steel, 3), new ItemStack(Items.iron, 3 * 4));
        new Recipe(defense, DefenseBlocks.largedoor, new ItemStack(Items.steel, 3 * 4), new ItemStack(Items.iron, 3 * 4 * 4));
        new Recipe(defense, DefenseBlocks.titaniumshieldwall, new ItemStack(Items.titanium, 16));

        new Recipe(distribution, DistributionBlocks.conveyor, new ItemStack(Items.iron, 1));
        new Recipe(distribution, DistributionBlocks.steelconveyor, new ItemStack(Items.steel, 1));
        new Recipe(distribution, DistributionBlocks.pulseconveyor, new ItemStack(Items.densealloy, 1));
        new Recipe(distribution, DistributionBlocks.router, new ItemStack(Items.iron, 2));
        new Recipe(distribution, DistributionBlocks.multiplexer, new ItemStack(Items.iron, 8));
        new Recipe(distribution, DistributionBlocks.junction, new ItemStack(Items.iron, 2));
        new Recipe(distribution, DistributionBlocks.sorter, new ItemStack(Items.steel, 2));
        new Recipe(distribution, DistributionBlocks.splitter, new ItemStack(Items.steel, 1));
        new Recipe(distribution, DistributionBlocks.overflowgate, new ItemStack(Items.steel, 1));
        new Recipe(distribution, StorageBlocks.vault, new ItemStack(Items.steel, 50));
        new Recipe(distribution, StorageBlocks.core, new ItemStack(Items.steel, 50));
        new Recipe(distribution, StorageBlocks.unloader, new ItemStack(Items.steel, 5));
        new Recipe(distribution, StorageBlocks.sortedunloader, new ItemStack(Items.steel, 5));
        new Recipe(distribution, DistributionBlocks.bridgeconveyor, new ItemStack(Items.steel, 5));
        new Recipe(distribution, DistributionBlocks.laserconveyor, new ItemStack(Items.steel, 5));

        new Recipe(weapon, WeaponBlocks.doubleturret, new ItemStack(Items.iron, 7));
        new Recipe(weapon, WeaponBlocks.gatlingturret, new ItemStack(Items.iron, 8));
        new Recipe(weapon, WeaponBlocks.flameturret, new ItemStack(Items.iron, 12), new ItemStack(Items.steel, 9));
        new Recipe(weapon, WeaponBlocks.railgunturret, new ItemStack(Items.iron, 15), new ItemStack(Items.steel, 10));
        new Recipe(weapon, WeaponBlocks.laserturret, new ItemStack(Items.steel, 12), new ItemStack(Items.titanium, 12));
        new Recipe(weapon, WeaponBlocks.flakturret, new ItemStack(Items.steel, 25), new ItemStack(Items.titanium, 15));
        new Recipe(weapon, WeaponBlocks.teslaturret, new ItemStack(Items.steel, 20), new ItemStack(Items.titanium, 25), new ItemStack(Items.densealloy, 15));
        new Recipe(weapon, WeaponBlocks.liquidturret, new ItemStack(Items.steel, 80), new ItemStack(Items.titanium, 70), new ItemStack(Items.densealloy, 60));
        new Recipe(weapon, WeaponBlocks.chainturret, new ItemStack(Items.steel, 50), new ItemStack(Items.titanium, 25), new ItemStack(Items.densealloy, 40));
        new Recipe(weapon, WeaponBlocks.titanturret, new ItemStack(Items.steel, 70), new ItemStack(Items.titanium, 50), new ItemStack(Items.densealloy, 55));
        new Recipe(weapon, WeaponBlocks.missileturret, new ItemStack(Items.steel, 70), new ItemStack(Items.titanium, 50), new ItemStack(Items.densealloy, 55));
        new Recipe(weapon, WeaponBlocks.fornaxcannon, new ItemStack(Items.steel, 70), new ItemStack(Items.titanium, 50), new ItemStack(Items.densealloy, 55));

        new Recipe(crafting, CraftingBlocks.smelter, new ItemStack(Items.iron, 40));
        new Recipe(crafting, CraftingBlocks.alloysmelter, new ItemStack(Items.titanium, 50), new ItemStack(Items.steel, 50));
        new Recipe(crafting, CraftingBlocks.powersmelter, new ItemStack(Items.steel, 30), new ItemStack(Items.iron, 30));
        new Recipe(crafting, CraftingBlocks.poweralloysmelter, new ItemStack(Items.steel, 30), new ItemStack(Items.iron, 30));
        new Recipe(crafting, CraftingBlocks.separator, new ItemStack(Items.steel, 30), new ItemStack(Items.iron, 30));
        new Recipe(crafting, CraftingBlocks.centrifuge, new ItemStack(Items.steel, 30), new ItemStack(Items.iron, 30));
        new Recipe(crafting, CraftingBlocks.siliconsmelter, new ItemStack(Items.steel, 30), new ItemStack(Items.iron, 30));
        new Recipe(crafting, CraftingBlocks.oilRefinery, new ItemStack(Items.steel, 15), new ItemStack(Items.iron, 15));
        new Recipe(crafting, CraftingBlocks.biomatterCompressor, new ItemStack(Items.steel, 15), new ItemStack(Items.iron, 15));
        new Recipe(crafting, CraftingBlocks.plasticFormer, new ItemStack(Items.steel, 30), new ItemStack(Items.titanium, 15));
        new Recipe(crafting, CraftingBlocks.cryofluidmixer, new ItemStack(Items.steel, 30), new ItemStack(Items.titanium, 15));
        new Recipe(crafting, CraftingBlocks.pulverizer, new ItemStack(Items.steel, 10), new ItemStack(Items.iron, 10));
        new Recipe(crafting, CraftingBlocks.stoneFormer, new ItemStack(Items.steel, 10), new ItemStack(Items.iron, 10));
        new Recipe(crafting, CraftingBlocks.melter, new ItemStack(Items.steel, 30), new ItemStack(Items.titanium, 15));
        new Recipe(crafting, CraftingBlocks.incinerator, new ItemStack(Items.steel, 60), new ItemStack(Items.iron, 60));
        new Recipe(crafting, CraftingBlocks.weaponFactory, new ItemStack(Items.steel, 60), new ItemStack(Items.iron, 60)).setDesktop();

        new Recipe(production, ProductionBlocks.ironDrill, new ItemStack(Items.iron, 25));
        new Recipe(production, ProductionBlocks.reinforcedDrill, new ItemStack(Items.iron, 25));
        new Recipe(production, ProductionBlocks.steelDrill, new ItemStack(Items.iron, 25));
        new Recipe(production, ProductionBlocks.titaniumDrill, new ItemStack(Items.iron, 25));
        new Recipe(production, ProductionBlocks.laserdrill, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40));
        new Recipe(production, ProductionBlocks.nucleardrill, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40));
        new Recipe(production, ProductionBlocks.plasmadrill, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40));
        new Recipe(production, ProductionBlocks.cultivator, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40));
        new Recipe(production, ProductionBlocks.waterextractor, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40));
        new Recipe(production, ProductionBlocks.oilextractor, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40));

        new Recipe(power, PowerBlocks.powernode, new ItemStack(Items.steel, 3), new ItemStack(Items.iron, 3));
        new Recipe(power, PowerBlocks.powernodelarge, new ItemStack(Items.steel, 3), new ItemStack(Items.iron, 3));
        new Recipe(power, PowerBlocks.battery, new ItemStack(Items.steel, 5), new ItemStack(Items.iron, 5));
        new Recipe(power, PowerBlocks.batteryLarge, new ItemStack(Items.steel, 5), new ItemStack(Items.iron, 5));
        new Recipe(power, PowerBlocks.combustiongenerator, new ItemStack(Items.iron, 30));
        new Recipe(power, PowerBlocks.liquidcombustiongenerator, new ItemStack(Items.iron, 30));
        new Recipe(power, PowerBlocks.thermalgenerator, new ItemStack(Items.steel, 30));
        new Recipe(power, PowerBlocks.rtgenerator, new ItemStack(Items.titanium, 20), new ItemStack(Items.steel, 20));
        new Recipe(power, PowerBlocks.solarpanel, new ItemStack(Items.iron, 30), new ItemStack(Items.silicon, 20));
        new Recipe(power, PowerBlocks.largesolarpanel, new ItemStack(Items.iron, 30), new ItemStack(Items.silicon, 20));
        new Recipe(power, PowerBlocks.nuclearReactor, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40), new ItemStack(Items.steel, 50));
        new Recipe(power, PowerBlocks.fusionReactor, new ItemStack(Items.titanium, 40), new ItemStack(Items.densealloy, 40), new ItemStack(Items.steel, 50));

        new Recipe(power, PowerBlocks.shieldgenerator, new ItemStack(Items.titanium, 30), new ItemStack(Items.densealloy, 30));

        new Recipe(distribution, PowerBlocks.teleporter, new ItemStack(Items.steel, 30), new ItemStack(Items.densealloy, 40));

        new Recipe(power, PowerBlocks.repairturret, new ItemStack(Items.iron, 30));
        new Recipe(power, PowerBlocks.megarepairturret, new ItemStack(Items.iron, 20), new ItemStack(Items.steel, 30));

        new Recipe(liquid, LiquidBlocks.conduit, new ItemStack(Items.steel, 1));
        new Recipe(liquid, LiquidBlocks.pulseconduit, new ItemStack(Items.titanium, 1), new ItemStack(Items.steel, 1));
        new Recipe(liquid, LiquidBlocks.liquidrouter, new ItemStack(Items.steel, 2));
        new Recipe(liquid, LiquidBlocks.liquidtank, new ItemStack(Items.steel, 2));
        new Recipe(liquid, LiquidBlocks.liquidjunction, new ItemStack(Items.steel, 2));
        new Recipe(liquid, LiquidBlocks.bridgeconduit, new ItemStack(Items.titanium, 2), new ItemStack(Items.steel, 2));
        new Recipe(liquid, LiquidBlocks.laserconduit, new ItemStack(Items.titanium, 2), new ItemStack(Items.steel, 2));

        new Recipe(liquid, LiquidBlocks.pump, new ItemStack(Items.steel, 10));
        new Recipe(liquid, LiquidBlocks.fluxpump, new ItemStack(Items.steel, 10), new ItemStack(Items.densealloy, 5));

        new Recipe(units, UnitBlocks.repairPoint, new ItemStack(Items.steel, 10));
        new Recipe(units, UnitBlocks.resupplyPoint, new ItemStack(Items.steel, 10));

        //new Recipe(units, UnitBlocks.droneFactory, new ItemStack(Items.steel, 10));
        //new Recipe(units, UnitBlocks.vtolFactory, new ItemStack(Items.steel, 10));
        //new Recipe(units, UnitBlocks.droneFactory, new ItemStack(Items.steel, 10));
        //new Recipe(units, UnitBlocks.droneFactory, new ItemStack(Items.steel, 10));
        //new Recipe(units, UnitBlocks.walkerFactory, new ItemStack(Items.steel, 10));

        new Recipe(units, DebugBlocks.itemSource, new ItemStack(Items.steel, 10)).setDebug();
        new Recipe(units, DebugBlocks.itemVoid, new ItemStack(Items.steel, 10)).setDebug();
        new Recipe(units, DebugBlocks.liquidSource, new ItemStack(Items.steel, 10)).setDebug();
        new Recipe(units, DebugBlocks.powerVoid, new ItemStack(Items.steel, 10)).setDebug();
        new Recipe(units, DebugBlocks.powerInfinite, new ItemStack(Items.steel, 10), new ItemStack(Items.densealloy, 5)).setDebug();
    }
}
