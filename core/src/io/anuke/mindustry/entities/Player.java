package io.anuke.mindustry.entities;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class Player extends SyncEntity{
	static final float speed = 1.1f;
	static final float dashSpeed = 1.8f;

	public String name = "name";
	public boolean isAndroid;

	//TODO send these.
	public transient Weapon weaponLeft = Weapon.blaster;
	public transient Weapon weaponRight = Weapon.blaster;
	public transient Mech mech = Mech.standard;

	public float angle;
	public transient float targetAngle = 0f;
	public transient boolean dashing = false;

	public transient int clientid;
	public transient boolean isLocal = false;
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(4f);
		
		maxhealth = 200;
		heal();
	}

	@Override
	public void damage(int amount){
		if(!Vars.debug && !isAndroid)
			super.damage(amount);
	}

	@Override
	public boolean collides(SolidEntity other){
		if(other instanceof Bullet){
			Bullet b = (Bullet)other;
			if(!Vars.control.isFriendlyFire() && b.owner instanceof Player){
				return false;
			}
		}
		return super.collides(other) && !isAndroid;
	}
	
	@Override
	public void onDeath(){

		if(isLocal){
			remove();
			if(Net.active()){
				Vars.netClient.handlePlayerDeath();
			}

			Effects.effect(Fx.explosion, this);
			Effects.shake(4f, 5f, this);
			Effects.sound("die", this);
		}

		//TODO respawning doesn't work properly for multiplayer at all
		if(isLocal) {
			Vars.control.setRespawnTime(respawnduration);
			ui.hudfrag.fadeRespawn(true);
		}
	}

	public void doRespawn(){
		dead = true;
		Effects.effect(Fx.explosion, this);
		Effects.shake(4f, 5f, this);
		Effects.sound("die", this);

		set(-9999, -9999);
		Timers.run(respawnduration, () -> {
			heal();
			set(Vars.control.getCore().worldx(), Vars.control.getCore().worldy());
		});
	}
	
	@Override
	public void draw(){
        if(isAndroid && isLocal){
            angle = Mathf.lerpAngDelta(angle, targetAngle, 0.2f);
        }

		if((Vars.debug && (!Vars.showPlayer || !Vars.showUI)) || (isAndroid && isLocal) ) return;
        boolean snap = Vars.snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate") && isLocal;

		String part = isAndroid ? "ship" : "mech";
		
		if(snap){
			Draw.rect(part + "-" + mech.name, (int)x, (int)y, angle-90);
		}else{
			Draw.rect(part + "-" + mech.name, x, y, angle-90);
		}

		if(!isAndroid) {
			for (boolean b : new boolean[]{true, false}) {
				Weapon weapon = b ? weaponLeft : weaponRight;
				Angles.translation(angle + Mathf.sign(b) * -50f, 3.5f);
				float s = 5f;
				if(snap){
					Draw.rect(weapon.name, (int)x + Angles.x(), (int)y + Angles.y(), s, s, angle- 90);
				}else{
					Draw.rect(weapon.name, x + Angles.x(), y + Angles.y(), s, s, angle - 90);
				}
			}
		}
	}
	
	@Override
	public void update(){
		if(!isLocal || isAndroid || Vars.ui.chatfrag.chatOpen()){
			if(!isDead() && !isLocal) interpolate();
			return;
		}

		dashing = Inputs.keyDown("dash");
		
		float speed = dashing ? (debug ? Player.dashSpeed * 5f : Player.dashSpeed) : Player.speed;
		
		if(health < maxhealth && Timers.get(this, "regen", 20))
			health ++;

		health = Mathf.clamp(health, -1, maxhealth);

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
		
		boolean shooting = !Inputs.keyDown("dash") && Inputs.keyDown("shoot") && control.getInput().recipe == null
				&& !ui.hasMouse() && !control.getInput().onConfigurable();
		
		if(shooting){
			weaponLeft.update(player, true);
			weaponRight.update(player, false);
		}
		
		if(dashing && Timers.get(this, "dashfx", 3) && vector.len() > 0){
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

		x = Mathf.clamp(x, 0, Vars.world.width() * Vars.tilesize);
		y = Mathf.clamp(y, 0, Vars.world.height() * Vars.tilesize);
	}

	@Override
	public Player add(){
		return add(Vars.control.playerGroup);
	}

    @Override
    public String toString() {
        return "Player{" + id + ", android=" + isAndroid + ", local=" + isLocal + ", " + x + ", " + y + "}\n";
    }

	@Override
	public void write(ByteBuffer data) {
		data.putFloat(x);
		data.putFloat(y);
		data.putFloat(angle);
		data.putShort((short)health);
		data.put((byte)(dashing ? 1 : 0));
	}

	@Override
	public void read(ByteBuffer data) {
		float x = data.getFloat();
		float y = data.getFloat();
		float angle = data.getFloat();
		short health = data.getShort();
		byte dashing = data.get();

		interpolator.target.set(x, y);
		interpolator.targetrot = angle;
		this.health = health;
		this.dashing = dashing == 1;
	}

	@Override
	public void interpolate() {
		Interpolator i = interpolator;
		if(i.target.dst(x, y) > 16 && !isAndroid){
			set(i.target.x, i.target.y);
		}

		if(isAndroid && i.target.dst(x, y) > 2f && Timers.get(this, "dashfx", 2)){
			Angles.translation(angle + 180, 3f);
			Effects.effect(Fx.dashsmoke, x + Angles.x(), y + Angles.y());
		}

		if(dashing && Timers.get(this, "dashfx", 3)){
			Angles.translation(angle + 180, 3f);
			Effects.effect(Fx.dashsmoke, x + Angles.x(), y + Angles.y());
		}

		x = Mathf.lerpDelta(x, i.target.x, 0.4f);
		y = Mathf.lerpDelta(y, i.target.y, 0.4f);
		angle = Mathf.lerpAngDelta(angle, i.targetrot, 0.6f);
	}
}
