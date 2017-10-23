package io.anuke.mindustry.world.blocks.types.production;

import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidBlock;

public class LiquidPowerGenerator extends LiquidBlock{
	public Liquid generateLiquid;
	public float generatePower;
	public float generateAmount = 1f;

	public LiquidPowerGenerator(String name) {
		super(name);
	}
	
	@Override
	public void update(Tile tile){
		LiquidEntity entity = tile.entity();
		
		if(entity.liquidAmount >= generateAmount){
			entity.liquidAmount -= generateAmount;
			//TODO actually add power
		}
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return liquid == generateLiquid && super.acceptLiquid(tile, source, liquid, amount);
	}
}
