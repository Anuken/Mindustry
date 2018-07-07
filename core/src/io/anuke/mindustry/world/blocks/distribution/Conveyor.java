package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongArray;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.itemSize;
import static io.anuke.mindustry.Vars.tilesize;

public class Conveyor extends Block{
	private static ItemPos drawpos = new ItemPos();
	private static ItemPos pos1 = new ItemPos();
	private static ItemPos pos2 = new ItemPos();

	private static final float itemSpace = 0.135f * 2.2f;
	private static final float offsetScl = 128f*3f;
	private static final float minmove = 1f / (Short.MAX_VALUE - 2);

	private final Translator tr1 = new Translator();
	private final Translator tr2 = new Translator();
	private final TextureRegion region1 = new TextureRegion();
	private final TextureRegion region2 = new TextureRegion();

	protected float speed = 0f;
	protected float carryCapacity = 8f;

	protected Conveyor(String name) {
		super(name);
		rotate = true;
		update = true;
		layer = Layer.overlay;
		group = BlockGroup.transportation;
		hasItems = true;
		autoSleep = true;
		itemCapacity = Math.round(tilesize/ itemSpace);
	}

	@Override
	public void setBars() {}

	@Override
	public void setStats(){
		super.setStats();
		stats.add(BlockStat.itemSpeed, speed * 60, StatUnit.pixelsSecond);
	}

