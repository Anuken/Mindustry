package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;

public class Wall extends Block{

	public Wall(String name) {
		super(name);
		solid = true;
		destructible = true;
		group = BlockGroup.walls;
		hasItems = false;
	}

	@Override
	public boolean canReplace(Block other){
		return super.canReplace(other) && health > other.health;
	}

}
