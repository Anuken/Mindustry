package io.anuke.mindustry.resource;

import static io.anuke.mindustry.resource.Section.*;

import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.ProductionBlocks;
import io.anuke.mindustry.world.blocks.WeaponBlocks;

public enum Recipe{
	stonewall(distribution, Blocks.stonewall, stack(Item.stone, 5)),
	ironwall(distribution, Blocks.ironwall, stack(Item.iron, 5)),
	steelwall(distribution, Blocks.steelwall, stack(Item.steel, 5)),
	titaniumwall(distribution, Blocks.titaniumwall, stack(Item.titanium, 5)),
	duriumwall(distribution, Blocks.diriumwall, stack(Item.dirium, 5)),
	compositewall(distribution, Blocks.compositewall, stack(Item.dirium, 5), stack(Item.titanium, 5), stack(Item.steel, 5), stack(Item.iron, 5)),
	conveyor(distribution, ProductionBlocks.conveyor, stack(Item.stone, 1)),
	fastconveyor(distribution, ProductionBlocks.steelconveyor, stack(Item.steel, 1)),
	router(distribution, ProductionBlocks.router, stack(Item.stone, 3)),
	junction(distribution, ProductionBlocks.junction, stack(Item.iron, 5)),
	
	turret(defense, WeaponBlocks.turret, stack(Item.stone, 6)),
	dturret(defense, WeaponBlocks.doubleturret, stack(Item.stone, 12)),
	machineturret(defense, WeaponBlocks.machineturret, stack(Item.iron, 15), stack(Item.stone, 20)),
	shotgunturret(defense, WeaponBlocks.shotgunturret, stack(Item.iron, 18), stack(Item.stone, 20)),
	flameturret(defense, WeaponBlocks.flameturret, stack(Item.iron, 25), stack(Item.steel, 18)),
	sniperturret(defense, WeaponBlocks.sniperturret, stack(Item.iron, 30), stack(Item.steel, 20)),
	laserturret(defense, WeaponBlocks.laserturret, stack(Item.steel, 20), stack(Item.titanium, 20)),
	mortarturret(defense, WeaponBlocks.mortarturret, stack(Item.steel, 40), stack(Item.titanium, 30)),
	teslaturret(defense, WeaponBlocks.teslaturret, stack(Item.steel, 20), stack(Item.titanium, 30), stack(Item.dirium, 30)),
	plasmaturret(defense, WeaponBlocks.plasmaturret, stack(Item.steel, 20), stack(Item.titanium, 20), stack(Item.dirium, 30)),
	
	healturret(defense, WeaponBlocks.repairturret, stack(Item.iron, 50)),
	megahealturret(defense, WeaponBlocks.megarepairturret, stack(Item.iron, 30), stack(Item.steel, 50)),
	
	drill(production, ProductionBlocks.stonedrill, stack(Item.stone, 6)),
	irondrill(production, ProductionBlocks.irondrill, stack(Item.stone, 40)),
	coaldrill(production, ProductionBlocks.coaldrill, stack(Item.stone, 40), stack(Item.iron, 40)),
	titaniumdrill(production, ProductionBlocks.titaniumdrill, stack(Item.iron, 40), stack(Item.steel, 40)),
	omnidrill(production, ProductionBlocks.omnidrill, stack(Item.titanium, 40), stack(Item.dirium, 40)),
	smelter(production, ProductionBlocks.smelter, stack(Item.stone, 80), stack(Item.iron, 80)),
	crucible(production, ProductionBlocks.crucible, stack(Item.titanium, 80), stack(Item.steel, 80)),
	coalpurifier(production, ProductionBlocks.coalpurifier, stack(Item.steel, 20), stack(Item.iron, 20)),
	
	conduit(distribution, ProductionBlocks.conduit, stack(Item.steel, 1)),
	liquidrouter(distribution, ProductionBlocks.liquidrouter, stack(Item.steel, 5)),
	pump(production, ProductionBlocks.pump, stack(Item.steel, 20));
	
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