	@Override
	public void draw(Tile tile){
		ConveyorEntity entity = tile.entity();
		byte rotation = tile.getRotation();

		GridPoint2 point = Geometry.d4[rotation];

		int offset = entity.clogHeat <= 0.5f ? (int)((Timers.time()/4f)%8) : 0;
		TextureRegion region = Draw.region(name);

		region1.setRegion(region, 0, 0, region.getRegionWidth() - offset, region.getRegionHeight());
		region2.setRegion(region, region.getRegionWidth() - offset, 0, offset, region.getRegionHeight());

		float x = tile.drawx(), y = tile.drawy();

		if(offset % 2 == 1){
			if(point.x < 0) x += 0.75f;
			if(point.y < 0)
				y += 0.5f;
			else if(point.y > 0)
				y -= 0.5f;
		}

		Draw.rect(region1,
				x + (point.x * (tilesize/2f - region1.getRegionWidth()/2f)),
				y + (point.y * (tilesize/2f - region1.getRegionWidth()/2f)), rotation * 90);
		Draw.rect(region2,
				x - (point.x * (tilesize/2f - region2.getRegionWidth()/2f)),
				y - (point.y * (tilesize/2f - region2.getRegionWidth()/2f)), rotation * 90);
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
						(int)(tile.x * tilesize + tr1.x * pos.y + tr2.x),
						(int)(tile.y * tilesize + tr1.y * pos.y + tr2.y), itemSize, itemSize);
			}

		}catch (IndexOutOfBoundsException e){
			Log.err(e);
		}
	}

	@Override
	public void unitOn(Tile tile, Unit unit) {
		ConveyorEntity entity = tile.entity();

		entity.wakeUp();

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
			unit.getVelocity().add(tx * speed * Timers.delta(), ty * speed * Timers.delta());
		}
	}

	@Override
	public synchronized void update(Tile tile){

		ConveyorEntity entity = tile.entity();
		entity.minitem = 1f;

		int minremove = Integer.MAX_VALUE;
		float speed = Math.max(this.speed - (1f - (carryCapacity - entity.carrying) / carryCapacity), 0f);
		float totalMoved = 0f;

		for (int i = entity.convey.size - 1; i >= 0; i--) {
			long value = entity.convey.get(i);
			ItemPos pos = pos1.set(value, ItemPos.updateShorts);

			//..this should never happen, but in case it does, remove it and stop here
			if (pos.item == null) {
				entity.convey.removeValue(value);
				break;
			}

			float nextpos = (i == entity.convey.size - 1 ? 100f : pos2.set(entity.convey.get(i + 1), ItemPos.updateShorts).y) - itemSpace;
			if (entity.minCarry >= pos.y && entity.minCarry <= nextpos) {
				nextpos = entity.minCarry;
			}
			float maxmove = Math.min(nextpos - pos.y, speed * Timers.delta());

			if (maxmove > minmove) {
				pos.y += maxmove;
				pos.x = Mathf.lerpDelta(pos.x, 0, 0.06f);
				totalMoved += maxmove;
			} else {
				pos.x = Mathf.lerpDelta(pos.x, pos.seed / offsetScl, 0.1f);
			}

			pos.y = Mathf.clamp(pos.y);

			if (pos.y >= 0.9999f && offloadDir(tile, pos.item)) {
				minremove = Math.min(i, minremove);
				totalMoved = 1f;
				tile.entity.items.remove(pos.item, 1);
			} else {
				value = pos.pack();

				if (pos.y < entity.minitem)
					entity.minitem = pos.y;
				entity.convey.set(i, value);
			}
		}

		if(entity.minitem < itemSpace){
			entity.clogHeat = Mathf.lerpDelta(entity.clogHeat, 1f, 0.02f);
		}else{
			entity.clogHeat = Mathf.lerpDelta(entity.clogHeat, 0f, 1f);
		}

		entity.carrying = 0f;
		entity.minCarry = 2f;

		if(totalMoved <= 0.0001f){
			entity.sleep();
		}

		if (minremove != Integer.MAX_VALUE) entity.convey.truncate(minremove);
	}

	@Override
	public boolean isAccessible(){
		return true;
	}

	@Override
	public synchronized int removeStack(Tile tile, Item item, int amount) {
		ConveyorEntity entity = tile.entity();
		entity.wakeUp();
		int removed = 0;

		for(int j = 0; j < amount; j ++) {
			for (int i = 0; i < entity.convey.size; i++) {
				long val = entity.convey.get(i);
				ItemPos pos = pos1.set(val, ItemPos.drawShorts);
				if(pos.item == item){
					entity.convey.removeValue(val);
					entity.items.remove(item, 1);
					removed ++;
					break;
				}
			}
		}
		return removed;
	}

	@Override
	public void getStackOffset(Item item, Tile tile, Translator trns) {
		trns.trns(tile.getRotation()*90 + 180f, tilesize/2f);
	}

	@Override
	public synchronized int acceptStack(Item item, int amount, Tile tile, Unit source) {
		ConveyorEntity entity = tile.entity();
		return entity.minitem > itemSpace ? 1 : 0;
	}

	@Override
	public synchronized void handleStack(Item item, int amount, Tile tile, Unit source) {
		ConveyorEntity entity = tile.entity();

		long result = ItemPos.packItem(item, 0f, 0f, (byte)Mathf.random(255));
		entity.convey.insert(0, result);
		entity.items.add(item, 1);
		entity.wakeUp();
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
		entity.wakeUp();
		long result = ItemPos.packItem(item, y*0.9f, pos, (byte)Mathf.random(255));
		boolean inserted = false;

		tile.entity.items.add(item, 1);

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

	@Override
	public Array<Object> getDebugInfo(Tile tile) {
		ConveyorEntity entity = tile.entity();
		Array<Object> arr = super.getDebugInfo(tile);
		arr.addAll(Array.with(
			"mincarry", entity.minCarry,
			"minitem", entity.minCarry,
			"carrying", entity.carrying,
			"clogHeat", entity.clogHeat,
			"sleeping", entity.isSleeping()
		));
		return arr;
	}

	@Override
	public TileEntity getEntity(){
		return new ConveyorEntity();
	}

	public static class ConveyorEntity extends TileEntity{

		LongArray convey = new LongArray();
		float minitem = 1;
		float carrying;
		float minCarry = 2f;

		float clogHeat = 0f;

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
		}
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

			if(values[0] >= Item.all().size || values[0] < 0)
				item = null;
			else
				item = Item.all().get(values[0]);

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