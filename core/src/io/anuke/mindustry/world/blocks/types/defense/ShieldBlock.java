package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BulletEntity;

public class ShieldBlock extends PowerBlock{
	public float shieldRadius = 40f;
	public float powerDrain = 0.005f;
	public float powerPerDamage = 0.1f;
	
	public ShieldBlock(String name) {
		super(name);
		voltage = powerDrain;
	}

	@Override
	public void update(Tile tile){
		ShieldEntity entity = tile.entity();

		if(entity.shield == null){
			entity.shield = new Shield(tile);
			if(Vars.infiniteAmmo && Vars.debug)
				entity.shield.add();
		}

		if(entity.power > powerPerDamage){
			if(!entity.shield.active && entity.power > powerDrain * Timers.delta() * 10f){
				entity.shield.add();
			}

			entity.power -= powerDrain * Timers.delta();
		}else{
			if(entity.shield.active && !(Vars.infiniteAmmo && Vars.debug)){
				entity.shield.removeDelay();
			}
		}

	}

	@Override
	public TileEntity getEntity(){
		return new ShieldEntity();
	}
	
	public void handleBullet(Tile tile, BulletEntity bullet){
		ShieldEntity entity = tile.entity();
		
		if(entity.power < bullet.getDamage() * powerPerDamage){
			return;
		}
		
		bullet.remove();
		Effects.effect(Fx.laserhit, bullet);
		Vars.renderer.addShieldHit(bullet.x, bullet.y);
		
		entity.power -= bullet.getDamage() * powerPerDamage;
	}

	static class ShieldEntity extends PowerEntity{
		Shield shield;
	}
}
