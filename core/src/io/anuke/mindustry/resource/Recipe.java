package io.anuke.mindustry.resource;

import static io.anuke.mindustry.resource.Section.*;

import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.*;

public enum Recipe{
	stonewall(defense, DefenseBlocks.stonewall, stack(Item.stone, 2)),
	ironwall(defense, DefenseBlocks.ironwall, stack(Item.iron, 2)),
	steelwall(defense, DefenseBlocks.steelwall, stack(Item.steel, 2)),
	titaniumwall(defense, DefenseBlocks.titaniumwall, stack(Item.titanium, 2)),
	duriumwall(defense, DefenseBlocks.diriumwall, stack(Item.dirium, 2)),
	compositewall(defense, DefenseBlocks.compositewall, stack(Item.dirium, 2), stack(Item.titanium, 2), stack(Item.steel, 2), stack(Item.iron, 2)),
	healturret(defense, DefenseBlocks.repairturret, stack(Item.iron, 30)),
	megahealturret(defense, DefenseBlocks.megarepairturret, stack(Item.iron, 20), stack(Item.steel, 30)),
	titaniumshieldwall(defense, DefenseBlocks.titaniumshieldwall, stack(Item.titanium, 2)),
	
	conveyor(distribution, DistributionBlocks.conveyor, stack(Item.stone, 1)),
	steelconveyor(distribution, DistributionBlocks.steelconveyor, stack(Item.steel, 1)),
	poweredconveyor(distribution, DistributionBlocks.poweredconveyor, stack(Item.dirium, 1)),
	router(distribution, DistributionBlocks.router, stack(Item.stone, 2)),
	junction(distribution, DistributionBlocks.junction, stack(Item.iron, 2)),
	conduit(distribution, DistributionBlocks.conduit, stack(Item.steel, 1)),
	liquidrouter(distribution, DistributionBlocks.liquidrouter, stack(Item.steel, 2)),
	liquidjunction(distribution, DistributionBlocks.liquidjunction, stack(Item.steel, 2)),
	
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
	
	smelter(crafting, ProductionBlocks.smelter, stack(Item.stone, 40), stack(Item.iron, 40)),
	crucible(crafting, ProductionBlocks.crucible, stack(Item.titanium, 40), stack(Item.steel, 40)),
	coalpurifier(crafting, ProductionBlocks.coalpurifier, stack(Item.steel, 10), stack(Item.iron, 10)),
	titaniumpurifier(crafting, ProductionBlocks.titaniumpurifier, stack(Item.steel, 30), stack(Item.iron, 30)),
	oilrefinery(crafting, ProductionBlocks.oilrefinery, stack(Item.steel, 30), stack(Item.iron, 30)),
	
	stonedrill(production, ProductionBlocks.stonedrill, stack(Item.stone, 12)),
	irondrill(production, ProductionBlocks.irondrill, stack(Item.stone, 25)),
	coaldrill(production, ProductionBlocks.coaldrill, stack(Item.stone, 25), stack(Item.iron, 40)),
	titaniumdrill(production, ProductionBlocks.titaniumdrill, stack(Item.iron, 40), stack(Item.steel, 40)),
	uraniumdrill(production, ProductionBlocks.uraniumdrill, stack(Item.titanium, 20), stack(Item.steel, 40)),
	omnidrill(production, ProductionBlocks.omnidrill, stack(Item.titanium, 20), stack(Item.dirium, 20)),
	
	coalgenerator(power, ProductionBlocks.coalgenerator, stack(Item.titanium, 10), stack(Item.dirium, 10)),
	thermalgenerator(power, ProductionBlocks.thermalgenerator, stack(Item.titanium, 10), stack(Item.dirium, 10)),
	combustiongenerator(power, ProductionBlocks.combustiongenerator, stack(Item.titanium, 10), stack(Item.dirium, 10)),
	nuclearreactor(power, ProductionBlocks.nuclearReactor, stack(Item.titanium, 10), stack(Item.dirium, 10)),
	shieldgenerator(power, DefenseBlocks.shieldgenerator, stack(Item.titanium, 10), stack(Item.dirium, 10)),
	
	pump(production, ProductionBlocks.pump, stack(Item.steel, 10));
	
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
	
	public static Array<Recipe> getBy(Section section, Array<Recipe> r){
		for(Recipe recipe : Recipe.values()){
			if(recipe.section == section)
			r.add(recipe);
		}
		
		return r;
	}
}
