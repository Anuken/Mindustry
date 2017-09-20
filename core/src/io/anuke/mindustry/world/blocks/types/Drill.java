package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class Drill extends Block{
	protected Block resource;
	protected Item result;
	protected int time = 6;

	public Drill(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void update(Tile tile){

		if(tile.floor() == resource && Timers.get(tile, 60 * time)){
			offloadNear(tile, result);
			Effects.effect("spark", tile.worldx(), tile.worldy());
		}

		if(Timers.get(tile.hashCode() + "dump", 30)){
			tryDump(tile);
		}
	}

	@Override
	public String description(){
		return "Mines 1 "+resource.name+" every "+time+" seconds.";
	}
	
	@Override
	public void drawOver(Tile tile){
		
		if(tile.floor() != resource && resource != null){
			Draw.colorl(0.85f + Mathf.absin(Timers.time(), 6f, 0.15f));
			Draw.rect("cross", tile.worldx(), tile.worldy());
			Draw.color();
		}
	}

}
