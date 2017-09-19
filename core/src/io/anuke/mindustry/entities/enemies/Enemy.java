package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.*;

public class Enemy extends DestructibleEntity{
	protected float speed = 0.3f;
	protected float reload = 40;
	protected float range = 60;
	protected float length = 4;
	protected float rotatespeed = 8f;
	protected BulletType bullet = BulletType.small;
	protected String shootsound = "enemyshoot";
	
	public Tile[] path;
	public int spawn;
	public int node = -1;
	
	public Vector2 direction = new Vector2();
	public float xvelocity, yvelocity;
	public Entity target;
	
	
	public Enemy(int spawn){
		this.spawn = spawn;
		
		hitsize = 5;
		
		maxhealth = 30;
		heal();
	}
	
	public float drawSize(){
		return 12;
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
			if(Timers.get(this, "reload", reload*Vars.multiplier)){
				shoot();
				Effects.sound(shootsound, this);
			}
		}
	}
	
	void shoot(){
		vector.set(length, 0).rotate(direction.angle());
		Bullet out = new Bullet(bullet, this, x+vector.x, y+vector.y, direction.angle()).add();
		out.damage = (int)(bullet.damage*Vars.multiplier);
	}
	
	public void findClosestNode(){
		Pathfind.find(this);
		
		int index = 0;
		int cindex = -1;
		float dst = Float.MAX_VALUE;
		
		
		for(Tile tile : path){
			if(Vector2.dst(tile.worldx(), tile.worldy(), x, y) < dst){
				dst = Vector2.dst(tile.worldx(), tile.worldy(), x, y);
				cindex = index;
			}
			
			index ++;
		}
		
		node = cindex;
	}
	
	@Override
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && !(((Bullet)other).owner instanceof Enemy);
	}
	
	@Override
	public void onDeath(){
		Effects.effect("explosion", this);
		Effects.shake(3f, 4f, this);
		Effects.sound("explosion", this);
		remove();
		dead = true;
	}
	
	@Override
	public void removed(){
		if(!dead)
			Vars.control.enemyDeath();
	}
	
	@Override
	public void update(){
		float lastx = x, lasty = y;
		
		move();
		
		xvelocity = (x - lastx) / Timers.delta();
		yvelocity = (y - lasty) / Timers.delta();
		
		if(target == null){
			direction.add(xvelocity * Timers.delta(), yvelocity * Timers.delta());
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
