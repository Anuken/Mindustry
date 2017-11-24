package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Router extends Block{
	private ObjectMap<Tile, Byte> lastmap = new ObjectMap<>();
	int capacity = 20;

	public Router(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[iteminfo]Capacity: " + capacity);
	}
	
	@Override
	public boolean canReplace(Block other){
		return other instanceof Junction || other instanceof Conveyor;
	}
	
	@Override
	public void update(Tile tile){
		if(Timers.get(tile, "dump", 2) && tile.entity.totalItems() > 0){
			if(lastmap.get(tile, (byte)-1) != tile.rotation 
					|| Mathf.chance(0.3)){ //sometimes dump backwards at a 1/4 chance... this somehow works?
				tryDump(tile, tile.rotation, null);
			}
			
			tile.rotation ++;
			tile.rotation %= 4;
		}
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		super.handleItem(item, tile, source);
		lastmap.put(tile, (byte)tile.relativeTo(source.x, source.y));
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int items = dest.entity.totalItems();
		return items < capacity;
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		
		float fract = (float)tile.entity.totalItems()/capacity;
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 6, fract);
	}

}
