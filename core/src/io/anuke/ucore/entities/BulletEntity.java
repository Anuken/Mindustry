package io.anuke.ucore.entities;

import com.badlogic.gdx.math.Vector2;

import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Scalable;

public abstract class BulletEntity<T extends BaseBulletType> extends SolidEntity implements Damager, Scalable{
	public T type;
	public Entity owner;
	public Vector2 velocity = new Vector2();
	public float time = 0f;
	/**-1 to use type's damage.*/
	public int damage = -1;
	
	public BulletEntity(){}
	
	public BulletEntity(T type, Entity owner, float angle){
		this.type = type;
		this.owner = owner;
		
		velocity.set(0, type.speed).setAngle(angle);
		hitbox.setSize(type.hitsize);
	}
	
	@Override
	public void update(){
		type.update(this);
		
		x += velocity.x*Timers.delta();
		y += velocity.y*Timers.delta();
		
		velocity.scl(1f - type.drag * Timers.delta());
		
		time += Timers.delta();

		time = Mathf.clamp(time, 0, type.lifetime);
		
		if(time >= type.lifetime){
			remove();
			type.despawned(this);
		}
	}

	@Override
	public float drawSize() {
		return type.drawSize;
	}

	@Override
	public void added() {
		type.init(this);
	}

	@Override
	public int getDamage(){
		return damage == -1 ? type.damage : damage;
	}
	
	@Override
	public void draw(){
		type.draw(this);
	}
	
	@Override
	public boolean collides(SolidEntity other){
		return other != owner && !(other instanceof Damager);
	}
	
	@Override
	public void collision(SolidEntity other, float x, float y){
		if(!type.pierce) remove();
		type.hit(this, x, y);
	}

	@Override
	public float fin() {
		return time/type.lifetime;
	}

	public void setVelocity(float speed, float angle){
		velocity.set(0, speed).setAngle(angle);
	}
	
	public void limit(float f){
		velocity.limit(f);
	}
	
	public void setAngle(float angle){
		velocity.setAngle(angle);
	}
	
	public float angle(){
		return velocity.angle();
	}
}
