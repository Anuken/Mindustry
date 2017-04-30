package io.anuke.moment.entities;

import com.badlogic.gdx.math.Vector2;

import io.anuke.moment.Control;
import io.anuke.moment.Moment;
import io.anuke.moment.ai.Pathfind;
import io.anuke.moment.world.TileType;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.USound;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.util.Timers;

public class Enemy extends DestructibleEntity{
	public static int amount = 0;
	
	public Vector2 direction = new Vector2();
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
	
	public Enemy(int spawn){
		this.spawn = spawn;
		
		hitsize = 5;
		
		maxhealth = 30;
		heal();
		
		amount ++;
	}
	
	void move(){
		Vector2 vec  = Pathfind.find(this);
		vec.sub(x, y).setLength(speed);
		
		Moment.module(Control.class).tryMove(this, vec.x*delta, vec.y*delta);
		
		//if(Timers.get(this, 10))
			target = TileType.findTileTarget(x, y, null, range, false);
		
		if(target != null){
			if(Timers.get(this, reload)){
				shoot();
				USound.play(shootsound);
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
		USound.play("explosion");
		remove();
	}
	
	@Override
	public void removed(){
		amount --;
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
