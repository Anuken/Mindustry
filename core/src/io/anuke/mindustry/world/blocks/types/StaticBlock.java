package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.graphics.DrawLayer;
import io.anuke.mindustry.world.Block;

public class StaticBlock extends Block{

	public StaticBlock(String name) {
		super(name);
		drawLayer = DrawLayer.walls;
	}

}
