package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Pump extends LiquidBlock{
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
	public void drawOver(Tile tile){
		if(tile.floor().liquidDrop == null){
			Draw.colorl(0.85f + Mathf.absin(Timers.time(), 6f, 0.15f));
			Draw.rect("cross", tile.worldx(), tile.worldy());
			Draw.color();
		}
	}
	
	@Override
	public void update(Tile tile){
		LiquidEntity entity = tile.entity();
		
		if(tile.floor().liquidDrop != null &&
				Timers.get(tile, "pump", 8) && entity.liquidAmount < liquidCapacity){
			entity.liquid = tile.floor().liquidDrop;
			entity.liquidAmount += pumpspeed;
		}
		
		if(Timers.get(tile, "dump", 1)){
			tryDumpLiquid(tile);
		}
	}

}
