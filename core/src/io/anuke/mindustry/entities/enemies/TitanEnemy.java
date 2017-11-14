package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class TitanEnemy extends Enemy{

	public TitanEnemy(int spawn) {
		super(spawn);
		
		speed = 0.1f;
		reload = 30;
		maxhealth = 400;
		range = 60f;
		bullet = BulletType.small;
		hitbox.setSize(7f);
		
		heal();
		
		Timers.reset(this, "salvo", 0);
		Timers.reset(this, "shotgun", 0);
		Timers.reset(this, "circle", 0);
	}
	
	@Override
	void updateShooting(){
		Timers.get(this, "salvo", 250);
		
		if(Timers.getTime(this, "salvo") < 60){
			if(Timers.get(this, "salvoShoot", 6)){
				shoot(BulletType.flame, Mathf.range(20f));
			}
		}
		
		if(Timers.get(this, "shotgun", 90)){
			Angles.shotgun(5, 10f, 0f, f->{
				shoot(BulletType.smallSlow, f);
			});
		}
		
		if(Timers.get(this, "circle", 200)){
			Angles.circle(8, f->{
				shoot(BulletType.smallSlow, f);
			});
		}
	}

}
