package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.BulletEntity;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.*;

public class Bullet extends BulletEntity<BulletType>{
	private static Vector2 vector = new Vector2();

	public Timer timer = new Timer(3);
	public Team team;

	public static Bullet create(BulletType type, Unit owner, float x, float y, float angle){
		return create(type, owner, owner.team, x, y, angle);
	}

	public static Bullet create (BulletType type, Entity owner, Team team, float x, float y, float angle){
		Bullet bullet = Pools.obtain(Bullet.class);
		bullet.type = type;
		bullet.owner = owner;

		bullet.velocity.set(0, type.speed).setAngle(angle);
		bullet.velocity.add(owner instanceof Unit ? ((Unit)owner).velocity : Vector2.Zero);
		bullet.hitbox.setSize(type.hitsize);

		bullet.team = team;
		bullet.type = type;
		bullet.set(x, y);
		return bullet.add();
	}

	public static Bullet create(BulletType type, Bullet parent, float x, float y, float angle){
		return create(type, parent.owner, parent.team, x, y, angle);
	}

	private Bullet(){}
	
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

	@Override
	public float drawSize(){
		return 8;
	}

	public boolean collidesTiles(){
		return true;
	}

	@Override
	public boolean collides(SolidEntity other){
		return super.collides(other);
	}

	@Override
	public void collision(SolidEntity other, float x, float y){
		super.collision(other, x, y);

		if(other instanceof Unit){
			Unit unit = (Unit)other;
			unit.velocity.add(vector.set(other.x, other.y).sub(x, y).setLength(type.knockback / unit.getMass()));
			unit.applyEffect(type.status, type.statusIntensity);
		}
	}

	@Override
	public void update(){
		super.update();

		if (type.hitTiles && collidesTiles()) {
			world.raycastEach(world.toTile(lastX), world.toTile(lastY), world.toTile(x), world.toTile(y), (x, y) -> {

				Tile tile = world.tile(x, y);
				if (tile == null) return false;
				tile = tile.target();

				if (tile.entity != null && tile.entity.collide(this) && !tile.entity.dead && tile.entity.tile.getTeam() != team) {
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
	public float getDamage(){
		return damage == -1 ? type.damage : damage;
	}

	@Override
	public void reset() {
		super.reset();
		timer.clear();
		team = null;
	}

	@Override
	public void removed() {
		Pools.free(this);
	}

	@Override
	public Bullet add(){
		return super.add(bulletGroup);
	}
}
