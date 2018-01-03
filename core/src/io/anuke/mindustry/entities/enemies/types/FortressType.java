package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;

public class FortressType extends EnemyType {
	final int maxSpawn = 6;
	final float spawnTime = 190;

	public FortressType() {
		super("fortressenemy");
		
		speed = 0.25f;
		reload = 90;
		health = 700;
		range = 70f;
		bullet = BulletType.yellowshell;
		hitsize = 10f;
		turretrotatespeed = rotatespeed = 0.08f;
		length = 7f;
		mass = 7f;
	}
	
	@Override
	public void move(Enemy enemy){
		if(enemy.distanceTo(Vars.control.getCore().worldx(),
				Vars.control.getCore().worldy()) <= 90f){

			if(Timers.get(this, "spawn", spawnTime) && enemy.spawned < maxSpawn){
				Angles.translation(enemy.angle, 20f);

				Enemy s = new Enemy(EnemyTypes.fast); //TODO assign type!
				s.lane = enemy.lane;
				s.tier = enemy.tier;
				s.spawner = enemy;
				s.set(enemy.x + Angles.x(), enemy.y + Angles.y());
				s.add();

				Effects.effect(Fx.spawn, enemy);
				enemy.spawned ++;
			}

		}else {
			super.move(enemy);
		}
	}


	public void onShoot(Enemy enemy, BulletType type, float rotation){
		Effects.effect(Fx.largeCannonShot, enemy.x + Angles.x(), enemy.y + Angles.y(), enemy.angle);
		Effects.shake(3f, 3f, enemy);
	}

}
