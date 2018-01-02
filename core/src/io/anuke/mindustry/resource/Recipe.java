package io.anuke.mindustry.resource;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.DefenseBlocks;
import io.anuke.mindustry.world.blocks.DistributionBlocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;

import static io.anuke.mindustry.resource.Section.*;

public enum Recipe{
	stonewall(defense, DefenseBlocks.stonewall, stack(Item.stone, 2)),
	ironwall(defense, DefenseBlocks.ironwall, stack(Item.iron, 2)),
	steelwall(defense, DefenseBlocks.steelwall, stack(Item.steel, 2)),
	titaniumwall(defense, DefenseBlocks.titaniumwall, stack(Item.titanium, 2)),
	duriumwall(defense, DefenseBlocks.diriumwall, stack(Item.dirium, 2)),
	//compositewall(defense, DefenseBlocks.compositewall, stack(Item.dirium, 2), stack(Item.titanium, 2), stack(Item.steel, 2), stack(Item.iron, 2)),
	steelwalllarge(defense, DefenseBlocks.steelwalllarge, stack(Item.steel, 8)),
	titaniumwalllarge(defense, DefenseBlocks.titaniumwalllarge, stack(Item.titanium, 8)),
	duriumwalllarge(defense, DefenseBlocks.diriumwalllarge, stack(Item.dirium, 8)),
	door(defense, DefenseBlocks.door, stack(Item.steel, 3), stack(Item.iron, 3)),
	largedoor(defense, DefenseBlocks.largedoor, stack(Item.steel, 3*4), stack(Item.iron, 3*4)),
	titaniumshieldwall(defense, DefenseBlocks.titaniumshieldwall, stack(Item.titanium, 3)),
	
	conveyor(distribution, DistributionBlocks.conveyor, stack(Item.stone, 1)),
	steelconveyor(distribution, DistributionBlocks.steelconveyor, stack(Item.steel, 1)),
	poweredconveyor(distribution, DistributionBlocks.pulseconveyor, stack(Item.dirium, 1)),
	router(distribution, DistributionBlocks.router, stack(Item.stone, 2)),
	junction(distribution, DistributionBlocks.junction, stack(Item.iron, 2)),
	tunnel(distribution, DistributionBlocks.tunnel, stack(Item.iron, 2)),
	conduit(distribution, DistributionBlocks.conduit, stack(Item.steel, 1)),
	liquidtunnel(distribution, DistributionBlocks.luquidtunel, stack(Item.steel, 2)),
	pulseconduit(distribution, DistributionBlocks.pulseconduit, stack(Item.titanium, 1), stack(Item.steel, 1)),
	liquidrouter(distribution, DistributionBlocks.liquidrouter, stack(Item.steel, 2)),
	liquidjunction(distribution, DistributionBlocks.liquidjunction, stack(Item.steel, 2)),
	sorter(distribution, DistributionBlocks.sorter, stack(Item.steel, 2)),
	
	turret(weapon, WeaponBlocks.turret, stack(Item.stone, 4)),
	dturret(weapon, WeaponBlocks.doubleturret, stack(Item.stone, 7)),
	machineturret(weapon, WeaponBlocks.machineturret, stack(Item.iron, 8), stack(Item.stone, 10)),
	shotgunturret(weapon, WeaponBlocks.shotgunturret, stack(Item.iron, 10), stack(Item.stone, 10)),
	flameturret(weapon, WeaponBlocks.flameturret, stack(Item.iron, 12), stack(Item.steel, 9)),
	sniperturret(weapon, WeaponBlocks.sniperturret, stack(Item.iron, 15), stack(Item.steel, 10)),
	laserturret(weapon, WeaponBlocks.laserturret, stack(Item.steel, 10), stack(Item.titanium, 10)),
	mortarturret(weapon, WeaponBlocks.mortarturret, stack(Item.steel, 20), stack(Item.titanium, 15)),
	teslaturret(weapon, WeaponBlocks.teslaturret, stack(Item.steel, 10), stack(Item.titanium, 15), stack(Item.dirium, 15)),
	plasmaturret(weapon, WeaponBlocks.plasmaturret, stack(Item.steel, 10), stack(Item.titanium, 10), stack(Item.dirium, 15)),
	chainturret(weapon, WeaponBlocks.chainturret, stack(Item.steel, 50), stack(Item.titanium, 25), stack(Item.dirium, 35)),
	titanturret(weapon, WeaponBlocks.titanturret, stack(Item.steel, 70), stack(Item.titanium, 50), stack(Item.dirium, 55)),
	
