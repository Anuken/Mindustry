package io.anuke.mindustry.world.blocks.types.production;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidAcceptor;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Strings;

public class LiquidPowerGenerator extends Generator implements LiquidAcceptor{
	public int generateTime = 5;
	public Liquid generateLiquid;
	/**Power to generate per generateInput.*/
	public float generatePower = 1f;
	/**How much liquid to consume to get one generatePower.*/
	public float inputLiquid = 5f;
	public float liquidCapacity = 30f;
	public Effect generateEffect = Fx.generate;

	public LiquidPowerGenerator(String name) {
		super(name);
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[liquidinfo]Liquid Capacity: " + (int)liquidCapacity);
		list.add("[liquidinfo]Generation: " + Strings.toFixed(generatePower / inputLiquid, 2) + " power/liquid");
		list.add("[liquidinfo]Input: " + generateLiquid);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		LiquidPowerEntity entity = tile.entity();
		
		if(entity.liquid == null) return;
		
		Draw.color(entity.liquid.color);
		Draw.alpha(entity.liquidAmount / liquidCapacity);
		drawLiquidCenter(tile);
		Draw.color();
	}
	
	public void drawLiquidCenter(Tile tile){
		Vector2 offset = getPlaceOffset();
		Draw.rect("black", tile.worldx() + offset.x, tile.worldy() + offset.y, 2, 2);
	}
	
	@Override
	public void update(Tile tile){
		LiquidPowerEntity entity = tile.entity();
		
		//TODO don't generate when full of energy
		if(entity.liquidAmount >= inputLiquid && entity.power + generatePower < powerCapacity 
				&& Timers.get(tile, "consume", generateTime)){
			entity.liquidAmount -= inputLiquid;
			entity.power += generatePower;
			
			Vector2 offset = getPlaceOffset();
			Effects.effect(generateEffect, tile.worldx() + offset.x, tile.worldy() + offset.y);
		}
		
		distributeLaserPower(tile);
		
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
