package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.ucore.util.Angles;

public class TankType extends EnemyType {

	public TankType() {
		super("tankenemy");
		
		health = 350;
		speed = 0.24f;
		reload = 90f;
		rotatespeed = 0.06f;
		bullet = BulletType.small;
		length = 3f;
		mass = 1.4f;
		length = 8f;
	}

	@Override
	public void shoot(Enemy enemy){
		super.shoot(enemy);

		Angles.shotgun(3, 8f, enemy.angle, f -> enemy.shoot(bullet, f));
	}

}
