package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.EnemyType;

public class EmpEnemy extends EnemyType {

	public EmpEnemy() {
		super("empenemy");
		
		speed = 0.3f;
		reload = 70;
		health = 210;
		range = 80f;
		bullet = BulletType.emp;
		turretrotatespeed = 0.1f;
	}

}
