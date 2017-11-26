package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.function.Predicate;
import io.anuke.ucore.util.Mathf;

public class BlendBlock extends Block{
	protected String edge;
	protected Predicate<Block> blend = block -> block == this;

	public BlendBlock(String name) {
		super(name);
		edge = name + "-edge";
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(variants > 0 ? (name() + Mathf.randomSeed(tile.id(), 1, variants))  : name(), 
				tile.worldx(), tile.worldy());
		
		Tile[] nearby = tile.getNearby();
		
		for(int i = 0; i < 4; i ++){
			if(nearby[i] != null && !blend.test(nearby[i].block())){
				Draw.rect(edge + "-" + i, tile.worldx(), tile.worldy());
			}
		}
	}
}
