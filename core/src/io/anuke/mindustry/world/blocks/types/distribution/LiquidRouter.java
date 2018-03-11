package io.anuke.mindustry.world.blocks.types.distribution;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.graphics.Draw;

public class LiquidRouter extends LiquidBlock{

	public LiquidRouter(String name) {
		super(name);
		rotate = false;
		solid = true;
		liquidFlowFactor = 2f;
		hasInventory = false;
	}
	
	@Override
	public void update(Tile tile){
		
		if(tile.entity.liquid.amount > 0){
			if(tile.getExtra() != tile.getRotation()){
				tryMoveLiquid(tile, tile.getNearby(tile.getRotation()));
			}
			
			tile.setRotation((byte)((tile.getRotation() + 1) % 4));
		}
	}
	
	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		super.handleLiquid(tile, source, liquid, amount);
		tile.setExtra(tile.relativeTo(source.x, source.y));
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy());
		
		Draw.color(tile.entity.liquid.liquid.color);
		Draw.alpha(tile.entity.liquid.amount / liquidCapacity);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color();
	}

}
