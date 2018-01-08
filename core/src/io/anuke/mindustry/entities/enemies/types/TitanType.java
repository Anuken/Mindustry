package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class TitanType extends EnemyType {

	public TitanType() {
		super("titanenemy");
		
		speed = 0.26f;
		reload = 30;
		health = 430;
		range = 60f;
		bullet = BulletType.small;
		hitsize = 7f;
		mass = 4f;
	}
	
	@Override
	public void updateShooting(Enemy enemy){
		Timers.get(enemy, "salvo", 240);
		
		if(Timers.getTime(enemy, "salvo") < 60){
			if(Timers.get(enemy, "salvoShoot", 6)){
				enemy.shoot(BulletType.flameshot, Mathf.range(20f));
			}
		}
		
		if(Timers.get(enemy, "shotgun", 80)){
			Angles.shotgun(5, 10f, 0f, f->{
				enemy.shoot(BulletType.smallSlow, f);
			});
		}
		
		if(Timers.get(enemy, "circle", 200)){
			Angles.circle(8, f->{
				enemy.shoot(BulletType.smallSlow, f);
			});
		}
	}

}
