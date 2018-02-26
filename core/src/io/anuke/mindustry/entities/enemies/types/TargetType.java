package io.anuke.mindustry.entities.enemies.types;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.entities.enemies.Enemy;
import io.anuke.mindustry.entities.enemies.EnemyType;
import io.anuke.mindustry.entities.enemies.EnemyTypes;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.control;

public class TargetType extends EnemyType {
	
	public TargetType(){
		super("targetenemy");

		speed = 0f;
		health = 40;
		shootsound = null;
	}
	
	@Override
	public void move(Enemy enemy){

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
		
		if(control.tutorial().showTarget()){
			Lines.spikes(enemy.x, enemy.y, 11f + Mathf.sin(Timers.time(), 7f, 1f), 4f, 8, Timers.time());
		}
		
		Draw.color();
	}
	
	@Override
	public void onDeath(Enemy enemy, boolean force){
		super.onDeath(enemy, force);
		Timers.run(100f, ()->{
			new Enemy(EnemyTypes.target).set(enemy.x, enemy.y).add();
		});
	}

	@Override
	public boolean isCalculating(Enemy enemy){
		return false;
	}
}
