package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.NumberUtils;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Bits;

public class Junction extends Block{
	protected float speed = 26; //frames taken to go through this junction
	protected int capacity = 32;

	public Junction(String name) {
		super(name);
		update = true;
		solid = true;
		instantTransfer = true;
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Conveyor || other instanceof Router;
	}

	@Override
	public void update(Tile tile){
		JunctionEntity entity = tile.entity();

		for(int i = 0; i < 2; i ++){
			Buffer buffer = (i == 0 ? entity.bx : entity.by);

			if(buffer.index > 0){
				if(buffer.index > buffer.items.length) buffer.index = buffer.items.length;
				long l = buffer.items[0];
				float time = NumberUtils.intBitsToFloat(Bits.getLeftInt(l));

				if(Timers.time() >= time + speed || Timers.time() < time){

					int val = Bits.getRightInt(l);

					Item item = Item.getByID(Bits.getLeftShort(val));
					int direction = Bits.getRightShort(val);
					Tile dest = tile.getNearby(direction);

					if(dest == null || !dest.block().acceptItem(item, dest, tile)){
						if(buffer.index > 1){
							System.arraycopy(buffer.items, 1, buffer.items, 0, buffer.index - 1);
							buffer.index --;
						}
						continue;
					}

					dest.block().handleItem(item, dest, tile);
					System.arraycopy(buffer.items, 1, buffer.items, 0, buffer.index - 1);
					buffer.index --;
				}
			}
		}
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		JunctionEntity entity = tile.entity();
		boolean x = tile.x == source.x;
		long value = Bits.packLong(NumberUtils.floatToIntBits(Timers.time()), Bits.packInt((short)item.id, source.relativeTo(tile.x, tile.y)));
		if(x){
			entity.bx.add(value);
		}else {
			entity.by.add(value);
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		JunctionEntity entity = tile.entity();
		boolean x = tile.x == source.x;

		if(entity == null || entity.bx == null || entity.by == null || (x && entity.bx.full()) || (!x && entity.by.full())) return false;
		int dir = source.relativeTo(tile.x, tile.y);
		if(dir == -1) return false;
		Tile to = tile.getNearby(dir);
		return to != null && to.block().acceptItem(item, to, tile);
	}

	@Override
	public TileEntity getEntity() {
		return new JunctionEntity();
	}

	@Override
	public Array<Object> getDebugInfo(Tile tile){
		JunctionEntity entity = tile.entity();
		Array<Object> arr = super.getDebugInfo(tile);
		for(int i = 0; i < 4; i ++){
			arr.add("nearby." + i);
			arr.add(tile.getNearby(i));
		}

        Consumer<Buffer> write = b -> {
            for(int i = 0; i < b.index; i++){
                long l = b.items[i];
                float time = NumberUtils.intBitsToFloat(Bits.getLeftInt(l));
                int val = Bits.getRightInt(l);
                Item item = Item.getByID(Bits.getLeftShort(val));
                int direction = Bits.getRightShort(val);
                Tile dest = tile.getNearby(direction);
                arr.add("  bufferx.item");
                arr.add(time + " | " + item.name + " | " + dest.block() + ":" + dest.floor());
            }
        };

		arr.add("buffer.bx");
		arr.add(entity.bx.index);
		write.accept(entity.bx);
        arr.add("buffer.by");
        arr.add(entity.bx.index);
		write.accept(entity.by);

		return arr;
	}

	class JunctionEntity extends TileEntity{
		Buffer bx = new Buffer();
		Buffer by = new Buffer();
	}

	class Buffer{
		long[] items = new long[capacity];
		int index;

		void add(long id){
			if(full()) return;
			items[index++] = id;
		}

		boolean full(){
			return index >= items.length - 1;
		}
	}
}
