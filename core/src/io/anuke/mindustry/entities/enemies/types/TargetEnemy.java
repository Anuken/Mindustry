package io.anuke.mindustry.entities.enemies.types;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Mathf;

public class TargetEnemy extends EnemyType {
	
	public TargetEnemy(){
		super("targetenemy");

		speed = 0f;
		health = 25;
		shootsound = null;
	}
	
	@Override
	public void move(Enemy enemy){
		super.move(enemy);
	}
	
	@Override
	public void shoot(Enemy enemy){
		//do nothing
	}
	
	@Override
	public void removed(Enemy enemy){
		//don't call enemy death since this is only a target
	}
	
	@Override
	public void draw(Enemy enemy){
		super.draw(enemy);
		
		Draw.color(Color.YELLOW);
		
		if(Vars.control.getTutorial().showTarget()){
			Draw.spikes(enemy.x, enemy.y, 11f + Mathf.sin(Timers.time(), 7f, 1f), 4f, 8, Timers.time());
		}
		
		Draw.color();
	}
	
	@Override
	public void onDeath(Enemy enemy){
		super.onDeath(enemy);
		Timers.run(100f, ()->{
			new Enemy(EnemyTypes.target).set(enemy.x, enemy.y).add();
		});
	}
}
