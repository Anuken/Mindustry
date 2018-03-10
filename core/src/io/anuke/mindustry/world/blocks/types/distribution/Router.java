package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Router extends Block{

	public Router(String name) {
		super(name);
		update = true;
		solid = true;
		itemCapacity = 20;
		group = BlockGroup.transportation;
	}
	
	@Override
	public void update(Tile tile){
		int iterations = Math.max(1, (int) (Timers.delta() + 0.4f));

		for(int i = 0; i < iterations; i ++) {
			if (tile.entity.inventory.totalItems() > 0) {
				tryDump(tile);
			}
		}
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		super.handleItem(item, tile, source);
		tile.setExtra(tile.relativeTo(source.x, source.y));
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		int items = tile.entity.inventory.totalItems();
		return items < itemCapacity;
	}

}
