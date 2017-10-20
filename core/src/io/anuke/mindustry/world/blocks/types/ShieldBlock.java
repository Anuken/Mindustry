package io.anuke.mindustry.world.blocks.types;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Timers;

public class ShieldBlock extends PowerBlock{
	public float shieldRadius = 40f;
	public float powerDrain = 0.01f;

	public ShieldBlock(String name) {
		super(name);
	}
	
	@Override
	public void update(Tile tile){
		ShieldEntity entity = tile.entity();
		
		if(entity.shield == null){
			entity.shield = new Shield();
		}
		
		if(power > powerDrain * Timers.delta()){
			if(!entity.shield.active){
				entity.shield.add();
			}
			
			power -= powerDrain * Timers.delta();
		}else{
			if(entity.shield.active){
				entity.shield.remove();
			}
		}
	}
	
	@Override
	public TileEntity getEntity(){
		return new ShieldEntity();
	}
	
	static class ShieldEntity extends TileEntity{
		Shield shield;
	}
}
