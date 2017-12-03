package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;

public class Wall extends Block{

	public Wall(String name) {
		super(name);
		solid = true;
		destructible = true;
	}
	
	public boolean canReplace(Block other){
		return other instanceof Wall && health > other.health;
	}

}
