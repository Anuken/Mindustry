package io.anuke.mindustry.world.blocks.types;

import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class LiquidRouter extends Conduit{
	private ObjectMap<Tile, Byte> lastmap = new ObjectMap<>();

	public LiquidRouter(String name) {
		super(name);
		rotate = false;
		solid = true;
	}
	
	@Override
	public String description(){
		return "Splits input liquid into 3 directions.";
	}
	
	@Override
	public void update(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(Timers.get(tile, 2) && entity.liquidAmount > 0){
			if(lastmap.get(tile, (byte)-1) != tile.rotation){
				tryMoveLiquid(tile, tile.getNearby()[tile.rotation]);
			}
			
			tile.rotation ++;
			tile.rotation %= 4;
		}
	}
	
	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		super.handleLiquid(tile, source, liquid, amount);
		lastmap.put(tile, (byte)tile.relativeTo(source.x, source.y));
	}
	
	@Override
	public void draw(Tile tile){
		ConduitEntity entity = tile.entity();
		Draw.rect(name(), tile.worldx(), tile.worldy());
		
		if(entity.liquid == null) return;
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color();
	}

}
