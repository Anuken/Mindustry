package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;

import static io.anuke.mindustry.Vars.world;

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
	public void behavior(Enemy enemy){
		if(enemy.distanceTo(world.getCore().worldx(),
				world.getCore().worldy()) <= 90f){

			if(Timers.get(this, "spawn", spawnTime) && enemy.spawned < maxSpawn){
				enemy.tr.trns(enemy.angle, 20f);

				Enemy s = new Enemy(EnemyTypes.fast);
				s.lane = enemy.lane;
				s.tier = enemy.tier;
				s.spawner = enemy;
				s.set(enemy.x + enemy.tr.x, enemy.y + enemy.tr.y);
				s.add();

				Effects.effect(Fx.spawn, enemy);
				enemy.spawned ++;
			}

		}
	}


	public void onShoot(Enemy enemy, BulletType type, float rotation){
		Effects.effect(Fx.largeCannonShot, enemy.x + enemy.tr.x, enemy.y + enemy.tr.y, enemy.angle);
		Effects.shake(3f, 3f, enemy);
	}

}
