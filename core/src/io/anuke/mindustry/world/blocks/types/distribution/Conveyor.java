package io.anuke.mindustry.world.blocks.types.distribution;

import static io.anuke.mindustry.Vars.tilesize;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class Conveyor extends Block{
	private static Item[] items = Item.values();
	private static ItemPos pos1 = new ItemPos();
	private static ItemPos pos2 = new ItemPos();
	private static IntArray removals = new IntArray();
	
	public float speed = 0.02f;
	
	protected Conveyor(String name) {
		super(name);
		rotate = true;
		update = true;
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Conveyor || other instanceof Router || other instanceof Junction;
	}
	
	@Override
	public void draw(Tile tile){
		ConveyorEntity entity = tile.entity();
		
		Draw.rect(name() + 
				(Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed && acceptItem(Item.stone, tile, null) ? "" : "move"), tile.worldx(), tile.worldy(), tile.rotation * 90);
		
		for(int i = 0; i < entity.convey.size; i ++){
			ItemPos pos = pos1.set(entity.convey.get(i));
			
			Tmp.v1.set(tilesize, 0).rotate(tile.rotation * 90);
			Tmp.v2.set(-tilesize / 2, pos.x*tilesize/2).rotate(tile.rotation * 90);
			
			Draw.rect("icon-" + pos.item.name(), 
					tile.x * tilesize + Tmp.v1.x * pos.y + Tmp.v2.x, 
					tile.y * tilesize + Tmp.v1.y * pos.y + Tmp.v2.y, 4, 4);
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
			
			boolean canmove = true;
			
			for(int j = 0; j < entity.convey.size; j ++){
				ItemPos other = pos2.set(entity.convey.get(j));
				
				if(other.y > pos.y && other.y - pos.y < 0.14){
					canmove = false;
					break;
				}
			}
			
			if(canmove){
				pos.y += speed * Timers.delta();
				pos.x = MathUtils.lerp(pos.x, 0, 0.06f * Timers.delta());
			}else{
				pos.x = MathUtils.lerp(pos.x, pos.seed/128f/3f, 0.1f * Timers.delta());
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
		
		if(removals.size > 0)
			UCore.log(removals.size);
		entity.convey.removeAll(removals);
	}
	
	@Override
	public TileEntity getEntity(){
		return new ConveyorEntity();
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int direction = source == null ? 0 : Math.abs(source.relativeTo(dest.x, dest.y) - dest.rotation);
		float minitem = dest.<ConveyorEntity>entity().minitem;
		return ((direction == 0) && minitem > 0.05f) || 
				((direction %2 == 1) && minitem > 0.5f);
	}
	
	@Override
	public String description(){
		return "Moves items.";
	}

	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		int ch = Math.abs(source.relativeTo(tile.x, tile.y) - tile.rotation);
		int ang = ((source.relativeTo(tile.x, tile.y) - tile.rotation));
		

		float pos = ch == 0 ? 0 : ch % 2 == 1 ? 0.5f : 1f;
		float y = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;
		
		ConveyorEntity entity = tile.entity();
		entity.convey.add(ItemPos.packItem(item, y*0.9f, pos, (byte)Mathf.random(255)));
	}
	
	/**
	 * Conveyor data format:
	 * [0] item ordinal
	 * [1] x: byte ranging from -128 to 127, scaled should be at [-1, 1], corresponds to relative X from the conveyor middle
	 * [2] y: byte ranging from 0 to 127, scaled should be at [0, 1], corresponds to relative Y from the conveyor start
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
		}
	}
	
	//Container class. Do not instantiate.
	static class ItemPos{
		Item item;
		float x, y;
		byte seed;
		
		private ItemPos(){}
		
		ItemPos set(int value){
			byte[] values = Bits.getBytes(value);
			item = items[values[0]];
			x = values[1] / 127f;
			y = ((int)values[2]) / 127f;
			seed = values[3];
			return this;
		}
		
		int pack(){
			return packItem(item, x, y, seed);
		}
		
		static int packItem(Item item, float x, float y, byte seed){
			byte[] bytes = Bits.getBytes(0);
			bytes[0] = (byte)item.ordinal();
			bytes[1] = (byte)(x*127);
			bytes[2] = (byte)(y*127);
			bytes[3] = seed;
			//UCore.log("Packing item: ", item, x, y, seed, "\n", Arrays.toString(bytes));
			//UCore.log(Arrays.toString(Bits.getBytes(Bits.packInt(bytes))));
			return Bits.packInt(bytes);
		}
	}
}
