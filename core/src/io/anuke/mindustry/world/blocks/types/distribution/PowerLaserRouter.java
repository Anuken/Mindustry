package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.world.Tile;

public class PowerLaserRouter extends PowerLaser{

	public PowerLaserRouter(String name) {
		super(name);
		laserDirections = 3;
	}
	
	@Override
	public void update(Tile tile){
		distributeLaserPower(tile);
	}

}
