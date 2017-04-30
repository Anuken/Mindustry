package io.anuke.moment.entities;

import com.badlogic.gdx.math.Vector2;

import io.anuke.moment.Control;
import io.anuke.moment.Moment;
import io.anuke.moment.ai.Pathfind;
import io.anuke.ucore.core.Draw;
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
		
		target = Entities.getClosest(x, y, 60, e->{
			return (e instanceof TileEntity || e instanceof Player);
		});
		
		if(target != null){
			if(Timers.get(this, reload))
				new Bullet(BulletType.small, this, x, y, direction.angle()).add();
		}
	}
	
	@Override
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && !(((Bullet)other).owner instanceof Enemy);
	}
	
	@Override
	public void onDeath(){
		Effects.effect("explosion", this);
		Effects.shake(3f, 4f);
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
			direction.limit(speed*8);
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
