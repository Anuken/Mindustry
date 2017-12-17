package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.ucore.util.Tmp;

public class BlastEnemy extends Enemy{

	public BlastEnemy() {
		maxhealth = 30;
		speed = 0.7f;
		bullet = null;
		turretrotatespeed = 0f;
		mass = 0.8f;
		stopNearCore = false;
		
		heal();
	}
	
	void move(){
		super.move();
		float range = 10f;
		Vector2 offset = Tmp.v3.setZero();
		if(target instanceof TileEntity){
			TileEntity e = (TileEntity)target;
			range = (e.tile.block().width * Vars.tilesize) /2f + 8f;
			offset.set(e.tile.block().getPlaceOffset());
		}
		
		if(target != null && target.distanceTo(this.x - offset.x, this.y - offset.y) < range){
			explode();
		}
	}
	
	@Override
	public void onDeath(){
		super.onDeath();
		explode();
	}
	
	void explode(){
		Bullet b = new Bullet(BulletType.blast, this, x, y, 0).add();
		b.damage = BulletType.blast.damage + (tier-1) * 40;
		damage(999);
		remove();
	}

}
