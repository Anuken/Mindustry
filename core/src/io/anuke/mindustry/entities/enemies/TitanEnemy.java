package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;

public class TitanEnemy extends Enemy{

	public TitanEnemy(int spawn) {
		super(spawn);
		
		speed = 0.4f;
		reload = 30;
		maxhealth = 210;
		range = 80f;
		bullet = BulletType.emp;
		
		heal();
	}
	
	@Override
	void updateShooting(){
		if(Timers.getTime(this, "salvo") < 30){
			if(Timers.get(this, "salvoShoot", 6)){
				shoot(BulletType.shot2);
			}
			
			Timers.get(this, "salvo", 200);
		}
		
		if(Timers.get(this, "shotgun", 50)){
			Angles.shotgun(5, 10f, 0f, f->{
				shoot(BulletType.purple, f);
			});
		}
	}

}
