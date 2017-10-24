package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class Router extends Block{
	private ObjectMap<Tile, Byte> lastmap = new ObjectMap<>();
	int maxitems = 20;

	public Router(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void update(Tile tile){
		if(Timers.get(tile, 2) && tile.entity.totalItems() > 0){
			if(lastmap.get(tile, (byte)-1) != tile.rotation)
				tryDump(tile, tile.rotation, null);
			
			tile.rotation ++;
			tile.rotation %= 4;
		}
	}
	
	@Override
	public void handleItem(Tile tile, Item item, Tile source){
		super.handleItem(tile, item, source);
		lastmap.put(tile, (byte)tile.relativeTo(source.x, source.y));
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		int items = dest.entity.totalItems();
		return items < maxitems;
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		
		float fract = (float)tile.entity.totalItems()/maxitems;
		
		Vars.renderer.drawBar(Color.GREEN, tile.worldx(), tile.worldy() + 6, fract);
	}
	
	@Override
	public String description(){
		return "Split input materials into 3 directions.";
	}

}
