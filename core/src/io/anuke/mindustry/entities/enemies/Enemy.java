package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.util.*;

public class Enemy extends DestructibleEntity{
	public final static Color[] tierColors = { Color.valueOf("ffe451"), Color.valueOf("f48e20"), Color.valueOf("ff6757"), Color.valueOf("ff2d86") };
	public final static int maxtier = 4;
	public final static float maxIdle = 60*1.5f;
	public final static float maxIdleLife = 60f*13f; //13 seconds idle = death
	
	protected int timeid;
	protected Timer timer = new Timer(5);
	protected float speed = 0.4f;
	protected float reload = 32;
	protected float range = 60;
	protected float length = 4;
	protected float rotatespeed = 0.1f;
	protected float turretrotatespeed = 0.2f;
	protected boolean alwaysRotate = false;
	protected BulletType bullet = BulletType.small;
	protected String shootsound = "enemyshoot";
	protected int damage;
	protected Enemy spawner;
	protected int spawned = 0;
	protected float angle;
	protected boolean targetCore = false;
	protected boolean stopNearCore = true;
	protected float mass = 1f;
	protected String className;
	
	public float idletime = 0f;
	public int spawn;
	public int node = -1;
	public Tile[] path;

	public float xvelocity, yvelocity;
	public Entity target;
	public int tier = 1;
	
	protected final int timerTarget = timeid ++;
	protected final int timerReload = timeid ++;

	public Enemy() {
		hitbox.setSize(5f);
		hitboxTile.setSize(4f);

		maxhealth = 60;
		heal();
		
		className = ClassReflection.getSimpleName(getClass()).toLowerCase();
	}

	public float drawSize(){
		return 12;
	}

	void move(){
		Tile core = Vars.control.getCore();
		
		if(idletime > maxIdleLife){
			onDeath();
			return;
		}
		
		boolean nearCore = distanceTo(core.worldx(), core.worldy()) <= range - 18f && stopNearCore;
		Vector2 vec;
		
		if(nearCore){
			vec = Tmp.v1.setZero();
			if(targetCore) target = core.entity;
		}else{
			vec = Vars.world.pathfinder().find(this);
			vec.sub(x, y).limit(speed);
		}

		Vector2 shift = Tmp.v3.setZero();
		float shiftRange = hitbox.width + 2f;
		float avoidRange = shiftRange + 4f;
		float attractRange = avoidRange + 7f;
		float avoidSpeed = this.speed/2.7f;
		
		Entities.getNearby(Vars.control.enemyGroup, x, y, range, other -> {
			Enemy enemy = (Enemy)other;
			if(other == this) return;
			float dst = other.distanceTo(this);
			
			if(dst < shiftRange){
				float scl = Mathf.clamp(1.4f - dst / shiftRange) * enemy.mass * 1f/mass;
				shift.add((x - other.x) * scl, (y - other.y) * scl);
			}else if(dst < avoidRange){
				Tmp.v2.set((x - other.x), (y - other.y)).setLength(avoidSpeed);
				shift.add(Tmp.v2.scl(1.1f));
			}else if(dst < attractRange && !nearCore){
				Tmp.v2.set((x - other.x), (y - other.y)).setLength(avoidSpeed);
				shift.add(Tmp.v2.scl(-1));
			}
		});

		shift.limit(1f);
		vec.add(shift.scl(0.5f));

		move(vec.x * Timers.delta(), vec.y * Timers.delta());

		updateTargeting(nearCore);
	}
	
	void updateTargeting(boolean nearCore){
		if(target != null && target instanceof TileEntity && ((TileEntity)target).dead){
			target = null;
		}
		
		if(timer.get(timerTarget, 15) && !nearCore){
			target = Vars.world.findTileTarget(x, y, null, range, false);

			//no tile found
			if(target == null){
				target = Entities.getClosest(Entities.defaultGroup(), x, y, range, e -> e instanceof Player);
			}
		}else if(nearCore){
			target = Vars.control.getCore().entity;
		}

		if(target != null && bullet != null){
			updateShooting();
		}
	}

