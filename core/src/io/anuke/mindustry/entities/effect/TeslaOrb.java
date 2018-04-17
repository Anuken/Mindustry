package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.enemyGroup;

public class TeslaOrb extends Entity{
	private Array<Vector2> points = new Array<>();
	private ObjectSet<Enemy> hit = new ObjectSet<>();
	private int damage = 0;
	private float range = 0;
	private float lifetime = 30f;
	private float life = 0f;
	private Vector2 vector = new Vector2();
	
	public TeslaOrb(float x, float y, float range, int damage){
		set(x, y);
		this.damage = damage;
		this.range = range;
	}
	
	void shock(){
		float stopchance = 0.1f;
		float curx = x, cury = y;
		float shake = 3f;
		
		int max = 7;
		
		while(points.size < max){
			if(Mathf.chance(stopchance)){
				break;
			}
			
			Array<SolidEntity> enemies = Entities.getNearby(enemyGroup, curx, cury, range*2f);

			synchronized (Entities.entityLock) {

				for (SolidEntity entity : enemies) {
					if (entity != null && entity.distanceTo(curx, cury) < range && !hit.contains((Enemy) entity)) {
						hit.add((Enemy) entity);
						points.add(new Vector2(entity.x + Mathf.range(shake), entity.y + Mathf.range(shake)));
						damageEnemy((Enemy) entity);
						curx = entity.x;
						cury = entity.y;
						break;
					}
				}
			}
		}
		
		if(points.size == 0){
			remove();
		}
	}
	
	void damageEnemy(Enemy enemy){
		enemy.damage(damage);
		Effects.effect(Fx.laserhit, enemy.x + Mathf.range(2f), enemy.y + Mathf.range(2f));
	}
	
	@Override
	public void update(){
		life += Timers.delta();
		
		if(life >= lifetime){
			remove();
		}
	}
	
	@Override
	public void drawOver(){
		if(points.size == 0) return;
		
		float range = 1f;
		
		Vector2 previous = vector.set(x, y);
		
		for(Vector2 enemy : points){
			
			
			float x1 = previous.x + Mathf.range(range), 
					y1 = previous.y + Mathf.range(range),
					x2 = enemy.x + Mathf.range(range),
					y2 = enemy.y + Mathf.range(range);
			
			Draw.color(Color.WHITE);
			Draw.alpha(1f-life/lifetime);
			
			Lines.stroke(3f - life/lifetime*2f);
			Lines.line(x1, y1, x2, y2);
			
			float rad = 7f - life/lifetime*5f;
			
			Draw.rect("circle", x2, y2, rad, rad);
			
			if(previous.epsilonEquals(x, y, 0.001f)){
				Draw.rect("circle", x, y, rad, rad);
			}
			
			//Draw.color(Color.WHITE);
			
			//Draw.stroke(2f - life/lifetime*2f);
			//Draw.line(x1, y1, x2, y2);
			
			Draw.reset();
			
			previous = enemy;
		}
	}
	
	@Override
	public void added(){
		Timers.run(1f, ()->{
			shock();
		});
	}
	
	@Override
	public float drawSize(){
		return 200;
	}
}
