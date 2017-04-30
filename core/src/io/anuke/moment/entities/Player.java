package io.anuke.moment.entities;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.Vector2;

import io.anuke.moment.Control;
import io.anuke.moment.Moment;
import io.anuke.moment.UI;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.UInput;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.util.Angles;

public class Player extends DestructibleEntity{
	Vector2 direction = new Vector2();
	float speed = 2f;
	float rotation;
	float reload;
	Weapon weapon = Weapon.blaster;
	
	public Player(){
		hitsize = 5;
		
		maxhealth = 100;
		heal();
	}
	
	@Override
	public void removed(){
	}
	
	@Override
	public void draw(){
		Draw.rect("player", x, y, direction.angle()-90);
	}
	
	@Override
	public void update(){
		vector.set(0, 0);
		
		if(UInput.keyDown("up"))
			vector.y += speed;
		if(UInput.keyDown("down"))
			vector.y -= speed;
		if(UInput.keyDown("left"))
			vector.x -= speed;
		if(UInput.keyDown("right"))
			vector.x += speed;
		
		reload -= delta;
		
		boolean shooting = UInput.buttonDown(Buttons.LEFT) && Moment.i.recipe == null && !Moment.module(UI.class).hasMouse();
		
		if(shooting && reload <= 0){
			weapon.shoot(this);
			reload = weapon.reload;
		}
		
		vector.limit(speed);
		
		//x += vector.x*delta;
		//y += vector.y*delta;
		
		Moment.module(Control.class).tryMove(this, vector.x*delta, vector.y*delta);
		
		if(!shooting){
			direction.add(vector.scl(delta));
			direction.limit(speed*6);
		}else{
			float angle = Angles.mouseAngle(x, y);
			direction.lerp(vector.set(0, 1).setAngle(angle), 0.26f);
		}
	}
}
