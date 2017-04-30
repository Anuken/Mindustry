package io.anuke.moment.entities;

import io.anuke.moment.Moment;
import io.anuke.moment.world.Tile;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Mathf;

public class Bullet extends BulletEntity{
	BulletType type;
	
	public Bullet(BulletType type, Entity owner, float x, float y, float angle){
		super(owner, type.speed, angle);
		set(x, y);
		this.lifetime = type.lifetime;
		this.type = type;
	}
	
	public void draw(){
		type.draw(this);
	}
	
	@Override
	public void update(){
		
		int tilex = Mathf.scl2(x, TileType.tilesize);
		int tiley = Mathf.scl2(y, TileType.tilesize);
		Tile tile = Moment.i.tile(tilex, tiley);
		
		if(tile != null && tile.entity != null &&
				tile.entity.collide(this) && !tile.entity.dead){
			tile.entity.collision(this);
			remove();
			type.collide(this);
		}
		
		super.update();
	}
	
	@Override
	public void collision(SolidEntity other){
		super.collision(other);
		type.collide(this);
	}

	@Override
	public int getDamage(){
		return type.damage;
	}

}
