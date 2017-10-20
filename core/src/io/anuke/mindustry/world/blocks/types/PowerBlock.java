package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;

public abstract class PowerBlock extends Block{
	public float powerCapacity;
	public float power;
	
	public PowerBlock(String name) {
		super(name);
		update = true;
		solid = true;
	}

}
