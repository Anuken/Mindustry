package io.anuke.mindustry.entities.enemies;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;

public class TargetEnemy extends Enemy{
	
	public TargetEnemy(int spawn){
		super(0);
		speed = 0f;
		maxhealth = 10;
	}
	
	void move(){
		
	}
	
	void shoot(){
		
	}
	
	@Override
	public void removed(){
		//don't call enemy death since this is only a target
	}
	
	@Override
	public void draw(){
		super.draw();
		
		Draw.color(Color.YELLOW);
		
		if(Vars.control.getTutorial().showTarget()){
			Draw.spikes(x, y, 11f, 4f, 8, Timers.time());
		}
		
		Draw.color();
	}
	
	@Override
	public void onDeath(){
		super.onDeath();
		Timers.run(100f, ()->{
			new TargetEnemy(0).set(x, y).add();
		});
	}
}
