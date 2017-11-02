package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.Generator;

public class PowerBooster extends Generator{
	
	public PowerBooster(String name) {
		super(name);
		drawRadius = true;
		explosive = false;
	}
	
	@Override
	public void update(Tile tile){
		distributePower(tile);
	}
	
	@Override
	public void drawOver(Tile tile){}
	
	@Override
	public boolean acceptsPower(Tile tile){
		PowerEntity entity = tile.entity();
		
		return entity.power + 0.001f <= powerCapacity;
	}
}
