package io.anuke.mindustry.entities.enemies;

public class FastEnemy extends Enemy{

	public FastEnemy() {
		
		speed = 0.73f;
		reload = 25;
		mass = 0.2f;
		
		maxhealth = 40;
		heal();
	}

}
