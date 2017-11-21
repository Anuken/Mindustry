package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.Fx;
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
	protected int time = 5;
	protected int capacity = 5;

	public Drill(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void update(Tile tile){
		
		if(tile.floor() == resource && Timers.get(tile, "drill", 60 * time) && tile.entity.totalItems() < capacity){
			offloadNear(tile, result);
			Effects.effect(Fx.spark, tile.worldx(), tile.worldy());
		}

		if(Timers.get(tile, "dump", 30)){
			tryDump(tile);
		}
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
