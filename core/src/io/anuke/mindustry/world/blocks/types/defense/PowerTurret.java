package io.anuke.mindustry.world.blocks.types.defense;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerAcceptor;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

public class PowerTurret extends Turret implements PowerAcceptor{
	public float powerCapacity = 20f;
	public float powerUsed = 0.5f;

	public PowerTurret(String name) {
		super(name);
		ammo = null;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power Capacity: " + (int)powerCapacity);
		list.add("[powerinfo]Power/shot: " + Strings.toFixed(powerUsed, 1));
	}
	
	@Override
	public void drawSelect(Tile tile){
		Vector2 offset = getPlaceOffset();
		
		Draw.color(Color.GREEN);
		Draw.dashCircle(tile.worldx() + offset.x, tile.worldy() + offset.y, range);
		Draw.reset();
		
		drawPowerBar(tile);
	}
	
	public void drawPowerBar(Tile tile){
		Vector2 offset = getPlaceOffset();
		
		PowerTurretEntity entity = tile.entity();
		
		float fract = (float)entity.power / powerCapacity;
		if(fract > 0)
			fract = Mathf.clamp(fract, 0.24f, 1f);
		
		Vars.renderer.drawBar(Color.YELLOW, tile.worldx() + offset.x, tile.worldy() + 6 + offset.y, fract);
	}
	
	@Override
	public boolean hasAmmo(Tile tile){
		PowerTurretEntity entity = tile.entity();
		return entity.power >= powerUsed;
	}
	
	@Override
	public void consumeAmmo(Tile tile){
		PowerTurretEntity entity = tile.entity();
		entity.power -= powerUsed;
	}
	
	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		return false;
	}
	
	@Override
	public boolean acceptsPower(Tile tile){
		PowerTurretEntity entity = tile.entity();
		
		return entity.power + 0.001f <= powerCapacity;
	}
	
	@Override
	public float addPower(Tile tile, float amount){
		PowerTurretEntity entity = tile.entity();
		
		float canAccept = Math.min(powerCapacity - entity.power, amount);
		
		entity.power += canAccept;
		
		return canAccept;
	}
	
	@Override
	public void setPower(Tile tile, float power){
		PowerTurretEntity entity = tile.entity();
		entity.power = power;
	}
	
	@Override
	public TileEntity getEntity(){
		return new PowerTurretEntity();
	}
	
	public static class PowerTurretEntity extends TurretEntity{
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
