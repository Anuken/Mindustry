package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.utils.LongArray;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

import static io.anuke.mindustry.Vars.tilesize;

public class Conveyor extends Block{
	private static ItemPos drawpos = new ItemPos();
	private static ItemPos pos1 = new ItemPos();
	private static ItemPos pos2 = new ItemPos();
	private static final float itemSpace = 0.135f * 2.2f;
	private static final float offsetScl = 128f*3f;
	private static final float itemSize = 5f;
	private static final float minmove = 1f / (Short.MAX_VALUE - 2);

	private final Translator tr1 = new Translator();
	private final Translator tr2 = new Translator();

	protected float speed = 0f;
	protected float carryCapacity = 8f;

	protected Conveyor(String name) {
		super(name);
		rotate = true;
		update = true;
		layer = Layer.overlay;
		group = BlockGroup.transportation;
		hasInventory = false;
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add("itemspeedsecond", Strings.toFixed(speed * 60, 1));
	}

	@Override
	public void draw(Tile tile){
		byte rotation = tile.getRotation();

		Draw.rect(name() +
						(Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed && acceptItem(Items.stone, tile, null) ? "" : "move"),
				tile.worldx(), tile.worldy(), rotation * 90);
	}

	@Override
	public boolean isLayer(Tile tile){
		return tile.<ConveyorEntity>entity().convey.size > 0;
	}

	@Override
	public void drawLayer(Tile tile){
		ConveyorEntity entity = tile.entity();

		byte rotation = tile.getRotation();

		try {

			for (int i = 0; i < entity.convey.size; i++) {
				ItemPos pos = drawpos.set(entity.convey.get(i), ItemPos.drawShorts);

				if (pos.item == null) continue;

				tr1.trns(rotation * 90, tilesize, 0);
				tr2.trns(rotation * 90, -tilesize / 2, pos.x * tilesize / 2);

				Draw.rect(pos.item.region,
						tile.x * tilesize + tr1.x * pos.y + tr2.x,
						tile.y * tilesize + tr1.y * pos.y + tr2.y, itemSize, itemSize);
			}

		}catch (IndexOutOfBoundsException e){
			Log.err(e);
		}
	}

	@Override
	public void unitOn(Tile tile, Unit unit) {
		ConveyorEntity entity = tile.entity();

		float speed = this.speed * tilesize / 2.3f;
		float tx = Geometry.d4[tile.getRotation()].x, ty = Geometry.d4[tile.getRotation()].y;

		float min;

		if(Math.abs(tx) > Math.abs(ty)){
			float rx = tile.worldx() - tx/2f*tilesize;
			min = Mathf.clamp((unit.x - rx) * tx / tilesize);
		}else{
			float ry = tile.worldy() - ty/2f*tilesize;
			min = Mathf.clamp((unit.y - ry) * ty / tilesize);
		}

		entity.minCarry = Math.min(entity.minCarry, min);
		entity.carrying += unit.getMass();

		if(entity.convey.size * itemSpace < 0.9f){
			unit.velocity.add(tx * speed * Timers.delta(), ty * speed * Timers.delta());
		}
	}

	@Override
	public void update(Tile tile){

		ConveyorEntity entity = tile.entity();
		entity.minitem = 1f;

		int minremove = Integer.MAX_VALUE;
		float speed = Math.max(this.speed - (1f - (carryCapacity - entity.carrying)/carryCapacity), 0f);

		for(int i = entity.convey.size - 1; i >= 0; i --){
			long value = entity.convey.get(i);
			ItemPos pos = pos1.set(value, ItemPos.updateShorts);

			//..this should never happen, but in case it does, remove it and stop here
			if(pos.item == null){
				entity.convey.removeValue(value);
				break;
			}

			float nextpos = (i == entity.convey.size - 1 ? 100f : pos2.set(entity.convey.get(i + 1), ItemPos.updateShorts).y) - itemSpace;
			if(entity.minCarry >= pos.y && entity.minCarry <= nextpos){
				nextpos = entity.minCarry;
			}
			float maxmove = Math.min(nextpos - pos.y, speed * Timers.delta());

			if(maxmove > minmove){
				pos.y += maxmove;
				pos.x = Mathf.lerpDelta(pos.x, 0, 0.06f);
			}else{
				pos.x = Mathf.lerpDelta(pos.x, pos.seed/offsetScl, 0.1f);
			}

			pos.y = Mathf.clamp(pos.y);

			if(pos.y >= 0.9999f && offloadDir(tile, pos.item)){
				minremove = Math.min(i, minremove);
			}else{
				value = pos.pack();

				if(pos.y < entity.minitem)
					entity.minitem = pos.y;
				entity.convey.set(i, value);
			}
		}

		entity.carrying = 0f;
		entity.minCarry = 2f;

		if(minremove != Integer.MAX_VALUE) entity.convey.truncate(minremove);
	}