	void updateShooting(){
		if(timer.get(timerReload, reload * Vars.multiplier)){
			shoot(bullet);
			if(shootsound != null) Effects.sound(shootsound, this);
		}
	}

	void shoot(BulletType bullet){
		shoot(bullet, 0);
	}

	void shoot(BulletType bullet, float rotation){
		Angles.translation(angle + rotation, length);
		Bullet out = new Bullet(bullet, this, x + Angles.x(), y + Angles.y(), this.angle + rotation).add();
		out.damage = (int) (damage * Vars.multiplier);
	}
	
	/*
	public void findClosestNode(){
		int index = 0;
		int cindex = -1;
		float dst = Float.MAX_VALUE;
		UCore.log("Finding closest.");
		
		Tile[] clone = path.clone();
		UCore.log(clone.length);

		//find closest node index
		for(Tile tile : path){
			if(Vector2.dst(tile.worldx(), tile.worldy(), x, y) < dst){
				dst = Vector2.dst(tile.worldx(), tile.worldy(), x, y);
				cindex = index;
			}

			index++;
		}

		cindex = Math.max(cindex, 1);

		//set node to that index
		node = cindex;

		int x2 = path[node].x, y2 = path[node].y;

		//if the enemy can't move to that node right now, set its position to it
		if(Vars.world.raycast(Mathf.scl2(x, Vars.tilesize), Mathf.scl2(y, Vars.tilesize), x2, y2) != null){
			Timers.run(Mathf.random(15f), () -> {
				set(x2 * Vars.tilesize, y2 * Vars.tilesize);
			});
		}
	}*/

	@Override
	public void added(){
		if(bullet != null){
			damage = (int) (bullet.damage * (1 + (tier - 1) * 1f));
		}

		maxhealth *= tier;
		speed += 0.04f * tier /*+ Mathf.range(0.1f)*/;
		reload /= Math.max(tier / 1.5f, 1f);
		range += tier * 5;
		speed = Math.max(speed, 0.07f);

		heal();
	}

	@Override
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && !(((Bullet) other).owner instanceof Enemy);
	}

	@Override
	public void onDeath(){
		Effects.effect(Fx.explosion, this);
		Effects.shake(3f, 4f, this);
		Effects.sound("bang2", this);
		remove();
		dead = true;
	}

	@Override
	public void removed(){
		if(!dead){
			
			if(spawner != null){
				spawner.spawned --;
			}else{
				Vars.control.enemyDeath();
			}
		}
	}

	@Override
	public void update(){
		float lastx = x, lasty = y;

		move();

		xvelocity = (x - lastx) / Timers.delta();
		yvelocity = (y - lasty) / Timers.delta();
		
		float minv = 0.07f;
		
		if(xvelocity < minv && yvelocity < minv && node > 0 && target == null){
			idletime += Timers.delta();
		}else{
			idletime = 0;
		}
		
		if(Float.isNaN(angle)){
			angle = 0;
		}

		if(target == null || alwaysRotate){
			angle = Mathf.slerp(angle, 180f+Mathf.atan2(xvelocity, yvelocity), rotatespeed * Timers.delta());
		}else{
			angle = Mathf.slerp(angle, angleTo(target), turretrotatespeed * Timers.delta());
		}
	}

	@Override
	public void draw(){
		String region = className + "-t" + Mathf.clamp(tier, 1, 3);

		Shaders.outline.color.set(tierColors[tier - 1]);
		Shaders.outline.region = Draw.region(region);

		Shaders.outline.apply();

		Draw.rect(region, x, y, this.angle - 90);
		
		if(Vars.showPaths){
			Draw.color(Color.PURPLE);
			Draw.line(x, y, x + xvelocity*10f, y + yvelocity*10f);
			Draw.color();
		}
		
		Graphics.flush();
	}
	
	@Override
	public <T extends Entity> T add(){
		return (T) add(Vars.control.enemyGroup);
	}
}
