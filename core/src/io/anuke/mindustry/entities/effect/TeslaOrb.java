package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.units.BaseUnit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

public class TeslaOrb extends Entity{
	private final static Rectangle rect = new Rectangle();

	private final Array<Vector2> points = new Array<>();
	private final ObjectSet<BaseUnit> hit = new ObjectSet<>();
	private final int damage;
	private final float range;
	private final float lifetime = 30f;
	private final Vector2 vector = new Vector2();
	private final Team team;

	private float life = 0f;
	private float curx = x, cury = y;
	private boolean done = false;
	
	public TeslaOrb(Team team, float x, float y, float range, int damage){
		set(x, y);
		this.team = team;
		this.damage = damage;
		this.range = range;
	}
	
	void shock(){
		float stopchance = 0.1f;
		float shake = 3f;
		
		int max = 7;
		
		while(points.size < max){
			if(Mathf.chance(stopchance)){
				break;
			}

			rect.setSize(range*2f).setCenter(curx, cury);

			Units.getNearbyEnemies(team, rect, entity -> {
				if(!done && entity != null && entity.distanceTo(curx, cury) < range && !hit.contains((BaseUnit)entity)){
					hit.add((BaseUnit)entity);
					points.add(new Vector2(entity.x + Mathf.range(shake), entity.y + Mathf.range(shake)));
					damageEnemy((BaseUnit)entity);
					curx = entity.x;
					cury = entity.y;
					done = true;
				}
			});
		}
		
		if(points.size == 0){
			remove();
		}
	}
	
	void damageEnemy(BaseUnit enemy){
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
