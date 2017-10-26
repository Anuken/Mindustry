package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.Generator;

public class PowerBooster extends Generator{
	
	//TODO
	public PowerBooster(String name) {
		super(name);
	}
	
	@Override
	public void update(Tile tile){
		distributePower(tile);
	}
	
	@Override
	public float addPower(Tile tile, float amount){
		PowerEntity entity = tile.entity();
		
		float canAccept = Math.min(powerCapacity - entity.power, amount);
		entity.power += canAccept;
		return canAccept;
	}
}