	@Override
	public TileEntity getEntity(){
		return new ConveyorEntity();
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		int direction = source == null ? 0 : Math.abs(source.relativeTo(tile.x, tile.y) - tile.getRotation());
		float minitem = tile.<ConveyorEntity>entity().minitem;
		return (((direction == 0) && minitem > itemSpace) ||
				((direction %2 == 1) && minitem > 0.52f)) && (source == null || !(source.block().rotate && (source.getRotation() + 2) % 4 == tile.getRotation()));
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		byte rotation = tile.getRotation();

		int ch = Math.abs(source.relativeTo(tile.x, tile.y) - rotation);
		int ang = ((source.relativeTo(tile.x, tile.y) - rotation));

		float pos = ch == 0 ? 0 : ch % 2 == 1 ? 0.5f : 1f;
		float y = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;

		ConveyorEntity entity = tile.entity();
		long result = ItemPos.packItem(item, y*0.9f, pos, (byte)Mathf.random(255));
		boolean inserted = false;

		for(int i = 0; i < entity.convey.size; i ++){
			if(compareItems(result, entity.convey.get(i)) < 0){
				entity.convey.insert(i, result);
				inserted = true;
				break;
			}
		}

		//this item must be greater than anything there...
		if(!inserted){
			entity.convey.add(result);
		}
	}

	public static class ConveyorEntity extends TileEntity{

		LongArray convey = new LongArray();
		float minitem = 1;
		float carrying;
		float minCarry = 2f;

		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeInt(convey.size);

			for(int i = 0; i < convey.size; i ++){
				stream.writeInt(ItemPos.toInt(convey.get(i)));
			}
		}

		@Override
		public void read(DataInputStream stream) throws IOException{
			convey.clear();
			int amount = stream.readInt();
			convey.ensureCapacity(amount);

			for(int i = 0; i < amount; i ++){
				convey.add(ItemPos.toLong(stream.readInt()));
			}

			sort(convey.items, convey.size);
		}
	}

	private static void sort(long[] elements, int length){
		List<Long> wrapper = new AbstractList<Long>() {

			@Override
			public Long get(int index) {
				return elements[index];
			}

			@Override
			public int size() {
				return length;
			}

			@Override
			public Long set(int index, Long element) {
				long v = elements[index];
				elements[index] = element;
				return v;
			}
		};

		Collections.sort(wrapper, Conveyor::compareItems);
	}

	private static int compareItems(Long a, Long b){
		pos1.set(a, ItemPos.packShorts);
		pos2.set(b, ItemPos.packShorts);
		return Float.compare(pos1.y, pos2.y);
	}

	//Container class. Do not instantiate.
	static class ItemPos{
		private static short[] writeShort = new short[4];
		private static byte[] writeByte = new byte[4];

		private static short[] packShorts = new short[4];
		private static short[] drawShorts = new short[4];
		private static short[] updateShorts = new short[4];

		Item item;
		float x, y;
		byte seed;

		private ItemPos(){}

		ItemPos set(long lvalue, short[] values){
			Bits.getShorts(lvalue, values);

			if(values[0] >= Item.getAllItems().size || values[0] < 0)
				item = null;
			else
				item = Item.getAllItems().get(values[0]);

			x = values[1] / (float)Short.MAX_VALUE;
			y = ((float)values[2]) / Short.MAX_VALUE + 1f;
			seed = (byte)values[3];
			return this;
		}

		long pack(){
			return packItem(item, x, y, seed);
		}

		static long packItem(Item item, float x, float y, byte seed){
			short[] shorts = packShorts;
			shorts[0] = (short)item.id;
			shorts[1] = (short)(x*Short.MAX_VALUE);
			shorts[2] = (short)((y - 1f)*Short.MAX_VALUE);
			shorts[3] = seed;
			return Bits.packLong(shorts);
		}

		static int toInt(long value){
			short[] values = Bits.getShorts(value, writeShort);

			short itemid = values[0];
			float x = values[1] / (float)Short.MAX_VALUE;
			float y = ((float)values[2]) / Short.MAX_VALUE + 1f;
			byte seed = (byte)values[3];

			byte[] bytes = writeByte;
			bytes[0] = (byte)itemid;
			bytes[1] = (byte)(x*127);
			bytes[2] = (byte)(y*255-128);
			bytes[3] = seed;

			return Bits.packInt(bytes);
		}

		static long toLong(int value){
			byte[] values = Bits.getBytes(value, writeByte);

			byte itemid = values[0];
			float x = values[1] / 127f;
			float y = ((int)values[2] + 128) / 255f;
			byte seed = values[3];

			short[] shorts = writeShort;
			shorts[0] = (short)itemid;
			shorts[1] = (short)(x*Short.MAX_VALUE);
			shorts[2] = (short)((y - 1f)*Short.MAX_VALUE);
			shorts[3] = seed;
			return Bits.packLong(shorts);
		}
	}
}