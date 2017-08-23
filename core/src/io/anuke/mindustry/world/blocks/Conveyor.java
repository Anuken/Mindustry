package io.anuke.mindustry.world.blocks;

import static io.anuke.mindustry.Vars.tilesize;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.DelayedRemovalArray;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timers;

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
		
		Draw.rect(name() + (Timers.time() % ((20 / 100f) / speed) < (10 / 100f) / speed ? "" : "move"), tile.worldx(), tile.worldy(), tile.rotation * 90);
		
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
		
		entity.convey.begin();

		for(ItemPos pos : entity.convey){
			pos.pos += speed * Mathf.delta();
			pos.y = MathUtils.lerp(pos.y, 0, 0.14f * Mathf.delta());
			
			if(pos.pos >= 1f && offloadDir(tile, pos.item)){
				entity.convey.removeValue(pos, true);
				continue;
			}
			pos.pos = Mathf.clamp(pos.pos);
		}

		entity.convey.end();
	}
	
	@Override
	public TileEntity getEntity(){
		return new ConveyorEntity();
	}

	@Override
	public boolean accept(Item item, Tile dest, Tile source){
		return true;
	}
	
	@Override
	public String description(){
		return "Moves Items";
	}

	@Override
	public void handleItem(Tile tile, Item item, Tile source){
		int ch = Math.abs(source.relativeTo(tile.x, tile.y) - tile.rotation);
		int ang = ((source.relativeTo(tile.x, tile.y) - tile.rotation));
		

		float pos = ch == 0 ? 0 : ch%2 == 1 ? 0.5f : 1f;
		float y = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;
		
		ConveyorEntity entity = tile.entity();
		entity.convey.add(new ItemPos(item, pos, y*0.9f));
	}
	
	public static class ConveyorEntity extends TileEntity{
		DelayedRemovalArray<ItemPos> convey = new DelayedRemovalArray<>();
	}
	
	static class ItemPos{
		public Item item;
		public float pos, y;
		
		public ItemPos(Item item, float pos, float y){
			this.item = item;
			this.pos = pos;
			this.y = y;
		}
	}
}
