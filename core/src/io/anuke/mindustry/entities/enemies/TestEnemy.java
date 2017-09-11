package io.anuke.mindustry.entities.enemies;

import io.anuke.ucore.core.Timers;

public class TestEnemy extends Enemy{
	boolean dir = false;

	public TestEnemy(int spawn) {
		super(spawn);
		maxhealth = 99999;
		heal();
	}
	
	void move(){
		if(Timers.get(this, "asd", 300)){
			dir = !dir;
		}
		
		move(dir ? -0.3f * Timers.delta() : 0.3f * Timers.delta(),  0);
	}
}
