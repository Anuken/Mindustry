package io.anuke.mindustry.entities.enemies;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;

public class BlastEnemy extends Enemy{

	public BlastEnemy(int spawn) {
		super(spawn);
		maxhealth = 30;
		speed = 0.65f;
		bullet = null;
		turretrotatespeed = 0f;
		mass = 0.8f;
		
		heal();
	}
	
	void move(){
		super.move();
		float range = 10f;
		if(target instanceof TileEntity){
			TileEntity e = (TileEntity)target;
			range = (e.tile.block().width * Vars.tilesize) /2f + 6f;
		}
		
		if(target != null && target.distanceTo(this) < range){
			Bullet b = new Bullet(BulletType.blast, this, x, y, 0).add();
			b.damage = BulletType.blast.damage + (tier-1) * 40;
			damage(999);
		}
	}

}
