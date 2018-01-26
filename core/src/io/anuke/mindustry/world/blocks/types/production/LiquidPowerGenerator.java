package io.anuke.mindustry.world.blocks.types.production;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LiquidAcceptor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
		list.add("[liquidinfo]Power/Liquid: " + Strings.toFixed(powerPerLiquid, 2) + " power/liquid");
		list.add("[liquidinfo]Max liquid/second: " + Strings.toFixed(maxLiquidGenerate*60f, 2) + " liquid/s");
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
		Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
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
				
				Effects.effect(generateEffect, tile.drawx() + Mathf.range(3f), tile.drawy() + Mathf.range(3f));
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
		
		return liquid == generateLiquid && entity.liquidAmount + amount < liquidCapacity && (entity.liquid == liquid || entity.liquidAmount <= 0.01f);
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
			stream.writeByte(liquid == null ? -1 : liquid.id);
			stream.writeByte((byte)(liquidAmount));
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			byte id = stream.readByte();
			liquid = id == -1 ? null : Liquid.getByID(id);
			liquidAmount = stream.readByte();
		}
	}

}
