package io.anuke.mindustry.world.blocks.types.production;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.anuke.mindustry.entities.TileEntity;
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
		LiquidPowerEntity entity = tile.entity();
		
		if(entity.liquidAmount >= generateAmount){
			entity.liquidAmount -= generateAmount;
			//TODO actually add power
		}
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return liquid == generateLiquid && super.acceptLiquid(tile, source, liquid, amount);
	}
	
	@Override
	public TileEntity getEntity(){
		return new LiquidPowerEntity();
	}
	
	public static class LiquidPowerEntity extends LiquidEntity{
		public float power;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			super.write(stream);
			stream.writeFloat(power);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			power = stream.readFloat();
		}
	}
}
