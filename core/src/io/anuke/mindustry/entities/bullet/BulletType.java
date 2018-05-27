package io.anuke.mindustry.entities.bullet;

import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.BulletFx;
import io.anuke.mindustry.entities.StatusEffect;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.BaseBulletType;

public abstract class BulletType extends BaseBulletType<Bullet>{
	/**Knockback in velocity.*/
	public float knockback;
	/**Whether this bullet hits tiles.*/
	public boolean hitTiles = true;
	/**Status effect applied on hit.*/
	public StatusEffect status = StatusEffects.none;
	/**Intensity of applied status effect in terms of duration.*/
	public float statusIntensity = 0.5f;
	/**What fraction of armor is pierced, 0-1*/
	public float armorPierce = 0f;

	public BulletType(float speed, float damage){
		this.speed = speed;
		this.damage = damage;
		lifetime = 40f;
		hiteffect = BulletFx.hitBulletSmall;
		despawneffect = BulletFx.despawn;
	}
	
	@Override
	public void hit(Bullet b, float hitx, float hity){
		Effects.effect(hiteffect, hitx, hity, b.angle());
	}

	@Override
	public void despawned(Bullet b){
		Effects.effect(despawneffect, b.x, b.y, b.angle());
	}
}
