package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class Crafter extends Block{
	protected Item[] requirements;
	protected Item result;

	public Crafter(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void update(Tile tile){
		
		if(Timers.get(tile, 20) && tile.entity.hasItem(result)){
			tryDump(tile, -1, result);
		}
		
		for(Item item : requirements){
			if(!tile.entity.hasItem(item)){
				return;
			}
		}
		
		for(Item item : requirements){
			tile.entity.removeItem(item, 1);
		}
		
		offloadNear(tile, result);
		Effects.effect("smelt", tile.entity);
		
		
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		boolean craft = false;
		for(Item req : requirements){
			if(item == req){
				craft = true;
				break;
			}
		}
		return craft;
	}
}
