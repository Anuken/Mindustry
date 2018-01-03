package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.EnemyType;

public class RapidType extends EnemyType {

	public RapidType() {
		super("rapidenemy");
		
		reload = 8;
		bullet = BulletType.purple;
		rotatespeed = 0.08f;
		health = 260;
		speed = 0.33f;
		hitsize = 8f;
		mass = 3f;
		range = 70;
	}

}
