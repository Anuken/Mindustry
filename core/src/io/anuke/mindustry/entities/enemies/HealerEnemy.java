package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.Shaders;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;

public class HealerEnemy extends Enemy{

	public HealerEnemy(int spawn) {
		super(spawn);
		
		speed = 0.2f;
		reload = 14;
		maxhealth = 130;
		range = 90f;
		bullet = BulletType.shot;
		range = 30f;
		alwaysRotate = false;
		targetCore = false;
		mass = 1.1f;
		
		heal();
	}
	
	@Override
	void updateTargeting(boolean nearCore){
		if(Timers.get(this, "target", 15)){
			target = Entities.getClosest(Entities.getGroup(Enemy.class),
					x, y, range, e -> e instanceof Enemy && e != this && ((Enemy)e).healthfrac() < 1f);
		}
		
		if(target != null){
			updateShooting();
		}
	}
	
	@Override
	void updateShooting(){
		Enemy enemy = (Enemy)target;
		
		if(enemy.health < enemy.maxhealth && Timers.get(this, "heal", reload)){
			enemy.health ++;
		}
	}
	
	@Override
	public void drawOver(){
		super.drawOver();
		Enemy enemy = (Enemy)target;
		
		if(enemy == null) return;
		
		Angles.translation(this.angleTo(enemy), 5f);
		
		Graphics.shader();
		if(enemy != null && enemy.health < enemy.maxhealth){
			Draw.color(Hue.rgb(138, 244, 138, (MathUtils.sin(Timers.time()) + 1f) / 13f));
			Draw.alpha(0.9f);
			Draw.laser("laser", "laserend", x + Angles.x(), y + Angles.y(), enemy.x - Angles.x()/1.5f, enemy.y - Angles.y()/1.5f);
			Draw.color();
		}
		Graphics.shader(Shaders.outline);
	}

}
