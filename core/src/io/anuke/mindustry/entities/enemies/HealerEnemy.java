package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.math.MathUtils;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.effect.Shaders;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.util.Angles;

public class HealerEnemy extends Enemy{

	public HealerEnemy() {
		
		speed = 0.25f;
		reload = 10;
		maxhealth = 200;
		bullet = BulletType.shot;
		range = 40f;
		alwaysRotate = false;
		targetCore = false;
		stopNearCore = true;
		mass = 1.1f;
		
		heal();
	}
	
	@Override
	void move(){
		super.move();
		
		if(idletime > 60f*3){ //explode after 3 seconds of stillness
			explode();
			Effects.effect(Fx.shellexplosion, this);
			Effects.effect(Fx.shellsmoke, this);
		}
	}
	
	@Override
	void updateTargeting(boolean nearCore){
		if(timer.get(timerTarget, 15)){
			target = Entities.getClosest(Vars.control.enemyGroup,
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
			idletime = 0;
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
	
	void explode(){
		Bullet b = new Bullet(BulletType.blast, this, x, y, 0).add();
		b.damage = BulletType.blast.damage + (tier-1) * 30;
		damage(999);
		remove();
	}

}
