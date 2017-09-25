package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.util.Angles;

public class TankEnemy extends Enemy{

	public TankEnemy(int spawn) {
		super(spawn);
		
		maxhealth = 400;
		speed = 0.2f;
		reload = 90f;
		bullet = BulletType.small;
		length = 3f;
	}
	
	void shoot(){
		vector.set(length, 0).rotate(direction.angle());
		
		Angles.shotgun(3, 8f, direction.angle(), f->{
			Bullet out = new Bullet(bullet, this, x+vector.x, y+vector.y, f).add();
			out.damage = (int)(damage*Vars.multiplier);
		});
		
		
	}

}
