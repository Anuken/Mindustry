package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class TankEnemy extends Enemy{

	public TankEnemy(int spawn) {
		super(spawn);
		
		maxhealth = 400;
		speed = 0.2f;
		reload = 140f;
		bullet = BulletType.iron;
	}

}
