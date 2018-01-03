package io.anuke.mindustry.entities.enemies.types;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.ucore.util.Tmp;

public class BlastEnemy extends EnemyType {

	public BlastEnemy() {
		super("blastenemy");
		health = 30;
		speed = 0.7f;
		bullet = null;
		turretrotatespeed = 0f;
		mass = 0.8f;
		stopNearCore = false;
	}

	@Override
	public void move(Enemy enemy){
		super.move(enemy);

		float range = 10f;
		Vector2 offset = Tmp.v3.setZero();

		if(enemy.target instanceof TileEntity){
			TileEntity e = (TileEntity)enemy.target;
			range = (e.tile.block().width * Vars.tilesize) /2f + 8f;
			offset.set(e.tile.block().getPlaceOffset());
		}
		
		if(enemy.target != null && enemy.target.distanceTo(enemy.x - offset.x, enemy.y - offset.y) < range){
			explode(enemy);
		}
	}
	
	@Override
	public void onDeath(Enemy enemy){
		super.onDeath(enemy);
		explode(enemy);
	}
	
	void explode(Enemy enemy){
		Bullet b = new Bullet(BulletType.blast, enemy, enemy.x, enemy.y, 0).add();
		b.damage = BulletType.blast.damage + (enemy.tier-1) * 40;
		enemy.damage(999);
		enemy.remove();
	}

}
