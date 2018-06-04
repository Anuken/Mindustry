package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.traits.TeamTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.entities.trait.VelocityTrait;
import io.anuke.ucore.entities.impl.BulletEntity;
import io.anuke.ucore.util.Timer;

import static io.anuke.mindustry.Vars.bulletGroup;
import static io.anuke.mindustry.Vars.world;

public class Bullet extends BulletEntity<BulletType> implements TeamTrait{
	private static Vector2 vector = new Vector2();

	public Timer timer = new Timer(3);
	public Team team;

	public static Bullet create(BulletType type, TeamTrait owner, float x, float y, float angle){
		return create(type, owner, owner.getTeam(), x, y, angle);
	}

	public static Bullet create (BulletType type, Entity owner, Team team, float x, float y, float angle){
		Bullet bullet = Pools.obtain(Bullet.class);
		bullet.type = type;
		bullet.owner = owner;

		bullet.velocity.set(0, type.speed).setAngle(angle);
		bullet.velocity.add(owner instanceof VelocityTrait ? ((VelocityTrait)owner).getVelocity() : Vector2.Zero);
		bullet.hitbox.setSize(type.hitsize);

		bullet.team = team;
		bullet.type = type;
		bullet.set(x, y);
		bullet.add();
		return bullet;
	}

	public static Bullet create(BulletType type, Bullet parent, float x, float y, float angle){
		return create(type, parent.owner, parent.team, x, y, angle);
	}

	private Bullet(){}

	public boolean collidesTiles(){
		return true; //TODO make artillery and such not do this
	}

	@Override
	public Team getTeam() {
		return team;
	}

	@Override
	public void draw(){
		type.draw(this);
	}

	@Override
	public float drawSize(){
		return 8;
	}

	@Override
	public boolean collides(SolidTrait other){
		return super.collides(other);
	}

	@Override
	public void collision(SolidTrait other, float x, float y){
		super.collision(other, x, y);

		if(other instanceof Unit){
			Unit unit = (Unit)other;
			unit.getVelocity().add(vector.set(other.getX(), other.getY()).sub(x, y).setLength(type.knockback / unit.getMass()));
			unit.applyEffect(type.status, type.statusIntensity);
		}
	}

	@Override
	public void update(){
		super.update();

		if (type.hitTiles && collidesTiles()) {
			world.raycastEach(world.toTile(lastPosition().x), world.toTile(lastPosition().y), world.toTile(x), world.toTile(y), (x, y) -> {

				Tile tile = world.tile(x, y);
				if (tile == null) return false;
				tile = tile.target();

				if (tile.entity != null && tile.entity.collide(this) && !tile.entity.isDead() && tile.entity.tile.getTeam() != team) {
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
	public EntityGroup targetGroup() {
		return bulletGroup;
	}
}
