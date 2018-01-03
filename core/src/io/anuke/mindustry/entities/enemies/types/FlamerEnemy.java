package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.EnemyType;

public class FlamerEnemy extends EnemyType {

	public FlamerEnemy() {
		super("flamerenemy");
		
		speed = 0.35f;
		health = 150;
		reload = 6;
		bullet = BulletType.flameshot;
		shootsound = "flame";
		mass = 1.5f;
		range = 40;
	}

}
