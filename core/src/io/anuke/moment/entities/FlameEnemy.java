package io.anuke.moment.entities;

import io.anuke.ucore.core.Draw;

public class FlameEnemy extends Enemy{

	public FlameEnemy(int spawn) {
		super(spawn);
		speed = 0.3f;
		
		maxhealth = 150;
		reload = 6;
		bullet = BulletType.flameshot;
		
		range = 40;
		
		heal();
	}
	
	@Override
	public void draw(){
		Draw.rect("firemech", x, y, direction.angle()-90);
	}

}