	smelter(crafting, ProductionBlocks.smelter, stack(Item.stone, 40), stack(Item.iron, 40)),
	crucible(crafting, ProductionBlocks.crucible, stack(Item.titanium, 40), stack(Item.steel, 40)),
	coalpurifier(crafting, ProductionBlocks.coalpurifier, stack(Item.steel, 10), stack(Item.iron, 10)),
	titaniumpurifier(crafting, ProductionBlocks.titaniumpurifier, stack(Item.steel, 30), stack(Item.iron, 30)),
	oilrefinery(crafting, ProductionBlocks.oilrefinery, stack(Item.steel, 15), stack(Item.iron, 15)),
	stoneformer(crafting, ProductionBlocks.stoneformer, stack(Item.steel, 10), stack(Item.iron, 10)),
	lavasmelter(crafting, ProductionBlocks.lavasmelter, stack(Item.steel, 30), stack(Item.titanium, 15)),
	
	stonedrill(production, ProductionBlocks.stonedrill, stack(Item.stone, 12)),
	irondrill(production, ProductionBlocks.irondrill, stack(Item.stone, 25)),
	coaldrill(production, ProductionBlocks.coaldrill, stack(Item.stone, 25), stack(Item.iron, 40)),
	titaniumdrill(production, ProductionBlocks.titaniumdrill, stack(Item.iron, 40), stack(Item.steel, 40)),
	uraniumdrill(production, ProductionBlocks.uraniumdrill, stack(Item.iron, 40), stack(Item.steel, 40)),
	omnidrill(production, ProductionBlocks.omnidrill, stack(Item.titanium, 30), stack(Item.dirium, 20)),
	
	coalgenerator(power, ProductionBlocks.coalgenerator, stack(Item.iron, 30), stack(Item.stone, 20)),
	thermalgenerator(power, ProductionBlocks.thermalgenerator, stack(Item.steel, 30), stack(Item.iron, 30)),
	combustiongenerator(power, ProductionBlocks.combustiongenerator, stack(Item.iron, 30), stack(Item.stone, 20)),
	rtgenerator(power, ProductionBlocks.rtgenerator, stack(Item.titanium, 20), stack(Item.steel, 20)),
	nuclearreactor(power, ProductionBlocks.nuclearReactor, stack(Item.titanium, 40), stack(Item.dirium, 40), stack(Item.steel, 50)),
	powerbooster(power, DistributionBlocks.powerbooster, stack(Item.steel, 8), stack(Item.iron, 8)),
	powerlaser(power, DistributionBlocks.powerlaser, stack(Item.steel, 3), stack(Item.iron, 3)),
	powerlasercorner(power, DistributionBlocks.powerlasercorner, stack(Item.steel, 4), stack(Item.iron, 4)),
	powerlaserrouter(power, DistributionBlocks.powerlaserrouter, stack(Item.steel, 5), stack(Item.iron, 5)),
	
	shieldgenerator(power, DefenseBlocks.shieldgenerator, stack(Item.titanium, 30), stack(Item.dirium, 40)),
	
	teleporter(distribution, DistributionBlocks.teleporter, stack(Item.steel, 20), stack(Item.dirium, 15)),
	
	healturret(power, DefenseBlocks.repairturret, stack(Item.iron, 30)),
	megahealturret(power, DefenseBlocks.megarepairturret, stack(Item.iron, 20), stack(Item.steel, 30)),
	
	pump(production, ProductionBlocks.pump, stack(Item.steel, 10)),
	fluxpump(production, ProductionBlocks.fluxpump, stack(Item.steel, 10), stack(Item.dirium, 5));
	
	public Block result;
	public ItemStack[] requirements;
	public Section section;
	
	private Recipe(Section section, Block result, ItemStack... requirements){
		this.result = result;
		this.requirements = requirements;
		this.section = section;
	}
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}

	public static Recipe getByResult(Block block){
		for(Recipe recipe : Recipe.values()){
			if(recipe.result == block){
				return recipe;
			}
		}
		return null;
	}
	
	public static Array<Recipe> getBy(Section section, Array<Recipe> r){
		for(Recipe recipe : Recipe.values()){
			if(recipe.section == section)
			r.add(recipe);
		}
		
		return r;
	}
}
