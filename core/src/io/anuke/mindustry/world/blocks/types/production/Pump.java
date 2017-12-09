package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class Pump extends LiquidBlock{
	protected final int timerPump = timers++;
	protected final int timerDump = timers++;
	
	protected float pumpAmount = 2f;
	protected float pumpTime = 8f;

	public Pump(String name) {
		super(name);
		rotate = false;
		solid = true;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[liquidinfo]Pump Speed: " + Strings.toFixed(60f/pumpTime*pumpAmount, 1) + "/s");
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return false;
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.worldx(), tile.worldy());
		
		LiquidEntity entity = tile.entity();
		
		if(entity.liquid == null) return;
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color();
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
				entity.timer.get(timerPump, pumpTime) && entity.liquidAmount < liquidCapacity){
			entity.liquid = tile.floor().liquidDrop;
			entity.liquidAmount += Math.min(pumpAmount, this.liquidCapacity - entity.liquidAmount);
		}
		
		if(entity.timer.get(timerDump, pumpTime)){
			tryDumpLiquid(tile);
		}
	}

}
