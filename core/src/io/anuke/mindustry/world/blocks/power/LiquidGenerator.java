package io.anuke.mindustry.world.blocks.power;

import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.ItemGenerator.ItemGeneratorEntity;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.values.LiquidFilterValue;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public abstract class LiquidGenerator extends PowerGenerator {
	protected float minEfficiency = 0.2f;
	protected float powerPerLiquid = 0.13f;
	/**Maximum liquid used per frame.*/
	protected float maxLiquidGenerate = 0.4f;
	protected Effect generateEffect = BlockFx.generatespark;

	public LiquidGenerator(String name) {
		super(name);
		liquidCapacity = 30f;
		hasLiquids = true;
	}

	@Override
	public void setStats() {
		super.setStats();

		stats.add(BlockStat.inputLiquid, new LiquidFilterValue(item -> getEfficiency(item) >= minEfficiency));
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
		return new ItemGeneratorEntity();
	}

	/**Returns an efficiency value for the specified liquid.
	 * Greater efficiency means more power generation.
	 * If a liquid's efficiency is below {@link #minEfficiency}, it is not accepted.*/
	protected abstract float getEfficiency(Liquid liquid);
}
