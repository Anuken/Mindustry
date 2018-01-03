package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.enemies.EnemyType;

public class FastEnemy extends EnemyType {

	public FastEnemy() {
		super("fastenemy");
		
		speed = 0.73f;
		reload = 25;
		mass = 0.2f;
		
		health = 40;
	}

}
