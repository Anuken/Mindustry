package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class MortarEnemy extends Enemy{

	public MortarEnemy(int spawn) {
		super(spawn);
		
		maxhealth = 200;
		speed = 0.2f;
		reload = 100f;
		bullet = BulletType.shell;
		turretrotatespeed = 0.15f;
		rotatespeed = 7f;
		range = 120f;
	}

}
