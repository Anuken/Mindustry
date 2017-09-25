package io.anuke.mindustry.entities.enemies;

public class FastEnemy extends Enemy{

	public FastEnemy(int spawn) {
		super(spawn);
		
		speed = 0.7f;
		reload = 30;
		
		maxhealth = 30;
		heal();
	}

}
