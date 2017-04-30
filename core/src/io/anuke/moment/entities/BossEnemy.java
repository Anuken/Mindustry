package io.anuke.moment.entities;

import io.anuke.ucore.core.Draw;

public class BossEnemy extends Enemy{

	public BossEnemy(int spawn) {
		super(spawn);
		
		reload = 8;
		bullet = BulletType.smallfast;
		rotatespeed = 30f;
		maxhealth = 260;
		hitsize = 8;
		speed = 0.27f;
		heal();
		
		range = 70;
	}
	
	@Override
	public void draw(){
		Draw.rect("bossmech", x, y, direction.angle()-90);
	}

}
