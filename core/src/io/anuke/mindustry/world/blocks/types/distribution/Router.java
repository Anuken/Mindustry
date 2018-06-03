package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.type.Item;
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
		autoSleep = true;
	}
	
	@Override
	public void update(Tile tile){
		int iterations = Math.max(1, (int) (Timers.delta() + 0.4f));
		boolean moved = false;

		for(int i = 0; i < iterations; i ++) {
			if (tile.entity.items.totalItems() > 0) {
				tryDump(tile);
				moved = true;
			}
		}

		if(!moved){
			tile.entity.sleep();
		}
	}

	@Override
	public boolean canDump(Tile tile, Tile to, Item item) {
        return !(to.block() instanceof Router) || ((float) to.target().entity.items.totalItems() / to.target().block().itemCapacity) < ((float) tile.entity.items.totalItems() / to.target().block().itemCapacity);
    }

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		super.handleItem(item, tile, source);
		tile.entity.wakeUp();
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		int items = tile.entity.items.totalItems();
		return items < itemCapacity;
	}

}
