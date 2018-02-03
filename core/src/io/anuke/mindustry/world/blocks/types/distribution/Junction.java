package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Bits;

public class Junction extends Block{
	float speed = 20; //frames taken to go through this junction

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
		JunctionEntity entity = tile.entity();
		entity.items[entity.index ++] = Bits.packInt((short)item.id, source.relativeTo(tile.x, tile.y));
	}

	@Override
	public void update(Tile tile){
		JunctionEntity entity = tile.entity();

		if(entity.index > 0){
			entity.time += Timers.delta();
			if(entity.time >= speed){
				int i = entity.items[-- entity.index];

				int item = Bits.getLeftShort(i);
				int direction = Bits.getRightShort(i);

				Tile target = tile.getNearby(direction);

				target.block().handleItem(Item.getByID(item), target, tile);

				entity.time = 0f;
			}
		}else{
			entity.time = 0f;
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		JunctionEntity entity = tile.entity();

		if(entity.index >= entity.items.length - 1) return false;
		int dir = source.relativeTo(tile.x, tile.y);
		if(dir == -1) return false;
		Tile to = tile.getNearby(dir);
		return to != null && to.block().acceptItem(item, to, tile);
	}

	@Override
	public TileEntity getEntity() {
		return new JunctionEntity();
	}

	class JunctionEntity extends TileEntity{
		int[] items = new int[16]; //16 item buffer
		int index;
		float time;
	}
}
