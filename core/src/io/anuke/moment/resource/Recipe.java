package io.anuke.moment.resource;

import static io.anuke.moment.resource.Section.*;

import com.badlogic.gdx.utils.Array;

import io.anuke.moment.world.TileType;

public enum Recipe{
	stonewall(distribution, TileType.stonewall, stack(Item.stone, 5)),
	ironwall(distribution, TileType.ironwall, stack(Item.iron, 5)),
	steelwall(distribution, TileType.steelwall, stack(Item.steel, 5)),
	conveyor(distribution, TileType.conveyor, stack(Item.stone, 1)),
	fastconveyor(distribution, TileType.steelconveyor, stack(Item.steel, 1)),
	router(distribution, TileType.router, stack(Item.stone, 3)),
	
	
	turret(defense, TileType.turret, stack(Item.stone, 8)),
	dturret(defense, TileType.doubleturret, stack(Item.stone, 12)),
	machineturret(defense, TileType.machineturret, stack(Item.iron, 15), stack(Item.stone, 20)),
	shotgunturret(defense, TileType.shotgunturret, stack(Item.iron, 15), stack(Item.steel, 24)),
	flameturret(defense, TileType.flameturret, stack(Item.iron, 25), stack(Item.steel, 18)),
	sniperturret(defense, TileType.sniperturret, stack(Item.iron, 30), stack(Item.steel, 20)),
	
	healturret(defense, TileType.healturret, stack(Item.iron, 40)),
	megahealturret(defense, TileType.megahealturret, stack(Item.iron, 30), stack(Item.steel, 40)),
	
	drill(production, TileType.stonedrill, stack(Item.stone, 6)),
	irondrill(production, TileType.irondrill, stack(Item.stone, 40)),
	coaldrill(production, TileType.coaldrill, stack(Item.stone, 40), stack(Item.iron, 40)),
	smelter(production, TileType.smelter, stack(Item.stone, 80), stack(Item.iron, 80));
	
	public TileType result;
	public ItemStack[] requirements;
	public Section section;
	
	private Recipe(Section section, TileType result, ItemStack... requirements){
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
