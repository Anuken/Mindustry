package io.anuke.mindustry.resource;

import static io.anuke.mindustry.resource.Section.*;

import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;

public enum Recipe{
	stonewall(distribution, Blocks.stonewall, stack(Item.stone, 2)),
	ironwall(distribution, Blocks.ironwall, stack(Item.iron, 2)),
	steelwall(distribution, Blocks.steelwall, stack(Item.steel, 2)),
	titaniumwall(distribution, Blocks.titaniumwall, stack(Item.titanium, 2)),
	duriumwall(distribution, Blocks.diriumwall, stack(Item.dirium, 2)),
	compositewall(distribution, Blocks.compositewall, stack(Item.dirium, 2), stack(Item.titanium, 2), stack(Item.steel, 2), stack(Item.iron, 2)),
	conveyor(distribution, ProductionBlocks.conveyor, stack(Item.stone, 1)),
	fastconveyor(distribution, ProductionBlocks.steelconveyor, stack(Item.steel, 1)),
	router(distribution, ProductionBlocks.router, stack(Item.stone, 2)),
	junction(distribution, ProductionBlocks.junction, stack(Item.iron, 2)),
	
	turret(defense, WeaponBlocks.turret, stack(Item.stone, 4)),
	dturret(defense, WeaponBlocks.doubleturret, stack(Item.stone, 7)),
	machineturret(defense, WeaponBlocks.machineturret, stack(Item.iron, 8), stack(Item.stone, 10)),
	shotgunturret(defense, WeaponBlocks.shotgunturret, stack(Item.iron, 10), stack(Item.stone, 10)),
	flameturret(defense, WeaponBlocks.flameturret, stack(Item.iron, 12), stack(Item.steel, 9)),
	sniperturret(defense, WeaponBlocks.sniperturret, stack(Item.iron, 15), stack(Item.steel, 10)),
	laserturret(defense, WeaponBlocks.laserturret, stack(Item.steel, 10), stack(Item.titanium, 10)),
	mortarturret(defense, WeaponBlocks.mortarturret, stack(Item.steel, 20), stack(Item.titanium, 15)),
	teslaturret(defense, WeaponBlocks.teslaturret, stack(Item.steel, 10), stack(Item.titanium, 15), stack(Item.dirium, 15)),
	plasmaturret(defense, WeaponBlocks.plasmaturret, stack(Item.steel, 10), stack(Item.titanium, 10), stack(Item.dirium, 15)),
	
	healturret(defense, WeaponBlocks.repairturret, stack(Item.iron, 30)),
	megahealturret(defense, WeaponBlocks.megarepairturret, stack(Item.iron, 20), stack(Item.steel, 30)),
	
	drill(production, ProductionBlocks.stonedrill, stack(Item.stone, 16)),
	irondrill(production, ProductionBlocks.irondrill, stack(Item.stone, 25)),
	coaldrill(production, ProductionBlocks.coaldrill, stack(Item.stone, 25), stack(Item.iron, 40)),
	titaniumdrill(production, ProductionBlocks.titaniumdrill, stack(Item.iron, 40), stack(Item.steel, 40)),
	smelter(production, ProductionBlocks.smelter, stack(Item.stone, 40), stack(Item.iron, 40)),
	crucible(production, ProductionBlocks.crucible, stack(Item.titanium, 40), stack(Item.steel, 40)),
	coalpurifier(production, ProductionBlocks.coalpurifier, stack(Item.steel, 10), stack(Item.iron, 10)),
	titaniumpurifier(production, ProductionBlocks.titaniumpurifier, stack(Item.steel, 30), stack(Item.iron, 30)),
	omnidrill(production, ProductionBlocks.omnidrill, stack(Item.titanium, 10), stack(Item.dirium, 10)),
	
	conduit(distribution, ProductionBlocks.conduit, stack(Item.steel, 1)),
	liquidrouter(distribution, ProductionBlocks.liquidrouter, stack(Item.steel, 2)),
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
