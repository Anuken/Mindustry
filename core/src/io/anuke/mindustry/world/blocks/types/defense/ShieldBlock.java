package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Timers;

public class ShieldBlock extends PowerBlock{
	private static boolean debugShield = false;
	
	public float shieldRadius = 40f;
	public float powerDrain = 0.005f;

	public ShieldBlock(String name) {
		super(name);
		voltage = powerDrain;
	}

	@Override
	public void update(Tile tile){
		ShieldEntity entity = tile.entity();

		if(entity.shield == null){
			entity.shield = new Shield(tile);
			if(debugShield)
				entity.shield.add();
		}

		if(entity.power > powerDrain * Timers.delta()){
			if(!entity.shield.active && entity.power > powerDrain * Timers.delta() * 10f){
				entity.shield.add();
			}

			entity.power -= powerDrain * Timers.delta();
		}else{
			if(entity.shield.active && !debugShield){
				entity.shield.removeDelay();
			}
		}

	}

	@Override
	public TileEntity getEntity(){
		return new ShieldEntity();
	}

	static class ShieldEntity extends PowerEntity{
		Shield shield;
	}
}
