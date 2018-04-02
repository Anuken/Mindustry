package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.NumberUtils;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Bits;

public class TunnelConveyor extends Block{
	protected int maxdist = 3;
	protected float speed = 53; //frames taken to go through this tunnel
	protected int capacity = 32;

	protected TunnelConveyor(String name) {
		super(name);
		rotate = true;
		update = true;
		solid = true;
		health = 70;
		instantTransfer = true;
		bars.add(new BlockBar(Color.GREEN, true, tile -> (float)tile.<TunnelEntity>entity().index/capacity));
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Conveyor || other instanceof Router || other instanceof Junction;
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		TunnelEntity entity = tile.entity();

		if(entity.index >= entity.buffer.length) return;

		entity.buffer[entity.index ++] = Bits.packLong(NumberUtils.floatToIntBits(Timers.time()), item.id);
	}

	@Override
	public void update(Tile tile){
		TunnelEntity entity = tile.entity();

		if(entity.index > 0){
			long l = entity.buffer[0];
			float time = NumberUtils.intBitsToFloat(Bits.getLeftInt(l));

			if(Timers.time() >= time + speed || Timers.time() < time){

				Item item = Item.getByID(Bits.getRightInt(l));

				Tile tunnel = getDestTunnel(tile, item);
				if(tunnel == null) return;
				Tile target = tunnel.getNearby(tunnel.getRotation());
				if(target == null) return;

				if(!target.block().acceptItem(item, target, tunnel)) return;

				target.block().handleItem(item, target, tunnel);
				System.arraycopy(entity.buffer, 1, entity.buffer, 0, entity.index - 1);
				entity.index --;
			}
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TunnelEntity entity = tile.entity();
		int rot = source.relativeTo(tile.x, tile.y);
		if(rot != (tile.getRotation() + 2)%4) return false;
		return entity.index < entity.buffer.length - 1;
	}

	@Override
	public TileEntity getEntity() {
		return new TunnelEntity();
	}

	@Override
	public Array<Object> getDebugInfo(Tile tile){
		TunnelEntity entity = tile.entity();
		Array<Object> arr = super.getDebugInfo(tile);
		for(int i = 0; i < 4; i ++){
			arr.add("nearby." + i);
			arr.add(tile.getNearby(i));
		}

		arr.add("buffer");
		arr.add(entity.index);

		for(int i = 0; i < entity.index; i++){
			long l = entity.items[i];
			float time = NumberUtils.intBitsToFloat(Bits.getLeftInt(l));
			Item item = Item.getByID(Bits.getRightInt(l));
			Tile dest = getDestTunnel(tile, item);
			arr.add("  buffer.item");
			arr.add(time + " | " + item.name + " | " + ( dest == null ? "no dest" : dest.block() + ":" + dest.floor()));
		}

		return arr;
	}

	Tile getDestTunnel(Tile tile, Item item){
		Tile dest = tile;
		int rel = (tile.getRotation() + 2)%4;
		for(int i = 0; i < maxdist; i ++){
			if(dest == null) return null;
			dest = dest.getNearby(rel);
			if(dest != null && dest.block() instanceof TunnelConveyor && dest.getRotation() == rel
					&& dest.getNearby(rel) != null
					&& dest.getNearby(rel).block().acceptItem(item, dest.getNearby(rel), dest)){
				return dest;
			}
		}
		return null;
	}

	class TunnelEntity extends TileEntity {
		long[] buffer = new long[capacity];
		int index;
	}
}
