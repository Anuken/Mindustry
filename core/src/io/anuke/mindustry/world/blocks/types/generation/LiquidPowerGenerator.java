package io.anuke.mindustry.world.blocks.types.generation;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class LiquidPowerGenerator extends Generator{
	public int generateTime = 15;
	public Liquid generateLiquid;
	public float powerPerLiquid = 0.13f;
	/**Maximum liquid used per frame.*/
	public float maxLiquidGenerate = 0.4f;
	public Effect generateEffect = Fx.generatespark;

	public LiquidPowerGenerator(String name) {
		super(name);
		outputOnly = true;
		liquidCapacity = 30f;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("liquidcapacity", (int)liquidCapacity);
		stats.add("powerliquid", Strings.toFixed(powerPerLiquid, 2) + " power/liquid");
		stats.add("maxliquidsecond", Strings.toFixed(maxLiquidGenerate*60f, 2) + " liquid/s");
		stats.add("input", generateLiquid);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);

		TileEntity entity = tile.entity();
		
		if(entity.liquid.liquid == null) return;
		
		Draw.color(entity.liquid.liquid.color);
		Draw.alpha(entity.liquid.amount / liquidCapacity);
		drawLiquidCenter(tile);
		Draw.color();
	}
	
	public void drawLiquidCenter(Tile tile){
		Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
	}
	
	@Override
	public void update(Tile tile){
		TileEntity entity = tile.entity();
		
		if(entity.liquid.amount > 0){
			float used = Math.min(entity.liquid.amount, maxLiquidGenerate * Timers.delta());
			used = Math.min(used, (powerCapacity - entity.power.amount)/powerPerLiquid);
			
			entity.liquid.amount -= used;
			entity.power.amount += used * powerPerLiquid;
			
			if(used > 0.001f && Mathf.chance(0.05 * Timers.delta())){
				
				Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
			}
		}
		
		distributeLaserPower(tile);
		
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return liquid == generateLiquid && super.acceptLiquid(tile, source, liquid, amount);
	}
}
