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
		Tile to = tile.getNearby()[dir];
		
		Timers.run(15, ()->{
			if(to == null || to.entity == null) return;
			to.block().handleItem(item, to, tile);
		});
		
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int dir = source.relativeTo(dest.x, dest.y);
		if(dir == -1) return false;
		Tile to = dest.getNearby()[dir];
		return to != null && to.block().acceptItem(item, to, dest);
	}
	
}
