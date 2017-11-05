package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class RapidEnemy extends Enemy{

	public RapidEnemy(int spawn) {
		super(spawn);
		
		reload = 8;
		bullet = BulletType.purple;
		rotatespeed = 30f;
		maxhealth = 260;
		speed = 0.27f;
		heal();
		hitbox.setSize(8f);
		
		range = 70;
	}

}
