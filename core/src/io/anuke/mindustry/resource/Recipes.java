package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.DefenseBlocks;
import io.anuke.mindustry.world.blocks.DistributionBlocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;

import static io.anuke.mindustry.resource.Section.*;

public class Recipes {
	private static final Array<Recipe> list = Array.with(
			new Recipe(defense, DefenseBlocks.stonewall, stack(Item.stone, 12)),
			new Recipe(defense, DefenseBlocks.ironwall, stack(Item.iron, 12)),
			new Recipe(defense, DefenseBlocks.steelwall, stack(Item.steel, 12)),
			new Recipe(defense, DefenseBlocks.titaniumwall, stack(Item.titanium, 12)),
			new Recipe(defense, DefenseBlocks.diriumwall, stack(Item.dirium, 12)),
			new Recipe(defense, DefenseBlocks.steelwalllarge, stack(Item.steel, 12*4)),
			new Recipe(defense, DefenseBlocks.titaniumwalllarge, stack(Item.titanium, 12*4)),
			new Recipe(defense, DefenseBlocks.diriumwalllarge, stack(Item.dirium, 12*4)),
			new Recipe(defense, DefenseBlocks.door, stack(Item.steel, 3), stack(Item.iron, 3*4)).setDesktop(),
			new Recipe(defense, DefenseBlocks.largedoor, stack(Item.steel, 3*4), stack(Item.iron, 3*4*4)).setDesktop(),
			new Recipe(defense, DefenseBlocks.titaniumshieldwall, stack(Item.titanium, 16)),

			new Recipe(distribution, DistributionBlocks.conveyor, stack(Item.stone, 1)),
			new Recipe(distribution, DistributionBlocks.steelconveyor, stack(Item.steel, 1)),
			new Recipe(distribution, DistributionBlocks.pulseconveyor, stack(Item.dirium, 1)),
			new Recipe(distribution, DistributionBlocks.router, stack(Item.stone, 2)),
			new Recipe(distribution, DistributionBlocks.junction, stack(Item.iron, 2)),
			new Recipe(distribution, DistributionBlocks.tunnel, stack(Item.iron, 2)),
			new Recipe(distribution, DistributionBlocks.conduit, stack(Item.steel, 1)),
			new Recipe(distribution, DistributionBlocks.pulseconduit, stack(Item.titanium, 1), stack(Item.steel, 1)),
			new Recipe(distribution, DistributionBlocks.liquidrouter, stack(Item.steel, 2)),
			new Recipe(distribution, DistributionBlocks.liquidjunction, stack(Item.steel, 2)),
			new Recipe(distribution, DistributionBlocks.sorter, stack(Item.steel, 2)),

			new Recipe(weapon, WeaponBlocks.turret, stack(Item.stone, 4)),
			new Recipe(weapon, WeaponBlocks.doubleturret, stack(Item.stone, 7)),
			new Recipe(weapon, WeaponBlocks.machineturret, stack(Item.iron, 8), stack(Item.stone, 10)),
			new Recipe(weapon, WeaponBlocks.shotgunturret, stack(Item.iron, 10), stack(Item.stone, 10)),
			new Recipe(weapon, WeaponBlocks.flameturret, stack(Item.iron, 12), stack(Item.steel, 9)),
			new Recipe(weapon, WeaponBlocks.sniperturret, stack(Item.iron, 15), stack(Item.steel, 10)),
			new Recipe(weapon, WeaponBlocks.laserturret, stack(Item.steel, 12), stack(Item.titanium, 12)),
			new Recipe(weapon, WeaponBlocks.mortarturret, stack(Item.steel, 25), stack(Item.titanium, 15)),
			new Recipe(weapon, WeaponBlocks.teslaturret, stack(Item.steel, 20), stack(Item.titanium, 25), stack(Item.dirium, 15)),
			new Recipe(weapon, WeaponBlocks.plasmaturret, stack(Item.steel, 10), stack(Item.titanium, 20), stack(Item.dirium, 15)),
			new Recipe(weapon, WeaponBlocks.chainturret, stack(Item.steel, 50), stack(Item.titanium, 25), stack(Item.dirium, 40)),
			new Recipe(weapon, WeaponBlocks.titanturret, stack(Item.steel, 70), stack(Item.titanium, 50), stack(Item.dirium, 55)),

			new Recipe(crafting, ProductionBlocks.smelter, stack(Item.stone, 40), stack(Item.iron, 40)),
			new Recipe(crafting, ProductionBlocks.crucible, stack(Item.titanium, 50), stack(Item.steel, 50)),
			new Recipe(crafting, ProductionBlocks.coalpurifier, stack(Item.steel, 10), stack(Item.iron, 10)),
			new Recipe(crafting, ProductionBlocks.titaniumpurifier, stack(Item.steel, 30), stack(Item.iron, 30)),
			new Recipe(crafting, ProductionBlocks.oilrefinery, stack(Item.steel, 15), stack(Item.iron, 15)),
			new Recipe(crafting, ProductionBlocks.stoneformer, stack(Item.steel, 10), stack(Item.iron, 10)),
			new Recipe(crafting, ProductionBlocks.lavasmelter, stack(Item.steel, 30), stack(Item.titanium, 15)),
			new Recipe(crafting, ProductionBlocks.weaponFactory, stack(Item.steel, 60), stack(Item.iron, 60)).setDesktop(),

			new Recipe(production, ProductionBlocks.stonedrill, stack(Item.stone, 12)),
			new Recipe(production, ProductionBlocks.irondrill, stack(Item.stone, 25)),
			new Recipe(production, ProductionBlocks.coaldrill, stack(Item.stone, 25), stack(Item.iron, 40)),
			new Recipe(production, ProductionBlocks.titaniumdrill, stack(Item.iron, 50), stack(Item.steel, 50)),
			new Recipe(production, ProductionBlocks.uraniumdrill, stack(Item.iron, 40), stack(Item.steel, 40)),
			new Recipe(production, ProductionBlocks.omnidrill, stack(Item.titanium, 40), stack(Item.dirium, 40)),

			new Recipe(power, ProductionBlocks.coalgenerator, stack(Item.iron, 30), stack(Item.stone, 20)),
			new Recipe(power, ProductionBlocks.thermalgenerator, stack(Item.steel, 30), stack(Item.iron, 30)),
			new Recipe(power, ProductionBlocks.combustiongenerator, stack(Item.iron, 30), stack(Item.stone, 20)),
			new Recipe(power, ProductionBlocks.rtgenerator, stack(Item.titanium, 20), stack(Item.steel, 20)),
			new Recipe(power, ProductionBlocks.nuclearReactor, stack(Item.titanium, 40), stack(Item.dirium, 40), stack(Item.steel, 50)),
			new Recipe(power, DistributionBlocks.powerbooster, stack(Item.steel, 8), stack(Item.iron, 8)),
			new Recipe(power, DistributionBlocks.powerlaser, stack(Item.steel, 3), stack(Item.iron, 3)),
			new Recipe(power, DistributionBlocks.powerlasercorner, stack(Item.steel, 4), stack(Item.iron, 4)),
			new Recipe(power, DistributionBlocks.powerlaserrouter, stack(Item.steel, 5), stack(Item.iron, 5)),

			new Recipe(power, DefenseBlocks.shieldgenerator, stack(Item.titanium, 30), stack(Item.dirium, 30)),

			new Recipe(distribution, DistributionBlocks.teleporter, stack(Item.steel, 30), stack(Item.dirium, 40)),

			new Recipe(power, DefenseBlocks.repairturret, stack(Item.iron, 30)),
			new Recipe(power, DefenseBlocks.megarepairturret, stack(Item.iron, 20), stack(Item.steel, 30)),

			new Recipe(production, ProductionBlocks.pump, stack(Item.steel, 10)),
			new Recipe(production, ProductionBlocks.fluxpump, stack(Item.steel, 10), stack(Item.dirium, 5))
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
			if(recipe.section == section && !(Vars.mobile && recipe.desktopOnly))
				r.add(recipe);
		}
		
		return r;
	}
}
