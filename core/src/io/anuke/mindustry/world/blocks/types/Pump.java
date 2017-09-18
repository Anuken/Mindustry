package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class Pump extends Conduit{

	public Pump(String name) {
		super(name);
		rotate = false;
	}
	
	@Override
	public boolean accept(Tile tile, Tile source, Liquid liquid, float amount){
		return false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy());
	}
	
	@Override
	public void update(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(Timers.get(tile, "pump", 10) && entity.amount < capacity){
			entity.liquid = Liquid.water;
			entity.amount += 10f;
		}
		
		if(Timers.get(tile, "dump", 1)){
			tryDumpLiquid(tile);
		}
	}

}
