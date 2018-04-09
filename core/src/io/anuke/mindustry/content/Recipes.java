package io.anuke.mindustry.content;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Section;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.Vars.debug;
import static io.anuke.mindustry.resource.Section.*;

public class Recipes {
	private static final Array<Recipe> list = Array.with(
		//new Recipe(defense, DefenseBlocks.stonewall, stack(Item.stone, 12)),
		new Recipe(defense, DefenseBlocks.ironwall, stack(Items.iron, 12)),
		new Recipe(defense, DefenseBlocks.steelwall, stack(Items.steel, 12)),
		new Recipe(defense, DefenseBlocks.titaniumwall, stack(Items.titanium, 12)),
		new Recipe(defense, DefenseBlocks.diriumwall, stack(Items.densealloy, 12)),
		new Recipe(defense, DefenseBlocks.steelwalllarge, stack(Items.steel, 12*4)),
		new Recipe(defense, DefenseBlocks.titaniumwalllarge, stack(Items.titanium, 12*4)),
		new Recipe(defense, DefenseBlocks.diriumwall, stack(Items.densealloy, 12*4)),
		new Recipe(defense, DefenseBlocks.door, stack(Items.steel, 3), stack(Items.iron, 3*4)).setDesktop(),
		new Recipe(defense, DefenseBlocks.largedoor, stack(Items.steel, 3*4), stack(Items.iron, 3*4*4)).setDesktop(),
		new Recipe(defense, DefenseBlocks.titaniumshieldwall, stack(Items.titanium, 16)),

		new Recipe(distribution, DistributionBlocks.conveyor, stack(Items.iron, 1)),
		new Recipe(distribution, DistributionBlocks.steelconveyor, stack(Items.steel, 1)),
		new Recipe(distribution, DistributionBlocks.pulseconveyor, stack(Items.densealloy, 1)),
		new Recipe(distribution, DistributionBlocks.router, stack(Items.iron, 2)),
		new Recipe(distribution, DistributionBlocks.multiplexer, stack(Items.iron, 8)),
		new Recipe(distribution, DistributionBlocks.junction, stack(Items.iron, 2)),
		new Recipe(distribution, DistributionBlocks.tunnel, stack(Items.iron, 2)),
		new Recipe(distribution, DistributionBlocks.sorter, stack(Items.steel, 2)),
		new Recipe(distribution, DistributionBlocks.splitter, stack(Items.steel, 1)),
		new Recipe(distribution, StorageBlocks.vault, stack(Items.steel, 50)),
		new Recipe(distribution, StorageBlocks.core, stack(Items.steel, 50)),
		new Recipe(distribution, StorageBlocks.unloader, stack(Items.steel, 5)),
		new Recipe(distribution, StorageBlocks.sortedunloader, stack(Items.steel, 5)),
		new Recipe(distribution, DistributionBlocks.itembridge, stack(Items.steel, 5)),

		new Recipe(weapon, WeaponBlocks.doubleturret, stack(Items.iron, 7)),
		new Recipe(weapon, WeaponBlocks.gatlingturret, stack(Items.iron, 8)),
		new Recipe(weapon, WeaponBlocks.flameturret, stack(Items.iron, 12), stack(Items.steel, 9)),
		new Recipe(weapon, WeaponBlocks.railgunturret, stack(Items.iron, 15), stack(Items.steel, 10)),
		new Recipe(weapon, WeaponBlocks.laserturret, stack(Items.steel, 12), stack(Items.titanium, 12)),
		new Recipe(weapon, WeaponBlocks.flakturret, stack(Items.steel, 25), stack(Items.titanium, 15)),
		new Recipe(weapon, WeaponBlocks.teslaturret, stack(Items.steel, 20), stack(Items.titanium, 25), stack(Items.densealloy, 15)),
		new Recipe(weapon, WeaponBlocks.liquidturret, stack(Items.steel, 80), stack(Items.titanium, 70), stack(Items.densealloy, 60)),
		new Recipe(weapon, WeaponBlocks.chainturret, stack(Items.steel, 50), stack(Items.titanium, 25), stack(Items.densealloy, 40)),
		new Recipe(weapon, WeaponBlocks.titanturret, stack(Items.steel, 70), stack(Items.titanium, 50), stack(Items.densealloy, 55)),
		new Recipe(weapon, WeaponBlocks.missileturret, stack(Items.steel, 70), stack(Items.titanium, 50), stack(Items.densealloy, 55)),
		new Recipe(weapon, WeaponBlocks.fornaxcannon, stack(Items.steel, 70), stack(Items.titanium, 50), stack(Items.densealloy, 55)),

		new Recipe(crafting, CraftingBlocks.smelter, stack(Items.iron, 40)),
		new Recipe(crafting, CraftingBlocks.alloysmelter, stack(Items.titanium, 50), stack(Items.steel, 50)),
		new Recipe(crafting, CraftingBlocks.powersmelter, stack(Items.steel, 30), stack(Items.iron, 30)),
		new Recipe(crafting, CraftingBlocks.poweralloysmelter, stack(Items.steel, 30), stack(Items.iron, 30)),
		new Recipe(crafting, CraftingBlocks.separator, stack(Items.steel, 30), stack(Items.iron, 30)),
		new Recipe(crafting, CraftingBlocks.centrifuge, stack(Items.steel, 30), stack(Items.iron, 30)),
		new Recipe(crafting, CraftingBlocks.siliconsmelter, stack(Items.steel, 30), stack(Items.iron, 30)),
		new Recipe(crafting, CraftingBlocks.oilRefinery, stack(Items.steel, 15), stack(Items.iron, 15)),
		new Recipe(crafting, CraftingBlocks.biomatterCompressor, stack(Items.steel, 15), stack(Items.iron, 15)),
		new Recipe(crafting, CraftingBlocks.plasticFormer, stack(Items.steel, 30), stack(Items.titanium, 15)),
		new Recipe(crafting, CraftingBlocks.cryofluidmixer, stack(Items.steel, 30), stack(Items.titanium, 15)),
		new Recipe(crafting, CraftingBlocks.pulverizer, stack(Items.steel, 10), stack(Items.iron, 10)),
		new Recipe(crafting, CraftingBlocks.stoneFormer, stack(Items.steel, 10), stack(Items.iron, 10)),
		new Recipe(crafting, CraftingBlocks.melter, stack(Items.steel, 30), stack(Items.titanium, 15)),
		new Recipe(crafting, CraftingBlocks.incinerator, stack(Items.steel, 60), stack(Items.iron, 60)),
		new Recipe(crafting, CraftingBlocks.weaponFactory, stack(Items.steel, 60), stack(Items.iron, 60)).setDesktop(),

		//new Recipe(production, ProductionBlocks.stonedrill, stack(Item.stone, 12)),
		new Recipe(production, ProductionBlocks.ironDrill, stack(Items.iron, 25)),
		new Recipe(production, ProductionBlocks.reinforcedDrill, stack(Items.iron, 25)),
		new Recipe(production, ProductionBlocks.steelDrill, stack(Items.iron, 25)),
		new Recipe(production, ProductionBlocks.titaniumDrill, stack(Items.iron, 25)),
		new Recipe(production, ProductionBlocks.laserdrill, stack(Items.titanium, 40), stack(Items.densealloy, 40)),
		new Recipe(production, ProductionBlocks.nucleardrill, stack(Items.titanium, 40), stack(Items.densealloy, 40)),
		new Recipe(production, ProductionBlocks.plasmadrill, stack(Items.titanium, 40), stack(Items.densealloy, 40)),
		//new Recipe(production, ProductionBlocks.leaddrill, stack(Items.iron, 25)),
		//new Recipe(production, ProductionBlocks.coaldrill, stack(Items.iron, 25), stack(Items.iron, 40)),
		//new Recipe(production, ProductionBlocks.titaniumdrill, stack(Items.iron, 50), stack(Items.steel, 50)),
		//new Recipe(production, ProductionBlocks.thoriumdrill, stack(Items.iron, 40), stack(Items.steel, 40)),
		//new Recipe(production, ProductionBlocks.quartzextractor, stack(Items.titanium, 40), stack(Items.densealloy, 40)),
		new Recipe(production, ProductionBlocks.cultivator, stack(Items.titanium, 40), stack(Items.densealloy, 40)),
		new Recipe(production, ProductionBlocks.waterextractor, stack(Items.titanium, 40), stack(Items.densealloy, 40)),
		new Recipe(production, ProductionBlocks.oilextractor, stack(Items.titanium, 40), stack(Items.densealloy, 40)),

		new Recipe(power, PowerBlocks.powernode, stack(Items.steel, 3), stack(Items.iron, 3)),
		new Recipe(power, PowerBlocks.powernodelarge, stack(Items.steel, 3), stack(Items.iron, 3)),
		new Recipe(power, PowerBlocks.battery, stack(Items.steel, 5), stack(Items.iron, 5)),
		new Recipe(power, PowerBlocks.batteryLarge, stack(Items.steel, 5), stack(Items.iron, 5)),
		new Recipe(power, PowerBlocks.combustiongenerator, stack(Items.iron, 30)),
		new Recipe(power, PowerBlocks.liquidcombustiongenerator, stack(Items.iron, 30)),
		new Recipe(power, PowerBlocks.thermalgenerator, stack(Items.steel, 30)),
		new Recipe(power, PowerBlocks.rtgenerator, stack(Items.titanium, 20), stack(Items.steel, 20)),
		new Recipe(power, PowerBlocks.solarpanel, stack(Items.iron, 30), stack(Items.silicon, 20)),
        new Recipe(power, PowerBlocks.largesolarpanel, stack(Items.iron, 30), stack(Items.silicon, 20)),
		new Recipe(power, PowerBlocks.nuclearReactor, stack(Items.titanium, 40), stack(Items.densealloy, 40), stack(Items.steel, 50)),
		new Recipe(power, PowerBlocks.fusionReactor, stack(Items.titanium, 40), stack(Items.densealloy, 40), stack(Items.steel, 50)),

		new Recipe(power, PowerBlocks.shieldgenerator, stack(Items.titanium, 30), stack(Items.densealloy, 30)),

		new Recipe(distribution, PowerBlocks.teleporter, stack(Items.steel, 30), stack(Items.densealloy, 40)),

		new Recipe(power, PowerBlocks.repairturret, stack(Items.iron, 30)),
		new Recipe(power, PowerBlocks.megarepairturret, stack(Items.iron, 20), stack(Items.steel, 30)),

		new Recipe(liquid, LiquidBlocks.conduit, stack(Items.steel, 1)),
		new Recipe(liquid, LiquidBlocks.pulseconduit, stack(Items.titanium, 1), stack(Items.steel, 1)),
		new Recipe(liquid, LiquidBlocks.liquidrouter, stack(Items.steel, 2)),
		new Recipe(liquid, LiquidBlocks.liquidtank, stack(Items.steel, 2)),
		new Recipe(liquid, LiquidBlocks.liquidjunction, stack(Items.steel, 2)),
		new Recipe(liquid, LiquidBlocks.conduittunnel, stack(Items.titanium, 2), stack(Items.steel, 2)),
		new Recipe(liquid, LiquidBlocks.liquidbridge, stack(Items.titanium, 2), stack(Items.steel, 2)),

		new Recipe(liquid, LiquidBlocks.pump, stack(Items.steel, 10)),
		new Recipe(liquid, LiquidBlocks.fluxpump, stack(Items.steel, 10), stack(Items.densealloy, 5)),

		new Recipe(units, UnitBlocks.flierFactory, stack(Items.steel, 10)),
		new Recipe(units, UnitBlocks.walkerFactory, stack(Items.steel, 10)),

		new Recipe(units, DebugBlocks.itemSource, stack(Items.steel, 10)).setDebug(),
		new Recipe(units, DebugBlocks.itemVoid, stack(Items.steel, 10)).setDebug(),
		new Recipe(units, DebugBlocks.liquidSource, stack(Items.steel, 10)).setDebug(),
		new Recipe(units, DebugBlocks.powerVoid, stack(Items.steel, 10)).setDebug(),
		new Recipe(units, DebugBlocks.powerInfinite, stack(Items.steel, 10), stack(Items.densealloy, 5)).setDebug()
	);
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}

	public static Array<Recipe> all(){
		return list;
	}

	public static Recipe getByResult(Block block){
		for(Recipe recipe : list){
			if(recipe.result == block){
				return recipe;
			}
		}
		return null;
	}
	
	public static Array<Recipe> getBy(Section section, Array<Recipe> r){
		for(Recipe recipe : list){
			if(recipe.section == section && !(Vars.android && recipe.desktopOnly) && !(!debug && recipe.debugOnly)) {
				r.add(recipe);
			}
		}
		
		return r;
	}
}
