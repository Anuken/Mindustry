package io.anuke.mindustry.entities.bullets;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.graphics.fx.Fx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.BaseBulletType;

public abstract class BulletType extends BaseBulletType<Bullet>{
	
	private BulletType(float speed, int damage){
		this.speed = speed;
		this.damage = damage;
	}
	
	@Override
	public void hit(Bullet b, float hitx, float hity){
		Effects.effect(Fx.hit, hitx, hity);
	}
}
