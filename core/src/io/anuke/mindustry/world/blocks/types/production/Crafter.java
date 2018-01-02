package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;

import java.util.Arrays;

public class Crafter extends Block{
	protected final int timerDump = timers++;
	
	protected Item[] requirements;
	protected Item result;

	public Crafter(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[craftinfo]Input: " + Arrays.toString(requirements));
		list.add("[craftinfo]Output: " + result);
	}
	
	@Override
	public void update(Tile tile){
		
		if(tile.entity.timer.get(timerDump, 15) && tile.entity.hasItem(result)){
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
		Effects.effect(Fx.smelt, tile.entity);
	}

	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		for(Item req : requirements){
			if(item == req){
				return true;
			}
		}
		return false;
	}
}
