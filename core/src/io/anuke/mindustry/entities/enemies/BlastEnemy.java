package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;

public class BlastEnemy extends Enemy{

	public BlastEnemy(int spawn) {
		super(spawn);
		maxhealth = 30;
		speed = 0.65f;
		bullet = null;
		turretrotatespeed = 0f;
		
		heal();
	}
	
	void move(){
		super.move();
		if(target != null && target.distanceTo(this) < 10f){
			Bullet b = new Bullet(BulletType.blast, this, x, y, 0).add();
			b.damage = BulletType.blast.damage + (tier-1) * 50;
			damage(999);
		}
	}

}
