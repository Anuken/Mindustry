package io.anuke.mindustry.entities;

import io.anuke.ucore.core.Draw;

public class FlameEnemy extends Enemy{

	public FlameEnemy(int spawn) {
		super(spawn);
		
		speed = 0.35f;
		
		maxhealth = 150;
		reload = 6;
		bullet = BulletType.flameshot;
		shootsound = "flame";
		
		range = 40;
		
		heal();
	}
	
	@Override
	public void draw(){
		Draw.rect("firemech", x, y, direction.angle()-90);
	}

}
