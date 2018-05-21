package io.anuke.mindustry.world.blocks.types.power;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.power.BurnerGenerator.BurnerEntity;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class LiquidBurnerGenerator extends PowerGenerator {
	protected float minEfficiency = 0.2f;
	protected float powerPerLiquid = 0.13f;
	/**Maximum liquid used per frame.*/
	protected float maxLiquidGenerate = 0.4f;
	protected Effect generateEffect = BlockFx.generatespark;

	public LiquidBurnerGenerator(String name) {
		super(name);
		liquidCapacity = 30f;
		hasLiquids = true;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("powerliquid", Strings.toFixed(powerPerLiquid, 2) + " power/liquid");
		stats.add("maxliquidsecond", Strings.toFixed(maxLiquidGenerate*60f, 2) + " liquid/s");
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);

		TileEntity entity = tile.entity();
		
		Draw.color(entity.liquids.liquid.color);
		Draw.alpha(entity.liquids.amount / liquidCapacity);
		drawLiquidCenter(tile);
		Draw.color();
	}
	
	public void drawLiquidCenter(Tile tile){
		Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
	}
	
	@Override
	public void update(Tile tile){
		TileEntity entity = tile.entity();
		
		if(entity.liquids.amount > 0){
			float powerPerLiquid = getEfficiency(entity.liquids.liquid)*this.powerPerLiquid;
			float used = Math.min(entity.liquids.amount, maxLiquidGenerate * Timers.delta());
			used = Math.min(used, (powerCapacity - entity.power.amount)/powerPerLiquid);
			
			entity.liquids.amount -= used;
			entity.power.amount += used * powerPerLiquid;
			
			if(used > 0.001f && Mathf.chance(0.05 * Timers.delta())){
				Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
			}
		}
		
		distributePower(tile);
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return getEfficiency(liquid) >= minEfficiency && super.acceptLiquid(tile, source, liquid, amount);
	}

	@Override
	public TileEntity getEntity() {
		return new BurnerEntity();
	}

	protected float getEfficiency(Liquid liquid){
		return liquid.flammability;
	}
}
