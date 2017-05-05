package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.World;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.util.Timers;

public class Enemy extends DestructibleEntity{
	public Vector2 direction = new Vector2();
	public Tile[] path;
	public float xvelocity, yvelocity;
	public float speed = 0.3f;
	public int node = -1;
	public Entity target;
	public int spawn;
	public float reload = 40;
	public float range = 60;
	public BulletType bullet = BulletType.small;
	public float length = 4;
	public float rotatespeed = 8f;
	public String shootsound = "enemyshoot";
	public boolean dead = false;
	
	public Enemy(int spawn){
		this.spawn = spawn;
		
		hitsize = 5;
		
		maxhealth = 30;
		heal();
	}
	
	void move(){
		Vector2 vec  = Pathfind.find(this);
		vec.sub(x, y).setLength(speed);
		
		move(vec.x*delta, vec.y*delta, Vars.tilesize-4);
		
		if(Timers.get(this, 15)){
			target = World.findTileTarget(x, y, null, range, false);
		
			//no tile found
			if(target == null)
				target = Entities.getClosest(x, y, range, e->{
					return e instanceof Player;
				});
		
		}
		
		if(target != null){
			if(Timers.get(hashCode()+"reload", reload)){
				shoot();
				Effects.sound(shootsound, this);
			}
		}
	}
	
	public void shoot(){
		
		vector.set(length, 0).rotate(direction.angle());
		new Bullet(bullet, this, x+vector.x, y+vector.y, direction.angle()).add();
	}
	
	@Override
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && !(((Bullet)other).owner instanceof Enemy);
	}
	
	@Override
	public void onDeath(){
		Effects.effect("explosion", this);
		Effects.shake(3f, 4f);
		Effects.sound("explosion", this);
		remove();
		dead = true;
	}
	
	@Override
	public void removed(){
		if(!dead)
		Vars.enemies --;
	}
	
	@Override
	public void update(){
		float lastx = x, lasty = y;
		
		move();
		
		xvelocity = x - lastx;
		yvelocity = y-lasty;
		
		if(target == null){
			direction.add(xvelocity, yvelocity);
			direction.limit(speed*rotatespeed);
		}else{
			float angle = angleTo(target);
			direction.lerp(vector.set(0, 1).setAngle(angle), 0.25f);
		}
	}
	
	@Override
	public void draw(){
		Draw.rect("mech1", x, y, direction.angle()-90);
	}
}
