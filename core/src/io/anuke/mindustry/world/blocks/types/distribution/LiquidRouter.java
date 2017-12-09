package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.utils.ObjectMap;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Draw;

public class LiquidRouter extends LiquidBlock{
	protected final int timerDump = timers++;
	
	private ObjectMap<Tile, Byte> lastmap = new ObjectMap<>();

	public LiquidRouter(String name) {
		super(name);
		rotate = false;
		solid = true;
	}
	
	@Override
	public void update(Tile tile){
		LiquidEntity entity = tile.entity();
		
		if(entity.timer.get(timerDump, 2) && entity.liquidAmount > 0){
			if(lastmap.get(tile, (byte)-1) != tile.getRotation()){
				tryMoveLiquid(tile, tile.getNearby()[tile.getRotation()]);
			}
			
			tile.setRotation((byte)((tile.getRotation() + 1) % 4));
		}
	}
	
	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		super.handleLiquid(tile, source, liquid, amount);
		lastmap.put(tile, (byte)tile.relativeTo(source.x, source.y));
	}
	
	@Override
	public void draw(Tile tile){
		LiquidEntity entity = tile.entity();
		Draw.rect(name(), tile.worldx(), tile.worldy());
		
		if(entity.liquid == null) return;
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color();
	}

}
