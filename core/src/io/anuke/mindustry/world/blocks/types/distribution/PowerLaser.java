package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.production.Generator;

public class PowerLaser extends Generator{
	public Color color = Color.valueOf("e54135");

	public PowerLaser(String name) {
		super(name);
		rotate = true;
		solid = true;
		explosive = false;
		laserDirections = 1;
		health = 50;
	}

	@Override
	public boolean canReplace(Block other) {
		return other instanceof  PowerLaser && other != this;
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
