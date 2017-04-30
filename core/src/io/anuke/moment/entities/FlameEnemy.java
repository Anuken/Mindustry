package io.anuke.moment.entities;

import io.anuke.ucore.core.Draw;

public class FlameEnemy extends Enemy{

	public FlameEnemy(int spawn) {
		super(spawn);
		speed = 0.25f;
		
		maxhealth = 100;
		reload = 6;
		bullet = BulletType.flameshot;
		
		range = 30;
		
		heal();
	}
	
	@Override
	public void draw(){
		Draw.rect("firemech", x, y, direction.angle()-90);
	}

}
