package io.anuke.mindustry.entities;

import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.BaseBulletType;

public abstract class BulletType extends BaseBulletType<Bullet>{
	
	public BulletType(float speed, int damage){
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
