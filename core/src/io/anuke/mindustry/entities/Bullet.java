package io.anuke.mindustry.entities;

import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.*;

public class Bullet extends BulletEntity{
	public Timer timer = new Timer(3);
	
	public Bullet(BulletType type, Entity owner, float x, float y, float angle){
		super(type, owner, angle);
		set(x, y);
		this.type = type;
	}
	
	public void draw(){
		//interpolate position linearly at low tick speeds
		if(SyncEntity.isSmoothing()){
			x += threads.getFramesSinceUpdate() * velocity.x;
			y += threads.getFramesSinceUpdate() * velocity.y;

			type.draw(this);

			x -= threads.getFramesSinceUpdate() * velocity.x;
			y -= threads.getFramesSinceUpdate() * velocity.y;
		}else{
			type.draw(this);
		}
	}
	
	public float drawSize(){
		return 8;
	}

	public boolean collidesTiles(){
		return owner instanceof Enemy;
	}
	
	@Override
	public void update(){
		super.update();

		if (collidesTiles()) {
			world.raycastEach(world.toTile(lastX), world.toTile(lastY), world.toTile(x), world.toTile(y), (x, y) -> {

				Tile tile = world.tile(x, y);
				if (tile == null) return false;
				tile = tile.target();

				if (tile.entity != null && tile.entity.collide(this) && !tile.entity.dead) {
					tile.entity.collision(this);
					remove();
					type.hit(this);

					return true;
				}

				return false;
			});
		}
	}

	@Override
	public int getDamage(){
		return damage == -1 ? type.damage : damage;
	}
	
	@Override
	public Bullet add(){
		return super.add(bulletGroup);
	}
}
