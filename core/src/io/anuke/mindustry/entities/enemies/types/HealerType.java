package io.anuke.mindustry.entities.enemies.types;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.Entities;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Shapes;

import static io.anuke.mindustry.Vars.enemyGroup;

public class HealerType extends EnemyType {

	public HealerType() {
		super("healerenemy");
		speed = 0.25f;
		reload = 10;
		health = 200;
		bullet = BulletType.shot;
		range = 40f;
		alwaysRotate = false;
		targetCore = false;
		stopNearCore = true;
		targetClient = true;
		mass = 1.1f;
	}

	
	@Override
	public void behavior(Enemy enemy){
		
		if(enemy.idletime > 60f*3){ //explode after 3 seconds of stillness
			explode(enemy);
			Effects.effect(Fx.shellexplosion, enemy);
			Effects.effect(Fx.shellsmoke, enemy);
		}
	}
	
	@Override
	public void updateTargeting(Enemy enemy, boolean nearCore){
		if(enemy.timer.get(timerTarget, 15)){
			enemy.target = Entities.getClosest(enemyGroup,
					enemy.x, enemy.y, range, e -> e instanceof Enemy && e != enemy && ((Enemy)e).healthfrac() < 1f);
		}
		
		if(enemy.target != null){
			updateShooting(enemy);
		}
	}

	
	@Override
	public void updateShooting(Enemy enemy){
		Enemy target = (Enemy)enemy.target;
		
		if(target.health < target.maxhealth && enemy.timer.get(timerReload, reload)){
			target.health ++;
			enemy.idletime = 0;
		}
	}
	
	@Override
	public void drawOver(Enemy enemy){
		Enemy target = (Enemy)enemy.target;

		if(target == null) return;

		enemy.tr.trns(enemy.angleTo(target), 5f);

		Shaders.outline.color.set(Color.CLEAR);
		Shaders.outline.apply();

		if(target.health < target.maxhealth){
			Draw.color(Hue.rgb(138, 244, 138, (MathUtils.sin(Timers.time()) + 1f) / 13f));
			Draw.alpha(0.9f);
			Shapes.laser("laser", "laserend", enemy.x + enemy.tr.x, enemy.y + enemy.tr.y, target.x - enemy.tr.x/1.5f, target.y - enemy.tr.y/1.5f);
			Draw.color();
		}

		Graphics.flush();
	}
	
	void explode(Enemy enemy){
		Bullet b = new Bullet(BulletType.blast, enemy, enemy.x, enemy.y, 0).add();
		b.damage = BulletType.blast.damage + (enemy.tier-1) * 30;
		enemy.damage(999);
		enemy.remove();
	}

}
