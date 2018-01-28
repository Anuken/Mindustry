package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.Shield;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects;
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
		voltage = powerDrain;
		powerCapacity = 80f;
	}
	
	@Override
	public void getStats(Array<String> list){
		super.getStats(list);
		list.add("[powerinfo]Power/second: " + Strings.toFixed(powerDrain*60, 2));
		list.add("[powerinfo]Power Drain/damage: " + Strings.toFixed(powerPerDamage, 2));
		list.add("[powerinfo]Shield Radius: " + (int)shieldRadius);
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
			if(!entity.shield.active){
				entity.shield.add();
			}

			entity.power -= powerDrain * Timers.delta();
		}else{
			if(entity.shield.active && !(Vars.infiniteAmmo && Vars.debug)){
				entity.shield.removeDelay();
			}
		}
		
		entity.shield.radius = Mathf.lerp(entity.shield.radius, Math.min(entity.power / powerCapacity * radiusScale, maxRadius), Timers.delta() * 0.05f);

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
		Effects.effect(bullet.damage > 5 ? Fx.shieldhit : Fx.laserhit, bullet);
		if(!headless) renderer.addShieldHit(bullet.x, bullet.y);
		
		entity.power -= bullet.getDamage() * powerPerDamage;
	}

	static class ShieldEntity extends PowerEntity{
		Shield shield;
	}
}
