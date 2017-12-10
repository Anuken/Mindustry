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
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class LiquidPowerGenerator extends Generator implements LiquidAcceptor{
	public int generateTime = 15;
	public Liquid generateLiquid;
	public float powerPerLiquid = 0.13f;
	/**Maximum liquid used per frame.*/
	public float maxLiquidGenerate = 0.4f;
	public float liquidCapacity = 30f;
	public Effect generateEffect = Fx.generatespark;

	public LiquidPowerGenerator(String name) {
		super(name);
		outputOnly = true;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[liquidinfo]Liquid Capacity: " + (int)liquidCapacity);
		list.add("[liquidinfo]Generation: " + Strings.toFixed(powerPerLiquid, 2) + " power/liquid");
		list.add("[liquidinfo]Max liquid: " + Strings.toFixed(maxLiquidGenerate*60f, 2) + " liquid/s");
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
		Draw.rect("blank", tile.worldx() + offset.x, tile.worldy() + offset.y, 2, 2);
	}
	
	@Override
	public void update(Tile tile){
		LiquidPowerEntity entity = tile.entity();
		
		if(entity.liquidAmount > 0){
			float used = Math.min(entity.liquidAmount, maxLiquidGenerate * Timers.delta());
			used = Math.min(used, (powerCapacity - entity.power)/powerPerLiquid);
			
			entity.liquidAmount -= used;
			entity.power += used * powerPerLiquid;
			
			if(used > 0.001f && Mathf.chance(0.05 * Timers.delta())){
				Vector2 offset = getPlaceOffset();
				Effects.effect(generateEffect, tile.worldx() + offset.x + Mathf.range(3f), tile.worldy() + offset.y + Mathf.range(3f));
			}
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
