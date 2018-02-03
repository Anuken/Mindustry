package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Junction extends Block{

	public Junction(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Conveyor || other instanceof Router;
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		int dir = source.relativeTo(tile.x, tile.y);
		Tile to = tile.getNearby(temptiles)[dir];
		
		Timers.run(30, () -> {
			if(to == null) return;
			to.block().handleItem(item, to, tile);
		});
		
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		int dir = source.relativeTo(tile.x, tile.y);
		if(dir == -1) return false;
		Tile to = tile.getNearby(temptiles)[dir];
		return to != null && to.block().acceptItem(item, to, tile);
	}
	
}
