package io.anuke.mindustry.entities;

import io.anuke.ucore.core.Draw;

public class FastEnemy extends Enemy{

	public FastEnemy(int spawn) {
		super(spawn);
		
		speed = 0.7f;
		reload = 30;
		
		maxhealth = 20;
		heal();
	}
	
	@Override
	public void draw(){
		Draw.rect("fastmech", x, y, direction.angle()-90);
	}

}
