package io.anuke.mindustry.world.blocks.types.production;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidAcceptor;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;

public class LiquidPowerGenerator extends Generator implements LiquidAcceptor{
	public int generateTime = 5;
	public Liquid generateLiquid;
	/**Power to generate per generateInput.*/
	public float generatePower = 1f;
	/**How much liquid to consume to get one generatePower.*/
	public float inputLiquid = 1f;
	public float liquidCapacity = 30f;
	public String generateEffect = "generate";

	public LiquidPowerGenerator(String name) {
		super(name);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		LiquidPowerEntity entity = tile.entity();
		
		if(entity.liquid == null) return;
		
		Vector2 offset = getPlaceOffset();
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		Draw.rect("blank", tile.worldx() + offset.x, tile.worldy() + offset.y, 2, 2);
		Draw.color();
	}
	
	@Override
	public void update(Tile tile){
		LiquidPowerEntity entity = tile.entity();
		
		if(entity.liquidAmount >= inputLiquid && Timers.get(tile, "consume", generateTime)){
			entity.liquidAmount -= inputLiquid;
			entity.power += generatePower;
			
			Vector2 offset = getPlaceOffset();
			Effects.effect(generateEffect, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
		
		if(Timers.get(tile, "consume", generateTime)){
			distributePower(tile);
		}
	}
	
	@Override
	public TileEntity getEntity(){
		return new LiquidPowerEntity();
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		LiquidPowerEntity entity = tile.entity();
		
		if(liquid != generateLiquid){
			return false;
		}
		
		return entity.liquidAmount + amount < liquidCapacity && (entity.liquid == liquid || entity.liquidAmount <= 0.01f);
	}
	
	@Override
	public void handleLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		LiquidPowerEntity entity = tile.entity();
		entity.liquid = liquid;
		entity.liquidAmount += amount;
	}
	
	@Override
	public float getLiquid(Tile tile){
		LiquidPowerEntity entity = tile.entity();
		return entity.liquidAmount;
	}

	@Override
	public float getLiquidCapacity(Tile tile){
		return liquidCapacity;
	}
	
	public static class LiquidPowerEntity extends PowerEntity{
		public Liquid liquid;
		public float liquidAmount;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			super.write(stream);
			stream.writeByte(liquid == null ? -1 : liquid.ordinal());
			stream.writeByte((byte)(liquidAmount));
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			byte ordinal = stream.readByte();
			liquid = ordinal == -1 ? null : Liquid.values()[ordinal];
			liquidAmount = stream.readByte();
		}
	}

}
