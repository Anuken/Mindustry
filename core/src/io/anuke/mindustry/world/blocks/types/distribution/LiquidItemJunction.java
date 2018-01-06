package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class LiquidItemJunction extends LiquidBlock{

	public LiquidItemJunction(String name) {
		super(name);
		rotate = true;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy(), tile.getRotation() * 90);
	}
	
	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		int dir = source.relativeTo(tile.x, tile.y);
		dir = (dir+4)%4;
		Tile to = tile.getNearby()[dir];
		
		((LiquidBlock)to.block()).handleLiquid(to, tile, liquid, amount);
	}

	@Override
	public boolean acceptLiquid(Tile dest, Tile source, Liquid liquid, float amount){
		int dir = source.relativeTo(dest.x, dest.y);
		dir = (dir+4)%4;
		
		if(dir+dest.getRotation() % 2 == 1) return false;
		
		Tile to = dest.getNearby()[dir];
		return to != null && to.block() != this && to.block() instanceof LiquidBlock &&
				((LiquidBlock)to.block()).acceptLiquid(to, dest, liquid, amount);
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		int dir = source.relativeTo(tile.x, tile.y);
		Tile to = tile.getNearby()[dir];
		
		Timers.run(15, ()->{
			if(to == null || to.entity == null) return;
			to.block().handleItem(item, to, tile);
		});
		
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		int dir = source.relativeTo(tile.x, tile.y);
		
		if((dir+ tile.getRotation()) % 2 == 0) return false;
		
		Tile to = tile.getNearby()[dir];
		return to != null && to.block().acceptItem(item, to, tile);
	}
}
