package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.reflect.ClassReflection;

import io.anuke.mindustry.Shaders.Outline;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.World;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.*;
import io.anuke.ucore.util.Mathf;

public class Enemy extends DestructibleEntity{
	public final static Color[] tierColors = {Color.YELLOW, Color.ORANGE, Color.RED, Color.MAGENTA};
	public final static int maxtier = 4;
	
	protected float speed = 0.3f;
	protected float reload = 32;
	protected float range = 60;
	protected float length = 4;
	protected float rotatespeed = 7f;
	protected float turretrotatespeed = 0.2f;
	protected BulletType bullet = BulletType.small;
	protected String shootsound = "enemyshoot";
	protected int damage;
	
	public Tile[] path;
	public int spawn;
	public int node = -1;
	
	public Vector2 direction = new Vector2();
	public float xvelocity, yvelocity;
	public Entity target;
	public int tier = 1;
	
	
	public Enemy(int spawn){
		this.spawn = spawn;
		
		hitsize = 5;
		
		maxhealth = 60;
		heal();
	}
	
	public float drawSize(){
		return 12;
	}
	
	void move(){
		Vector2 vec  = Pathfind.find(this);
		vec.sub(x, y).setLength(speed);
		
		move(vec.x*Timers.delta(), vec.y*Timers.delta(), Vars.tilesize-4);
		
		if(Timers.get(this, 15)){
			target = World.findTileTarget(x, y, null, range, false);
		
			//no tile found
			if(target == null){
				target = Entities.getClosest(x, y, range, e->{
					return e instanceof Player;
				});
			}
		}
		
		if(target != null && bullet != null){
			if(Timers.get(this, "reload", reload*Vars.multiplier)){
				shoot();
				Effects.sound(shootsound, this);
			}
		}
	}
	
	void shoot(){
		vector.set(length, 0).rotate(direction.angle());
		Bullet out = new Bullet(bullet, this, x+vector.x, y+vector.y, direction.angle()).add();
		out.damage = (int)(damage*Vars.multiplier);
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
		
		int x2 = path[node].x, y2 = path[node].y;
		
		if(World.raycast(Mathf.scl2(x, Vars.tilesize), Mathf.scl2(y, Vars.tilesize), x2, y2) != null){
			Timers.run(Mathf.random(15f), ()->{
				set(x2 * Vars.tilesize, y2 * Vars.tilesize);
			});
		}
	}
	
	@Override
	public void added(){
		if(bullet != null){
			damage = (int)(bullet.damage * (1 + (tier - 1) * 1f));
		}
		
		maxhealth *= tier;
		speed += 0.04f*tier + Mathf.range(0.1f);
		reload /= Math.max(tier / 1.5f, 1f);
		range += tier*5;
		
		heal();
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
		if(!dead){
			Vars.control.enemyDeath();
		}
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
			direction.lerp(vector.set(0, 1).setAngle(angle), turretrotatespeed * Timers.delta());
		}
	}
	
	@Override
	public void draw(){
		
		String region = ClassReflection.getSimpleName(getClass()).toLowerCase() + "-t" + Mathf.clamp(tier, 1, 3);
		
		//TODO is this really necessary?
		Graphics.getShader(Outline.class).color.set(tierColors[tier-1]);
		Graphics.getShader(Outline.class).region = Draw.region(region);
		
		Graphics.shader(Outline.class);
		Draw.color();
		Draw.rect(region, x, y, direction.angle()-90);
		Graphics.shader();
	}
}
