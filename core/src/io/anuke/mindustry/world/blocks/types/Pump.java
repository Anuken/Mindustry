package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class Pump extends Conduit{
	protected float pumpspeed = 2f;

	public Pump(String name) {
		super(name);
		rotate = false;
	}
	
	@Override
	public String description(){
		return "Pumps liquids from blocks into conduits.";
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy());
	}
	
	@Override
	public void update(Tile tile){
		ConduitEntity entity = tile.entity();
		
		if(tile.floor() == Blocks.water &&
				Timers.get(tile, "pump", 8) && entity.liquidAmount < liquidCapacity){
			entity.liquid = Liquid.water;
			entity.liquidAmount += pumpspeed;
		}
		
		if(Timers.get(tile, "dump", 1)){
			tryDumpLiquid(tile);
		}
	}

}
