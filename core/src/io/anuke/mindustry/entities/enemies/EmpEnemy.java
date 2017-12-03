package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class EmpEnemy extends Enemy{

	public EmpEnemy() {
		
		speed = 0.3f;
		reload = 60;
		maxhealth = 210;
		range = 80f;
		bullet = BulletType.emp;
		turretrotatespeed = 0.1f;
		
		heal();
	}

}
