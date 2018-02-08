package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.graphics.Shaders;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Translator;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class Player extends SyncEntity{
	static final float speed = 1.1f;
	static final float dashSpeed = 1.8f;

	public String name = "name";
	public boolean isAndroid;
	public Color color = new Color();

	public Weapon weaponLeft = Weapon.blaster;
	public Weapon weaponRight = Weapon.blaster;
	public Mech mech = Mech.standard;

	public float targetAngle = 0f;
	public float stucktime = 0f;
	public boolean dashing = false;

	public int clientid;
	public boolean isLocal = false;

	private Vector2 movement = new Vector2();
	private Translator tr = new Translator();
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(5f);
		
		maxhealth = 200;
		heal();
	}

	@Override
	public void damage(int amount){
		if(debug || isAndroid) return;

		health -= amount;
		if(health <= 0 && !dead && isLocal){ //remote players don't die normally
			onDeath();
			dead = true;
		}
	}

	@Override
	public boolean collides(SolidEntity other){
		if(other instanceof Bullet){
			Bullet b = (Bullet)other;
			if(!state.friendlyFire && b.owner instanceof Player){
				return false;
			}
		}
		return !isDead() && super.collides(other) && !isAndroid;
	}
	
	@Override
	public void onDeath(){
		remove();
		if(Net.active()){
			NetEvents.handlePlayerDeath();
		}

		Effects.effect(Fx.explosion, this);
		Effects.shake(4f, 5f, this);
		Effects.sound("die", this);

		control.setRespawnTime(respawnduration);
		ui.hudfrag.fadeRespawn(true);
	}

	/**called when a remote player death event is recieved*/
	public void doRespawn(){
		dead = true;
		Effects.effect(Fx.explosion, this);
		Effects.shake(4f, 5f, this);
		Effects.sound("die", this);

		Timers.run(respawnduration + 5f, () -> {
			heal();
			set(world.getSpawnX(), world.getSpawnY());
			interpolator.target.set(x, y);
		});
	}
	
	@Override
	public void drawSmooth(){
		if((debug && (!showPlayer || !showUI)) || (isAndroid && isLocal) || (dead && !isLocal)) return;
        boolean snap = snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate") && isLocal;

		String part = isAndroid ? "ship" : "mech";

		Shaders.outline.color.set(getColor());
		Shaders.outline.lighten = 0f;
		Shaders.outline.region = Draw.region(part + "-" + mech.name);

		Shaders.outline.apply();

		if(!isAndroid) {
			for (int i : Mathf.signs) {
				Weapon weapon = i < 0 ? weaponLeft : weaponRight;
				tr.trns(angle - 90, 3*i, 2);
				float w = i > 0 ? -8 : 8;
				if(snap){
					Draw.rect(weapon.name + "-equip", (int)x + tr.x, (int)y + tr.y, w, 8, angle - 90);
				}else{
					Draw.rect(weapon.name + "-equip", x + tr.x, y + tr.y, w, 8, angle - 90);
				}
			}
		}

		if(snap){
			Draw.rect(part + "-" + mech.name, (int)x, (int)y, angle-90);
		}else{
			Draw.rect(part + "-" + mech.name, x, y, angle-90);
		}

		Graphics.flush();
	}
	
	@Override
	public void update(){
		if(!isLocal || isAndroid){
			if(isAndroid && isLocal){
				angle = Mathf.lerpAngDelta(angle, targetAngle, 0.2f);
			}
			if(!isLocal) interpolate();
			return;
		}

		Tile tile = world.tileWorld(x, y);

		//if player is in solid block
		if(tile != null && ((tile.floor().liquid && tile.block() == Blocks.air) || tile.solid())){
			stucktime += Timers.delta();
		}else{
			stucktime = 0f;
		}

		if(stucktime > 15f){
			damage(health+1); //die instantly
			stucktime = 0f;
		}

		if(ui.chatfrag.chatOpen()) return;

		dashing = Inputs.keyDown("dash");
		
		float speed = dashing ? (debug ? Player.dashSpeed * 5f : Player.dashSpeed) : Player.speed;
		
		if(health < maxhealth && Timers.get(this, "regen", 20))
			health ++;

		health = Mathf.clamp(health, -1, maxhealth);
		
		movement.set(0, 0);

		float xa = Inputs.getAxis("move_x");
		float ya = Inputs.getAxis("move_y");
		if(Math.abs(xa) < 0.3) xa = 0;
		if(Math.abs(ya) < 0.3) ya = 0;

		movement.y += ya*speed;
		movement.x += xa*speed;
		
		boolean shooting = !Inputs.keyDown("dash") && Inputs.keyDown("shoot") && control.input().recipe == null
				&& !ui.hasMouse() && !control.input().onConfigurable();

		if(shooting){
			weaponLeft.update(player, true);
			weaponRight.update(player, false);
		}
		
		if(dashing && Timers.get(this, "dashfx", 3) && movement.len() > 0){
			Effects.effect(Fx.dashsmoke, x + Angles.trnsx(angle + 180f, 3f), y + Angles.trnsy(angle + 180f, 3f));
		}
		
		movement.limit(speed);
		
		if(!noclip){
			move(movement.x*Timers.delta(), movement.y*Timers.delta());
		}else{
			x += movement.x*Timers.delta();
			y += movement.y*Timers.delta();
		}
		
		if(!shooting){
			if(!movement.isZero())
				angle = Mathf.lerpAngDelta(angle, movement.angle(), 0.13f);
		}else{
			float angle = Angles.mouseAngle(x, y);
			this.angle = Mathf.lerpAngDelta(this.angle, angle, 0.1f);
		}

		x = Mathf.clamp(x, 0, world.width() * tilesize);
		y = Mathf.clamp(y, 0, world.height() * tilesize);
	}

	@Override
	public Player add(){
		return add(playerGroup);
	}

    @Override
    public String toString() {
        return "Player{" + id + ", android=" + isAndroid + ", local=" + isLocal + ", " + x + ", " + y + "}\n";
    }

	@Override
	public void writeSpawn(ByteBuffer buffer) {
		buffer.put((byte)name.getBytes().length);
		buffer.put(name.getBytes());
		buffer.put(weaponLeft.id);
		buffer.put(weaponRight.id);
		buffer.put(isAndroid ? 1 : (byte)0);
		buffer.putInt(Color.rgba8888(color));
		buffer.putFloat(x);
		buffer.putFloat(y);
	}

	@Override
	public void readSpawn(ByteBuffer buffer) {
		byte nlength = buffer.get();
		byte[] n = new byte[nlength];
		buffer.get(n);
		name = new String(n);
		weaponLeft = (Weapon) Upgrade.getByID(buffer.get());
		weaponRight = (Weapon) Upgrade.getByID(buffer.get());
		isAndroid = buffer.get() == 1;
		color.set(buffer.getInt());
		x = buffer.getFloat();
		y = buffer.getFloat();
		setNet(x, y);
	}

	@Override
	public void write(ByteBuffer data) {
		//TODO written angle is always 0

		data.putFloat(x);
		data.putFloat(y);
		data.putFloat(angle);
		data.putShort((short)health);
		data.put((byte)(dashing ? 1 : 0));
	}

	@Override
	public void read(ByteBuffer data, long time) {
		float x = data.getFloat();
		float y = data.getFloat();
		float angle = data.getFloat();
		short health = data.getShort();
		byte dashing = data.get();

		this.health = health;
		this.dashing = dashing == 1;

		interpolator.read(this.x, this.y, x, y, angle, time);
	}

	@Override
	public void interpolate() {
		super.interpolate();

		Interpolator i = interpolator;

		float tx = x + Angles.trnsx(angle + 180f, 3f);
		float ty = y + Angles.trnsy(angle + 180f, 3f);

		if(isAndroid && i.target.dst(i.last) > 2f && Timers.get(this, "dashfx", 2)){
			Effects.effect(Fx.dashsmoke, tx, ty);
		}

		if(dashing && !dead && Timers.get(this, "dashfx", 3) && i.target.dst(i.last) > 1f){
			Effects.effect(Fx.dashsmoke, tx, ty);
		}
	}

	public Color getColor(){
		return color;
	}
}
