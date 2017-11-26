package io.anuke.mindustry.entities;

import static io.anuke.mindustry.Vars.tilesize;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.*;
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
		Tile tile = Vars.world.tile(tilex, tiley);
		TileEntity targetEntity = null;
		
		if(tile != null){
			if(tile.entity != null && tile.entity.collide(this) && !tile.entity.dead){
				targetEntity = tile.entity;
			}else{
				//make sure to check for linked block collisions
				//TODO move this to the block class?
				Tile linked = tile.getLinked();
				if(linked != null &&
						linked.entity != null && linked.entity.collide(this) && !linked.entity.dead){
					targetEntity = linked.entity;
				}
			}
		}
		
		if(targetEntity != null){
			
			targetEntity.collision(this);
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
		return damage == -1 ? type.damage : damage;
	}
	
	@Override
	public Bullet add(){
		return super.add(Entities.getGroup(Bullet.class));
	}

}
