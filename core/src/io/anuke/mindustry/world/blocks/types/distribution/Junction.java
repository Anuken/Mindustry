package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Bits;

public class Junction extends Block{
	protected float speed = 20; //frames taken to go through this junction
	protected int capacity = 16;

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
		boolean x = tile.x == source.x;
		int value = Bits.packInt((short)item.id, source.relativeTo(tile.x, tile.y));
		if(x){
			entity.bx.add(value);
		}else{
			entity.by.add(value);
		}
	}

	@Override
	public void update(Tile tile){
		JunctionEntity entity = tile.entity();

		for(int i = 0; i < 2; i ++){
			Buffer buffer = (i == 0 ? entity.bx : entity.by);
			if(buffer.index > 0){
				buffer.time += Timers.delta();
				if(buffer.time >= speed){
					int val = buffer.items[buffer.index - 1];

					Item item = Item.getByID(Bits.getLeftShort(val));
					int direction = Bits.getRightShort(val);
					Tile dest = tile.getNearby(direction);

					if(dest == null || !dest.block().acceptItem(item, dest, tile)) continue;

					dest.block().handleItem(item, dest, tile);

					buffer.time = 0f;
					buffer.index --;
				}
			}else{
				buffer.time = 0f;
			}
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		JunctionEntity entity = tile.entity();
		boolean x = tile.x == source.x;

		if((x && entity.bx.full()) || (!x && entity.by.full())) return false;
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
		Buffer bx = new Buffer();
		Buffer by = new Buffer();
	}

	class Buffer{
		int[] items = new int[capacity];
		int index;
		float time;

		void add(int id){
			items[index ++] = id;
		}

		boolean full(){
			return index >= items.length - 1;
		}
	}
}
