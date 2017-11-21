package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.Generator;

public class PowerLaser extends Generator{
	public float powerAmount = 0.03f;
	public Color color = Color.valueOf("e54135");

	public PowerLaser(String name) {
		super(name);
		rotate = true;
		solid = true;
		explosive = false;
		laserDirections = 1;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Laser Range: " + laserRange + " tiles");
	}
	
	@Override
	public void update(Tile tile){
		distributeLaserPower(tile);
	}
	
	@Override
	public boolean acceptsPower(Tile tile){
		PowerEntity entity = tile.entity();
		
		return entity.power + 0.001f <= powerCapacity;
	}
}
