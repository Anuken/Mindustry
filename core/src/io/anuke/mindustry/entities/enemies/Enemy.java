package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.SyncEntity;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.enemyGroup;

public class Enemy extends SyncEntity {
	public EnemyType type;

	public Timer timer = new Timer(5);
	public float idletime = 0f;
	public int lane;
	public int node = -1;

	public Enemy spawner;
	public int spawned;

	public float angle;
	public Vector2 velocity = new Vector2();
	public Entity target;
	public float hitTime;
	public int tier = 1;
	public Vector2 totalMove = new Vector2();

	public Enemy(EnemyType type){
		this.type = type;
	}

	/**internal constructor used for deserialization, DO NOT USE*/
	public Enemy(){}

	@Override
	public void update(){
		type.update(this);
	}

	@Override
	public void draw(){
		type.draw(this);
	}

	@Override
	public void drawOver(){
		type.drawOver(this);
	}

	@Override
	public float drawSize(){
		return 14;
	}

	@Override
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && !(((Bullet) other).owner instanceof Enemy);
	}

	@Override
	public void damage(int amount){
		super.damage(amount);
		hitTime = EnemyType.hitDuration;
	}

	@Override
	public void onDeath(){
		type.onDeath(this, false);
	}

	@Override
	public void removed(){
		type.removed(this);
	}

	@Override
	public void added(){
		hitbox.setSize(type.hitsize);
		hitboxTile.setSize(type.hitsizeTile);
		maxhealth = type.health * tier;

		heal();
	}
	
	@Override
	public Enemy add(){
		return add(enemyGroup);
	}

	@Override
	public void writeSpawn(ByteBuffer buffer) {
		buffer.put(type.id);
		buffer.put((byte)lane);
		buffer.put((byte)tier);
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.putShort((short)health);
	}

	@Override
	public void readSpawn(ByteBuffer buffer) {
		type = EnemyType.getByID(buffer.get());
		lane = buffer.get();
		tier = buffer.get();
		x = buffer.getFloat();
		y = buffer.getFloat();
		health = buffer.getShort();
	}

	@Override
	public void write(ByteBuffer data) {
		data.putFloat(x);
		data.putFloat(y);
		data.putShort((short)(angle*2));
		data.putShort((short)health);
	}

	@Override
	public void read(ByteBuffer data) {

		float x = data.getFloat();
		float y = data.getFloat();
		short angle = data.getShort();
		short health = data.getShort();

		interpolator.target.set(x, y);
		interpolator.targetrot = angle/2f;
		this.health = health;
	}

	@Override
	public void interpolate() {
		Interpolator i = interpolator;
		if(i.target.dst(x, y) > 16){
			set(i.target.x, i.target.y);
		}

		x = Mathf.lerpDelta(x, i.target.x, 0.4f);
		y = Mathf.lerpDelta(y, i.target.y, 0.4f);
		angle = Mathf.lerpAngDelta(angle, i.targetrot, 0.6f);
	}

	public void shoot(BulletType bullet){
		shoot(bullet, 0);
	}

	public void shoot(BulletType bullet, float rotation){

		if(!(Net.client())) {
			Angles.translation(angle + rotation, type.length);
			Bullet out = new Bullet(bullet, this, x + Angles.x(), y + Angles.y(), this.angle + rotation).add();
			out.damage = (int) ((bullet.damage * (1 + (tier - 1) * 1f)));
			type.onShoot(this, bullet, rotation);

			if(Net.server()){
				NetEvents.handleBullet(bullet, this, x + Angles.x(), y + Angles.y(), this.angle + rotation, (short)out.damage);
			}
		}
	}
}
