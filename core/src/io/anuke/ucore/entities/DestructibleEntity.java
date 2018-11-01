package io.anuke.ucore.entities;

import io.anuke.ucore.util.Mathf;

public abstract class DestructibleEntity extends SolidEntity{
	public transient float maxhealth;
	public transient boolean dead;
	public float health;
	
	public void onHit(SolidEntity entity){}
	public void onDeath(){}
	
	public boolean isDead(){
		return dead;
	}
	
	@Override
	public boolean collides(SolidEntity other){
		return other instanceof Damager;
	}
	
	@Override
	public void collision(SolidEntity other, float x, float y){
		if(other instanceof Damager){
			onHit(other);
			damage(((Damager)other).getDamage());
		}
	}
	
	public void damage(float amount){
		health -= amount;
		if(health <= 0 && !dead){
			onDeath();
			dead = true;
		}
	}
	
	public void setMaxHealth(float health){
		maxhealth = health;
		heal();
	}
	
	public void clampHealth(){
		health = Mathf.clamp(health, 0, maxhealth);
	}
	
	public float healthfrac(){
		return (float)health/maxhealth;
	}
	
	public void heal(){
		dead = false;
		health = maxhealth;
	}
}
