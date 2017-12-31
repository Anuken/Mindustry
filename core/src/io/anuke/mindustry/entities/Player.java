package io.anuke.mindustry.entities;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.Input.Buttons;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.input.PlaceMode;
import io.anuke.mindustry.net.Syncable;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.resource.Recipe;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.DestructibleEntity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class Player extends DestructibleEntity implements Syncable{
	private static final float speed = 1.1f;
	private static final float dashSpeed = 1.8f;
	
	public transient Weapon weapon;
	public Mech mech = Mech.standard;
	public float angle;

	public transient int clientid;

	public transient float breaktime = 0;
	public transient Recipe recipe;
	public transient int rotation;
	public transient PlaceMode placeMode = android ? PlaceMode.cursor : PlaceMode.hold;
	public transient PlaceMode breakMode = android ? PlaceMode.none : PlaceMode.holdDelete;
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(4f);
		
		maxhealth = 100;
		heal();
	}
	
	@Override
	public void damage(int amount){
		if(!Vars.debug && !Vars.android)
			super.damage(amount);
	}
	
	@Override
	public void onDeath(){
		
		remove();
		Effects.effect(Fx.explosion, this);
		Effects.shake(4f, 5f, this);
		Effects.sound("die", this);
		
		Vars.control.setRespawnTime(respawnduration);
		ui.fadeRespawn(true);
	}
	
	@Override
	public void draw(){
		if(Vars.debug && (!Vars.showPlayer || !Vars.showUI)) return;
		
		if(Vars.snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate")){
			Draw.rect("mech-"+mech.name(), (int)x, (int)y, angle-90);
		}else{
			Draw.rect("mech-"+mech.name(), x, y, angle-90);
		}
		
	}
	
	@Override
	public void update(){
		
		float speed = Inputs.keyDown("dash") ? Player.dashSpeed : Player.speed;
		
		if(health < maxhealth && Timers.get(this, "regen", 50))
			health ++;

		Tile tile = world.tileWorld(x, y);
		if(tile != null && tile.floor().liquid && tile.block() == Blocks.air){
			damage(health+1); //drown
		}
		
		vector.set(0, 0);

		float xa = Inputs.getAxis("move_x");
		float ya = Inputs.getAxis("move_y");
		if(Math.abs(xa) < 0.3) xa = 0;
		if(Math.abs(ya) < 0.3) ya = 0;

		vector.y += ya*speed;
		vector.x += xa*speed;
		
		boolean shooting = !Inputs.keyDown("dash") && Inputs.keyDown("shoot") && recipe == null
				&& !ui.hasMouse() && !control.getInput().onConfigurable();
		
		if(shooting && Timers.get(this, "reload", weapon.reload)){
			weapon.shoot(this);
			Sounds.play(weapon.shootsound);
		}
		
		if(Inputs.keyDown("dash") && Timers.get(this, "dashfx", 3) && vector.len() > 0){
			Angles.translation(angle + 180, 3f);
			Effects.effect(Fx.dashsmoke, x + Angles.x(), y + Angles.y());
		}
		
		vector.limit(speed);
		
		if(!Vars.noclip){
			move(vector.x*Timers.delta(), vector.y*Timers.delta());
		}else{
			x += vector.x*Timers.delta();
			y += vector.y*Timers.delta();
		}
		
		if(!shooting){
			if(!vector.isZero())
				angle = Mathf.lerpAngDelta(angle, vector.angle(), 0.13f);
		}else{
			float angle = Angles.mouseAngle(x, y);
			this.angle = Mathf.lerpAngDelta(this.angle, angle, 0.1f);
		}
	}

	@Override
	public Player add(){
		return add(Vars.control.playerGroup);
	}
}
