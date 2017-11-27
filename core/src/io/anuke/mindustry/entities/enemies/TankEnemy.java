package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.util.Angles;

public class TankEnemy extends Enemy{

	public TankEnemy(int spawn) {
		super(spawn);
		
		maxhealth = 350;
		speed = 0.2f;
		reload = 90f;
		rotatespeed = 0.06f;
		bullet = BulletType.small;
		length = 3f;
		mass = 1.4f;
	}
	
	void shoot(){
		Angles.translation(angle, 8f);
		
		Angles.shotgun(3, 8f, angle, f->{
			Bullet out = new Bullet(bullet, this, x+vector.x, y+vector.y, f).add();
			out.damage = (int)(damage*Vars.multiplier);
		});
		
		
	}

}
