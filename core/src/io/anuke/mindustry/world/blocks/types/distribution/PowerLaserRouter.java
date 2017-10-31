package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;

public class PowerLaserRouter extends PowerLaser{

	public PowerLaserRouter(String name) {
		super(name);
	}
	
	@Override
	public void drawOver(Tile tile){
		
		PowerEntity entity = tile.entity();
		
		if(entity.power > powerAmount){
			for(int i = -1; i <= 1; i ++){
				drawLaserTo(tile, tile.rotation + i);
			}
		}
	}
	
	@Override
	public void update(Tile tile){
		PowerEntity entity = tile.entity();
		for(int i = -1; i <= 1; i ++){
			Tile target = target(tile, tile.rotation + i);
		
			if(target == null) return;
		
			PowerAcceptor p = (PowerAcceptor)target.block();
			if(p.acceptsPower(target) && entity.power >= powerAmount/3f){
				entity.power -= (powerAmount/3f - p.addPower(target, powerAmount/3f));
			}
		}
	}

}
