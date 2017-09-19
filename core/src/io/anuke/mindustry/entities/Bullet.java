package io.anuke.mindustry.entities;

import static io.anuke.mindustry.Vars.tilesize;

import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
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
	
	public float drawSize(){
		return 8;
	}
	
	@Override
	public void update(){
		
		int tilex = Mathf.scl2(x, tilesize);
		int tiley = Mathf.scl2(y, tilesize);
		Tile tile = World.tile(tilex, tiley);
		
		if(tile != null && tile.entity != null &&
				tile.entity.collide(this) && !tile.entity.dead){
			tile.entity.collision(this);
			remove();
			type.removed(this);
		}
		
		super.update();
	}
	
	@Override
	public boolean collides(SolidEntity other){
		if(owner instanceof TileEntity && other instanceof Player)
			return false;
		return super.collides(other);
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
