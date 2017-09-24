package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class RapidEnemy extends Enemy{

	public RapidEnemy(int spawn) {
		super(spawn);
		
		reload = 8;
		bullet = BulletType.smallfast;
		rotatespeed = 30f;
		maxhealth = 260;
		hitsize = 8;
		speed = 0.27f;
		heal();
		
		range = 70;
	}

}
