package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;

public class FortressEnemy extends Enemy{
	static int maxSpawn = 6;
	
	float spawnTime = 190;
	boolean deployed;

	public FortressEnemy() {
		
		speed = 0.25f;
		reload = 90;
		maxhealth = 700;
		range = 70f;
		bullet = BulletType.yellowshell;
		hitbox.setSize(10f);
		turretrotatespeed = rotatespeed = 0.08f;
		length = 7f;
		mass = 7f;
		
		heal();
	}
	
	@Override
	public void move(){
		super.move();
		
		if(deployed){
			
			if(Timers.get(this, "spawn", spawnTime) && spawned < maxSpawn){
				Angles.translation(angle, 20f);
				
				FastEnemy enemy = new FastEnemy();
				enemy.lane = lane;
				enemy.tier = this.tier;
				enemy.spawner = this;
				enemy.set(x + Angles.x(), y + Angles.y());
				Effects.effect(Fx.spawn, enemy);
				enemy.add();
				spawned ++;
			}
		}else if(distanceTo(Vars.control.getCore().worldx(), 
				Vars.control.getCore().worldy()) <= 90f){
			deployed = true;
			speed = 0.001f;
		}
	}


	void onShoot(BulletType type, float rotation){
		Effects.effect(Fx.largeCannonShot, x + Angles.x(), y + Angles.y(), angle);
		Effects.shake(3f, 3f, this);
	}

}
