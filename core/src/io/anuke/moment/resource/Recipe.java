package io.anuke.moment.resource;

import io.anuke.moment.world.TileType;

public enum Recipe{
	stonewall(TileType.stonewall, stack(Item.stone, 4)),
	ironwall(TileType.ironwall, stack(Item.iron, 4)),
	drill(TileType.stonedrill, stack(Item.stone, 5)),
	irondrill(TileType.irondrill, stack(Item.stone, 30)),
	coaldrill(TileType.coaldrill, stack(Item.stone, 30), stack(Item.iron, 30)),
	conveyor(TileType.conveyor, stack(Item.stone, 1)),
	router(TileType.router, stack(Item.stone, 3)),
	smelter(TileType.smelter, stack(Item.stone, 40), stack(Item.iron, 40)),
	healturret(TileType.healturret, stack(Item.iron, 20)),
	turret(TileType.turret, stack(Item.stone, 4)),
	dturret(TileType.doubleturret, stack(Item.stone, 6)),
	machineturret(TileType.machineturret, stack(Item.iron, 8), stack(Item.stone, 6));
	
	public TileType result;
	public ItemStack[] requirements;
	
	private Recipe(TileType result, ItemStack... requirements){
		this.result = result;
		this.requirements = requirements;
	}
	
	private static ItemStack stack(Item item, int amount){
		return new ItemStack(item, amount);
	}
}
