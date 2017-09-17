package io.anuke.mindustry.world.blocks;

import static io.anuke.mindustry.Vars.tilesize;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.DelayedRemovalArray;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Conveyor extends Block{
	float speed = 0.02f;
	
	protected Conveyor(String name) {
		super(name);
		rotate = true;
		update = true;
	}
	
	@Override
	public void draw(Tile tile){
		ConveyorEntity entity = tile.entity();
		
		Draw.rect(name() + 
				(Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed && accept(Item.stone, tile, null) ? "" : "move"), tile.worldx(), tile.worldy(), tile.rotation * 90);
		
		for(ItemPos pos : entity.convey){
			vector.set(tilesize, 0).rotate(tile.rotation * 90);
			vector2.set(-tilesize / 2, pos.y*tilesize/2).rotate(tile.rotation * 90);
			
			Draw.rect("icon-" + pos.item.name(), 
					tile.x * tilesize + vector.x * pos.pos + vector2.x, 
					tile.y * tilesize + vector.y * pos.pos + vector2.y, 4, 4);
		}
	}
	
	@Override
	public void update(Tile tile){
		
		ConveyorEntity entity = tile.entity();
		entity.minitem = 1f;
		
		entity.convey.begin();

		for(ItemPos pos : entity.convey){
			boolean canmove = true;
			
			for(int i = 0; i < entity.convey.size; i ++){
				ItemPos other = entity.convey.get(i);
				
				if(other.pos > pos.pos && other.pos-pos.pos < 0.14){
					canmove = false;
					break;
				}
			}
			
			if(canmove){
				pos.pos += speed * Timers.delta();
				pos.y = MathUtils.lerp(pos.y, 0, 0.14f * Timers.delta());
			}else{
				pos.y = MathUtils.lerp(pos.y, pos.seed/128f/3f, 0.1f * Timers.delta());
			}
			
			if(pos.pos >= 1f && offloadDir(tile, pos.item)){
				entity.convey.removeValue(pos, true);
				continue;
			}
			pos.pos = Mathf.clamp(pos.pos);
			
			if(pos.pos < entity.minitem)
				entity.minitem = pos.pos;
		}

		entity.convey.end();
	}
	
	@Override
	public TileEntity getEntity(){
		return new ConveyorEntity();
	}

	@Override
	public boolean accept(Item item, Tile dest, Tile source){
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
	public void handleItem(Tile tile, Item item, Tile source){
		int ch = Math.abs(source.relativeTo(tile.x, tile.y) - tile.rotation);
		int ang = ((source.relativeTo(tile.x, tile.y) - tile.rotation));
		

		float pos = ch == 0 ? 0 : ch % 2 == 1 ? 0.5f : 1f;
		float y = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;
		
		ConveyorEntity entity = tile.entity();
		entity.convey.add(new ItemPos(item, pos, y*0.9f));
	}
	
	public static class ConveyorEntity extends TileEntity{
		DelayedRemovalArray<ItemPos> convey = new DelayedRemovalArray<>();
		float minitem = 1;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeInt(convey.size);
			
			for(ItemPos pos : convey){
				stream.writeByte(pos.item.ordinal());
				stream.writeByte((int)(pos.pos*255-128));
				stream.writeByte((int)(pos.y*255-128));
			}
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			int amount = stream.readInt();
			
			for(int i = 0; i < amount; i ++){
				Item item = Item.values()[stream.readByte()];
				float pos = ((int)stream.readByte()+128)/255f;
				float y = ((int)stream.readByte()+128)/255f;
				
				ItemPos out = new ItemPos(item, pos, y);
				convey.add(out);
			}
		}
	}
	
	static class ItemPos{
		Item item;
		float pos, y;
		byte seed;
		
		public ItemPos(Item item, float pos, float y){
			this.item = item;
			this.pos = pos;
			this.y = y;
			seed = (byte)MathUtils.random(255);
		}
		
	}
}
