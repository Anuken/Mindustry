package io.anuke.mindustry.entities.enemies.types;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;

import static io.anuke.mindustry.Vars.tilesize;

public class BlastType extends EnemyType {

	public BlastType() {
		super("blastenemy");
		health = 30;
		speed = 0.8f;
		bullet = null;
		turretrotatespeed = 0f;
		mass = 0.8f;
		stopNearCore = false;
	}

	@Override
	public void behavior(Enemy enemy){

		float range = 10f;
		float ox = 0, oy = 0;

		if(enemy.target instanceof TileEntity){
			TileEntity e = (TileEntity)enemy.target;
			range = (e.tile.block().width * tilesize) /2f + 8f;
			ox = e.tile.block().getPlaceOffset().x;
			oy = e.tile.block().getPlaceOffset().y;
		}
		
		if(enemy.target != null && enemy.target.distanceTo(enemy.x - ox, enemy.y - oy) < range){
			explode(enemy);
		}
	}
	
	@Override
	public void onDeath(Enemy enemy, boolean force){
		if(force) explode(enemy);
		super.onDeath(enemy, force);
	}
	
	void explode(Enemy enemy){
		Bullet b = new Bullet(BulletType.blast, enemy, enemy.x, enemy.y, 0).add();
		b.damage = BulletType.blast.damage + (enemy.tier-1) * 40;
		enemy.damage(999);
		enemy.remove();
	}

}
