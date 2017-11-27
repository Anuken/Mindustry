package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;

public class FlamerEnemy extends Enemy{

	public FlamerEnemy(int spawn) {
		super(spawn);
		
		speed = 0.35f;
		
		maxhealth = 150;
		reload = 6;
		bullet = BulletType.flameshot;
		shootsound = "flame";
		mass = 1.5f;
		
		range = 40;
		
		heal();
	}

}
