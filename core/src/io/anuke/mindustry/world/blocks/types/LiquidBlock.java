package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.modules.LiquidModule;
import io.anuke.ucore.graphics.Draw;

public class LiquidBlock extends Block{
	protected final int timerFlow = timers++;
	
	public LiquidBlock(String name) {
		super(name);
		rotate = true;
		update = true;
	}
	
	@Override
	public void draw(Tile tile){
		LiquidModule mod = tile.entity.liquid;
		
		Draw.rect(name() + "bottom", tile.worldx(), tile.worldy(), tile.getRotation() * 90);
		
		if(mod.amount > 0.01f){
			Draw.color(mod.liquid.color);
			Draw.alpha(mod.amount / liquidCapacity);
			Draw.rect("conduitliquid", tile.worldx(), tile.worldy(), tile.getRotation() * 90);
			Draw.color();
		}
		
		Draw.rect(name() + "top", tile.worldx(), tile.worldy(), tile.getRotation() * 90);
	}
	
	@Override
	public void update(Tile tile){
		if(tile.entity.liquid.amount > 0.01f && tile.entity.timer.get(timerFlow, 1)){
			tryMoveLiquid(tile, tile.getNearby(tile.getRotation()));
		}
	}
}
