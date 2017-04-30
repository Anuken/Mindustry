package io.anuke.moment.entities;

import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;

public class Bullet extends BulletEntity{
	BulletType type;
	
	public Bullet(BulletType type, Entity owner, float x, float y, float angle){
		super(owner, type.speed, angle);
		set(x, y);
		this.lifetime = type.lifetime;
		this.type = type;
	}
	
	public void draw(){
		type.draw(this);
	}
	
	@Override
	public void collision(SolidEntity other){
		super.collision(other);
		type.collide(this);
	}

	@Override
	public int getDamage(){
		return type.damage;
	}

}
