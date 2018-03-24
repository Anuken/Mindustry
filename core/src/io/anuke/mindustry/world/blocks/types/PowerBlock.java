package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;

public abstract class PowerBlock extends Block{
	
	public PowerBlock(String name) {
		super(name);
		update = true;
		solid = true;
		hasPower = true;
		group = BlockGroup.power;
	}
}
