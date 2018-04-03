package io.anuke.mindustry.entities;

import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.BaseBulletType;

public abstract class BulletType extends BaseBulletType<Bullet>{
	public Effect hitEffect = BulletFx.hit;
	
	public BulletType(float speed, int damage){
		this.speed = speed;
		this.damage = damage;
	}
	
	@Override
	public void hit(Bullet b, float hitx, float hity){
		Effects.effect(hitEffect, hitx, hity, b.angle());
	}
}
