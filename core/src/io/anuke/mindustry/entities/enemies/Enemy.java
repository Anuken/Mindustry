package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Syncable;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Timer;

public class Enemy extends DestructibleEntity implements Syncable{
	protected Interpolator<Enemy> inter = new Interpolator<>(SyncType.enemy);

	public final EnemyType type;

	public Timer timer = new Timer(5);
	public float idletime = 0f;
	public int lane;
	public int node = -1;

	public Enemy spawner;
	public int spawned;

	public float angle;
	public Vector2 velocity = new Vector2();
	public Entity target;
	public int tier = 1;

	public Enemy(EnemyType type){
		this.type = type;
	}

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
	public void onDeath(){
		type.onDeath(this);
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
		return add(Vars.control.enemyGroup);
	}

	@Override
	public Interpolator<Enemy> getInterpolator() {
		return inter;
	}

	public void shoot(BulletType bullet){
		shoot(bullet, 0);
	}

	public void shoot(BulletType bullet, float rotation){

		if(!(Net.active() && Net.client())) {
			Angles.translation(angle + rotation, type.length);
			Bullet out = new Bullet(bullet, this, x + Angles.x(), y + Angles.y(), this.angle + rotation).add();
			out.damage = (int) ((bullet.damage * (1 + (tier - 1) * 1f)) * Vars.multiplier);
			type.onShoot(this, bullet, rotation);

			if(Net.active() && Net.server()){
				Vars.netServer.handleBullet(bullet, this, x + Angles.x(), y + Angles.y(), this.angle + rotation, (short)out.damage);
			}
		}
	}
}
