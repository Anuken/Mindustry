package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.graphics.Draw;

public class LiquidJunction extends Conduit{

	public LiquidJunction(String name) {
		super(name);
		update = true;
		solid = true;
		rotate = false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy());
	}
	
	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		int dir = source.relativeTo(tile.x, tile.y);
		dir = (dir+4)%4;
		Tile to = tile.getNearby(dir);

		if(to.block() instanceof LiquidBlock && ((LiquidBlock)to.block()).acceptLiquid(to, tile, liquid, amount))
			((LiquidBlock)to.block()).handleLiquid(to, tile, liquid, amount);
	}

	@Override
	public boolean acceptLiquid(Tile dest, Tile source, Liquid liquid, float amount){
		int dir = source.relativeTo(dest.x, dest.y);
		dir = (dir+4)%4;
		Tile to = dest.getNearby(dir);
		return to != null && to.block() instanceof LiquidBlock &&
				((LiquidBlock)to.block()).acceptLiquid(to, dest, liquid, amount);
	}
}
