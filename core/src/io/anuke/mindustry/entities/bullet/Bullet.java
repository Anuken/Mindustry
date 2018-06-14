package io.anuke.mindustry.entities.bullet;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.entities.traits.TeamTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.BulletEntity;
import io.anuke.ucore.entities.trait.Entity;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.entities.trait.VelocityTrait;
import io.anuke.ucore.util.Timer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.bulletGroup;
import static io.anuke.mindustry.Vars.world;

public class Bullet extends BulletEntity<BulletType> implements TeamTrait, SyncTrait{
	public static int typeID = -1;

	private static Vector2 vector = new Vector2();

	private Team team;

	public Timer timer = new Timer(3);

	public static void create (BulletType type, TeamTrait owner, float x, float y, float angle){
		create(type, owner, owner.getTeam(), x, y, angle);
	}

	public static void create (BulletType type, Entity owner, Team team, float x, float y, float angle){
		create(type, owner, team, x, y, angle, 1f);
	}

	public static void create (BulletType type, Entity owner, Team team, float x, float y, float angle, float velocityScl){
		Bullet bullet = Pools.obtain(Bullet.class);
		bullet.type = type;
		bullet.owner = owner;

		bullet.velocity.set(0, type.speed).setAngle(angle).scl(velocityScl);
		bullet.velocity.add(owner instanceof VelocityTrait ? ((VelocityTrait)owner).getVelocity() : Vector2.Zero);
		bullet.hitbox.setSize(type.hitsize);

		bullet.team = team;
		bullet.type = type;
		bullet.set(x, y);
		bullet.add();
	}

	public static void create(BulletType type, Bullet parent, float x, float y, float angle){
		create(type, parent.owner, parent.team, x, y, angle);
	}

	public static void create(BulletType type, Bullet parent, float x, float y, float angle, float velocityScl){
		create(type, parent.owner, parent.team, x, y, angle, velocityScl);
	}

	@Remote(called = Loc.server, in = In.entities)
	public static void createBullet(BulletType type, float x, float y, float angle){
		create(type, null, Team.none, x, y, angle);
	}

	/**Internal use only!*/
	public Bullet(){}

	public boolean collidesTiles(){
		return type.collidesTiles; //TODO make artillery and such not do this
	}

	@Override
	public int getTypeID() {
		return typeID;
	}

	@Override
	public boolean isSyncing(){
		return type.syncable;
	}

	@Override
	public void write(DataOutput data) throws IOException {
		data.writeFloat(x);
		data.writeFloat(y);
		data.writeFloat(velocity.x);
		data.writeFloat(velocity.y);
		data.writeByte(team.ordinal());
		data.writeByte(type.id);
	}

	@Override
	public void read(DataInput data, long time) throws IOException{
		x = data.readFloat();
		y = data.readFloat();
		velocity.x = data.readFloat();
		velocity.y = data.readFloat();
		team = Team.values()[data.readByte()];
		type = BulletType.getByID(data.readByte());
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
