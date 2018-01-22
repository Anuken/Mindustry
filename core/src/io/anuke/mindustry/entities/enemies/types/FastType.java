package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.enemies.EnemyType;

public class FastType extends EnemyType {

	public FastType() {
		super("fastenemy");
		
		speed = 0.73f;
		reload = 20;
		mass = 0.2f;
		
		health = 50;
	}

}
