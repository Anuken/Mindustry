package io.anuke.mindustry.entities;

import io.anuke.mindustry.Moment;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.TileType;
import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Mathf;

public class Bullet extends BulletEntity{
	BulletType type;
	
	public Bullet(BulletType type, Entity owner, float x, float y, float angle){
		super(type, owner, angle);
		set(x, y);
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
			type.removed(this);
		}
		
		super.update();
	}
	
	@Override
	public void collision(SolidEntity other){
		super.collision(other);
		type.removed(this);
	}

	@Override
	public int getDamage(){
		return type.damage;
	}

}
