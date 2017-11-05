package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class EmpEnemy extends Enemy{

	public EmpEnemy(int spawn) {
		super(spawn);
		
		speed = 0.4f;
		reload = 50;
		maxhealth = 210;
		range = 80f;
		bullet = BulletType.emp;
		
		heal();
	}

}
