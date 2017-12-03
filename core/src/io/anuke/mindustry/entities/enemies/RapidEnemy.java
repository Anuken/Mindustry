package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class RapidEnemy extends Enemy{

	public RapidEnemy() {
		
		reload = 8;
		bullet = BulletType.purple;
		rotatespeed = 0.08f;
		maxhealth = 260;
		speed = 0.33f;
		heal();
		hitbox.setSize(8f);
		mass = 3f;
		
		range = 70;
	}

}
