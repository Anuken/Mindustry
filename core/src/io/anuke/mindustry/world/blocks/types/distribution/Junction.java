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
	public void handleItem(Tile tile, Item item, Tile source){
		int dir = source.relativeTo(tile.x, tile.y);
		dir = (dir+4)%4;
		Tile to = tile.getNearby()[dir];
		
		Timers.run(15, ()->{
			if(to == null || to.entity == null) return;
			to.block().handleItem(to, item, tile);
		});
		
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int dir = source.relativeTo(dest.x, dest.y);
		dir = (dir+4)%4;
		Tile to = dest.getNearby()[dir];
		//uncomment the junction bit to disable giving items to other junctions
		return to != null /*&& to.block() != junction*/ && to.block().acceptItem(item, to, dest);
	}
	
	@Override
	public String description(){
		return "Serves as a conveyor junction.";
	}
	
}
