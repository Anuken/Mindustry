package io.anuke.mindustry.world.blocks.types.defense;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;

import static io.anuke.mindustry.Vars.headless;
import static io.anuke.mindustry.Vars.renderer;

public class ShieldBlock extends PowerBlock{
	public float shieldRadius = 40f;
	public float powerDrain = 0.005f;
	public float powerPerDamage = 0.06f;
	public float maxRadius = 40f;
	public float radiusScale = 300f;
	
	public ShieldBlock(String name) {
		super(name);
		powerCapacity = 80f;
		hasItems = false;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add("powersecond", Strings.toFixed(powerDrain*60, 2));
		stats.add("powerdraindamage", Strings.toFixed(powerPerDamage, 2));
		stats.add("shieldradius", (int)shieldRadius);
	}

	@Override
	public void update(Tile tile){
		ShieldEntity entity = tile.entity();

		if(entity.shield == null){
			entity.shield = new Shield(tile);
			if(Vars.infiniteAmmo && Vars.debug)
				entity.shield.add();
		}

		if(entity.power.amount > powerPerDamage){
			if(!entity.shield.active){
				entity.shield.add();
			}

			entity.power.amount -= powerDrain * Timers.delta();
		}else{
			if(entity.shield.active && !(Vars.infiniteAmmo && Vars.debug)){
				entity.shield.removeDelay();
			}
		}
		
		entity.shield.radius = Mathf.lerp(entity.shield.radius, Math.min(entity.power.amount / powerCapacity * radiusScale, maxRadius), Timers.delta() * 0.05f);

	}

	@Override
	public TileEntity getEntity(){
		return new ShieldEntity();
	}
	
	public void handleBullet(Tile tile, BulletEntity bullet){
		ShieldEntity entity = tile.entity();
		
		if(entity.power.amount < bullet.getDamage() * powerPerDamage){
			return;
		}
		
		bullet.remove();
		//Effects.effect(bullet.damage > 5 ? BulletFx.shieldhit : BulletFx.laserhit, bullet);
		if(!headless) renderer.addShieldHit(bullet.x, bullet.y);
		
		entity.power.amount -= bullet.getDamage() * powerPerDamage;
	}

	static class ShieldEntity extends TileEntity{
		Shield shield;
	}
}
