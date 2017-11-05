package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.ai.Pathfind;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;

public class HealerEnemy extends Enemy{
	int healTime = 10;

	public HealerEnemy(int spawn) {
		super(spawn);
		
		speed = 0.4f;
		reload = 30;
		maxhealth = 210;
		range = 80f;
		bullet = BulletType.shot;
		range = 30f;
		
		heal();
	}
	
	@Override
	void move(){
		Vector2 vec  = Pathfind.find(this);
		vec.sub(x, y).setLength(speed);
		
		move(vec.x*Timers.delta(), vec.y*Timers.delta());
		
		if(Timers.get(this, 15)){
			target = Entities.getClosest(x, y, range, e->e instanceof Enemy);
		}
		
		if(target != null){
			updateShooting();
		}
	}
	
	@Override
	void updateShooting(){
		Enemy enemy = (Enemy)target;
		
		if(enemy.health < enemy.maxhealth && Timers.get(this, "heal", healTime)){
			enemy.health ++;
		}
	}
	
	@Override
	public void drawOver(){
		super.drawOver();
		Enemy enemy = (Enemy)target;
		
		Angles.translation(this.angleTo(enemy), 3f);
		
		if(enemy != null && enemy.health < enemy.maxhealth){
			Draw.color(Hue.rgb(138, 244, 138, (MathUtils.sin(Timers.time()) + 1f) / 14f));
			Draw.alpha(0.3f);
			Draw.laser("laser", "laserend", x + Angles.x(), y + Angles.y(), enemy.x, enemy.y);
			Draw.color();
		}
	}

}
