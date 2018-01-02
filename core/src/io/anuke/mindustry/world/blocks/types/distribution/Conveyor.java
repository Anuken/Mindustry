package io.anuke.mindustry.world.blocks.types.distribution;

import static io.anuke.mindustry.Vars.tilesize;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Layer;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.*;

public class Conveyor extends Block{
	private static ItemPos pos1 = new ItemPos();
	private static ItemPos pos2 = new ItemPos();
	private static IntArray removals = new IntArray();
	private static final float itemSpace = 0.135f;
	private static final float offsetScl = 128f*3f;
	private static final float itemSize = 4f;
	
	public float speed = 0.02f;
	
	protected Conveyor(String name) {
		super(name);
		rotate = true;
		update = true;
		layer = Layer.overlay;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[iteminfo]Item Speed/second: " + Strings.toFixed(speed * 60, 1));
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Conveyor || other instanceof Router || other instanceof Junction;
	}
	
	@Override
	public void draw(Tile tile){
		byte rotation = tile.getRotation();
		
		Draw.rect(name() + 
				(Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed && acceptItem(Item.stone, tile, null) ? "" : "move"), 
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
		
		for(int i = 0; i < entity.convey.size; i ++){
			ItemPos pos = pos1.set(entity.convey.get(i));
			
			Tmp.v1.set(tilesize, 0).rotate(rotation * 90);
			Tmp.v2.set(-tilesize / 2, pos.x*tilesize/2).rotate(rotation * 90);
			
			Draw.rect("icon-" + pos.item.name,
					tile.x * tilesize + Tmp.v1.x * pos.y + Tmp.v2.x, 
					tile.y * tilesize + Tmp.v1.y * pos.y + Tmp.v2.y, itemSize, itemSize);
		}
	}
	
	@Override
	public void update(Tile tile){
		
		ConveyorEntity entity = tile.entity();
		entity.minitem = 1f;
		
		removals.clear();

		for(int i = 0; i < entity.convey.size; i ++){
			int value = entity.convey.get(i);
			ItemPos pos = pos1.set(value);
			
			boolean canmove = i == entity.convey.size - 1 || 
					!(pos2.set(entity.convey.get(i + 1)).y - pos.y < itemSpace * Timers.delta());
			
			if(canmove){
				pos.y += Math.max(speed * Timers.delta(), 1f/252f); //TODO fix precision issues?
				pos.x = Mathf.lerpDelta(pos.x, 0, 0.06f);
			}else{
				pos.x = Mathf.lerpDelta(pos.x, pos.seed/offsetScl, 0.1f);
			}
			
			pos.y = Mathf.clamp(pos.y);
			
			if(pos.y >= 0.9999f && offloadDir(tile, pos.item)){
				removals.add(value);
			}else{
				value = pos.pack();
				
				if(pos.y < entity.minitem)
					entity.minitem = pos.y;
				entity.convey.set(i, value);
			}
			
		}
		entity.convey.removeAll(removals);
	}
	
	@Override
	public TileEntity getEntity(){
		return new ConveyorEntity();
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int direction = source == null ? 0 : Math.abs(source.relativeTo(dest.x, dest.y) - dest.getRotation());
		float minitem = dest.<ConveyorEntity>entity().minitem;
		return (((direction == 0) && minitem > 0.05f) || 
				((direction %2 == 1) && minitem > 0.52f)) && (source == null || !(source.block().rotate && (source.getRotation() + 2) % 4 == dest.getRotation()));
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		byte rotation = tile.getRotation();
		
		int ch = Math.abs(source.relativeTo(tile.x, tile.y) - rotation);
		int ang = ((source.relativeTo(tile.x, tile.y) - rotation));
		

		float pos = ch == 0 ? 0 : ch % 2 == 1 ? 0.5f : 1f;
		float y = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;
		
		ConveyorEntity entity = tile.entity();
		int result = ItemPos.packItem(item, y*0.9f, pos, (byte)Mathf.random(255));
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
	
	/**
	 * Conveyor data format:
	 * [0] item ordinal
	 * [1] x: byte ranging from -128 to 127, scaled should be at [-1, 1], corresponds to relative X from the conveyor middle
	 * [2] y: byte ranging from -128 to 127, scaled should be at [0, 1], corresponds to relative Y from the conveyor start
	 * [3] seed: -128 to 127, unscaled
	 * Size is 4 bytes, or one int.
	 */
	public static class ConveyorEntity extends TileEntity{
		IntArray convey = new IntArray();
		float minitem = 1;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeInt(convey.size);
			
			for(int i = 0; i < convey.size; i ++){
				stream.writeInt(convey.get(i));
			}
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			int amount = stream.readInt();
			convey.ensureCapacity(amount);
			
			for(int i = 0; i < amount; i ++){
				convey.add(stream.readInt());
			}
			
			sort(convey.items, convey.size);
		}
	}
	
	private static void sort(int[] elements, int length){
		List<Integer> wrapper = new AbstractList<Integer>() {

	        @Override
	        public Integer get(int index) {
	            return elements[index];
	        }

	        @Override
	        public int size() {
	            return length;
	        }

	        @Override
	        public Integer set(int index, Integer element) {
	            int v = elements[index];
	            elements[index] = element;
	            return v;
	        }
	    };
	    
	    Collections.sort(wrapper, new Comparator(){public int compare(Object a, Object b){ return compareItems((Integer)a, (Integer)b); }});
	}
	
	private static int compareItems(int a, int b){
		pos1.set(a);
		pos2.set(b);
		return Float.compare(pos1.y, pos2.y);
	}
	
	//Container class. Do not instantiate.
	static class ItemPos{
		Item item;
		float x, y;
		byte seed;
		
		private ItemPos(){}
		
		ItemPos set(int value){
			byte[] values = Bits.getBytes(value);
			item = Item.getAllItems().get(values[0]);
			x = values[1] / 127f;
			y = ((int)values[2] + 128) / 255f;
			seed = values[3];
			return this;
		}
		
		int pack(){
			return packItem(item, x, y, seed);
		}
		
		static int packItem(Item item, float x, float y, byte seed){
			byte[] bytes = Bits.getBytes(0);
			bytes[0] = (byte)item.id;
			bytes[1] = (byte)(x*127);
			bytes[2] = (byte)(y*255-128);
			bytes[3] = seed;
			//UCore.log("Packing item: ", item, x, y, seed, "\n", Arrays.toString(bytes));
			//UCore.log(Arrays.toString(Bits.getBytes(Bits.packInt(bytes))));
			return Bits.packInt(bytes);
		}
	}
}
