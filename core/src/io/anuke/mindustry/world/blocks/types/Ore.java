package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.blocks.Blocks;

public class Ore extends Floor{

	public Ore(String name) {
		super(name);
		blends = block -> block != this && block != Blocks.stone;
	}

}
