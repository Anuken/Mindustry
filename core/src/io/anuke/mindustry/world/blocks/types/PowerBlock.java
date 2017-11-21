package io.anuke.mindustry.world.blocks.types;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.util.Mathf;

public abstract class PowerBlock extends Block implements PowerAcceptor{
	public float powerCapacity = 10f;
	public float voltage = 0.001f;
	
	public PowerBlock(String name) {
		super(name);
		update = true;
		solid = true;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power Capacity: " + powerCapacity);
	}
	
	@Override
	public void drawPixelOverlay(Tile tile){
		PowerEntity entity = tile.entity();
		
		float fract = (float)entity.power / powerCapacity;
		if(fract > 0)
			fract = Mathf.clamp(fract + 0.2f, 0.24f, 1f);
		
		Vector2 offset = getPlaceOffset();
		
		Vars.renderer.drawBar(Color.YELLOW, tile.worldx() + offset.x, 
				tile.worldy() + Vars.tilesize * height/2f + 2 + offset.y, fract);
	}
	
	/**Tries adding all the power with no remainder, returns success.*/
	public boolean tryAddPower(Tile tile, float amount){
		PowerEntity entity = tile.entity();
		
		if(entity.power + amount <= powerCapacity){
			entity.power += amount;
			return true;
		}
		return false;
	}
	
	@Override
	public boolean acceptsPower(Tile tile){
		PowerEntity entity = tile.entity();
		
		return entity.power + 0.001f <= powerCapacity;
	}
	
	//TODO voltage requirement so blocks need specific voltage
	@Override
	public float addPower(Tile tile, float amount){
		if(amount < voltage){
			return amount;
		}
		PowerEntity entity = tile.entity();
		
		float canAccept = Math.min(powerCapacity - entity.power, amount);
		
		entity.power += canAccept;
		
		return canAccept;
	}
	
	@Override
	public void setPower(Tile tile, float power){
		PowerEntity entity = tile.entity();
		entity.power = power;
	}
	
	@Override
	public TileEntity getEntity(){
		return new PowerEntity();
	}
	
	public static class PowerEntity extends TileEntity{
		public float power;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeFloat(power);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			power = stream.readFloat();
		}
	}
}
