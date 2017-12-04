package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class TunnelConveyor extends Block{

	protected TunnelConveyor(String name) {
		super(name);
		rotate = true;
		update = true;
		solid = true;
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Conveyor || other instanceof Router || other instanceof Junction;
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		int dir = source.relativeTo(tile.x, tile.y);
		Tile to = getOther(tile, dir, 3);
		Tile inter = getOther(tile, dir, 2);
		
		Timers.run(25, ()->{
			if(to == null || to.entity == null) return;
			to.block().handleItem(item, to, inter);
		});
		
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int dir = source.relativeTo(dest.x, dest.y);
		Tile to = getOther(dest, dir, 3);
		Tile inter = getOther(dest, dir, 2);
		return to != null && inter != null && source.getRotation() == (dest.getRotation() + 2)%4 && inter.block() instanceof TunnelConveyor 
				&& (inter.getRotation() + 2) % 4 == dest.getRotation() &&
				to.block().acceptItem(item, to, inter);
	}
	
	Tile getOther(Tile tile, int dir, int amount){
		for(int i = 0; i < amount; i ++){
			if(tile == null) return null;
			tile = tile.getNearby()[dir];
		}
		return tile;
	}
}
