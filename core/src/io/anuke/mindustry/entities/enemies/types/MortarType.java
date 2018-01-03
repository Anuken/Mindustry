package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.EnemyType;

public class MortarType extends EnemyType {

	public MortarType() {
		super("mortarenemy");
		
		health = 200;
		speed = 0.25f;
		reload = 100f;
		bullet = BulletType.shell;
		turretrotatespeed = 0.15f;
		rotatespeed = 0.05f;
		range = 120f;
		mass = 1.2f;
	}

}
