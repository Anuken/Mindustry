package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.ItemStack;
import io.anuke.mindustry.resource.Mech;
import io.anuke.mindustry.resource.Upgrade;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;
import io.anuke.ucore.util.Translator;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class Player extends Unit{
	static final float speed = 1.1f;
	static final float dashSpeed = 1.8f;

	static final int timerDash = 0;
	static final int timerRegen = 3;

	public String name = "name";
	public boolean isAdmin;
	public Color color = new Color();

	public Weapon weaponLeft = Weapons.blaster;
	public Weapon weaponRight = Weapons.blaster;
	public Mech mech = Mechs.standard;

	public float targetAngle = 0f;
	public boolean dashing = false;
	public boolean selectingItem;

	public int clientid = -1;
	public boolean isLocal = false;
	public Timer timer = new Timer(4);
	public float walktime;

	private Vector2 movement = new Vector2();
	private Translator tr = new Translator();
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(4f);
		
		maxhealth = 200;
		heal();
	}

	@Override
	public void onRemoteShoot(BulletType type, float x, float y, float rotation, short data) {
		Weapon weapon = Upgrade.getByID((byte)data);
		weapon.shoot(player, x, y, rotation);
	}

	@Override
	public float getMass(){
	    return mech.mass;
    }

    @Override
	public boolean isFlying(){
		return mech.flying || noclip;
	}

	@Override
	public float getSize() {
		return 8;
	}

	@Override
	public void damage(float amount){
		//if(debug || mech.flying) return;
		hitTime = hitDuration;
		if(!debug) {
			health -= amount;
			if(health <= 0 && !dead && isLocal){ //remote players don't die normally
				onDeath();
				dead = true;
			}
		}
	}

	@Override
	public boolean collides(SolidEntity other){
		return !isDead() && super.collides(other) && !mech.flying;
	}
	
	@Override
	public void onDeath(){
		super.onDeath();
		dead = true;
		if(Net.active()){
			NetEvents.handleUnitDeath(this);
		}

		Effects.effect(ExplosionFx.explosion, this);
		Effects.shake(4f, 5f, this);
		Effects.sound("die", this);

		control.setRespawnTime(respawnduration);
		ui.hudfrag.fadeRespawn(true);
	}

	@Override
	public void onRemoteDeath(){
		dead = true;
		Effects.effect(ExplosionFx.explosion, this);
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
		if((debug && (!showPlayer || !showUI)) || dead) return;

        boolean snap = snapCamera && Settings.getBool("smoothcam") && Settings.getBool("pixelate") && isLocal;

        String mname = mech.name;

		float px = x, py =y;

		if(snap){
			x = (int)x;
			y = (int)y;
		}

		float ft = Mathf.sin(walktime, 6f, 2f);

		Floor floor = getFloorOn();

		Draw.color();
		Draw.alpha(hitTime / hitDuration);

		if(!mech.flying) {
			if(floor.liquid){
				Draw.tint(Color.WHITE, floor.liquidColor, 0.5f);
			}

			for (int i : Mathf.signs) {
				tr.trns(baseRotation, ft * i);
				Draw.rect(mname + "-leg", x + tr.x, y + tr.y, 12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
			}

			Draw.rect(mname + "-base", x, y,baseRotation- 90);
		}

		if(floor.liquid) {
			Draw.tint(Color.WHITE, floor.liquidColor, drownTime * 0.4f);
		}else {
			Draw.tint(Color.WHITE);
		}

		Draw.rect(mname, x, y, rotation -90);

		for (int i : Mathf.signs) {
			Weapon weapon = i < 0 ? weaponLeft : weaponRight;
			tr.trns(rotation - 90, 4*i, 3);
			float w = i > 0 ? -8 : 8;
			Draw.rect(weapon.name + "-equip", x + tr.x, y + tr.y, w, 8, rotation - 90);
		}

		float backTrns = 4f, itemSize = 5f;
		if(inventory.hasItem()){
			ItemStack stack = inventory.getItem();
			Draw.rect(stack.item.region, x + Angles.trnsx(rotation + 180f, backTrns), y + Angles.trnsy(rotation + 180f, backTrns), itemSize, itemSize, rotation);
			//Draw.tint(Color.WHITE);
			//Lines.circle(x + Angles.trnsx(rotation + 180f, backTrns), y + Angles.trnsy(rotation + 180f, backTrns), 3f + Mathf.absin(Timers.time(), 3f, 0.8f));
			//Draw.tint(Color.WHITE);
		}

		Draw.alpha(1f);

		x = px;
		y = py;
	}
	
	@Override
	public void update(){
		hitTime = Math.max(0f, hitTime - Timers.delta());

		if(!isLocal){
			interpolate();
			return;
		}

		if(isDead()) return;

		if(mech.flying){
			updateFlying();
		}else{
			updateMech();
		}

		x = Mathf.clamp(x, 0, world.width() * tilesize);
		y = Mathf.clamp(y, 0, world.height() * tilesize);
	}

	protected void updateMech(){

		Tile tile = world.tileWorld(x, y);

		//if player is in solid block
		if(tile != null && tile.solid()) {
			damage(health + 1); //die instantly
		}

		if(ui.chatfrag.chatOpen()) return;

		dashing = Inputs.keyDown("dash");

		float speed = dashing ? (debug ? Player.dashSpeed * 5f : Player.dashSpeed) : Player.speed;

		if(health < maxhealth && timer.get(timerRegen, 20))
			health ++;

		health = Mathf.clamp(health, -1, maxhealth);

		movement.set(0, 0);

		float xa = Inputs.getAxis("move_x");
		float ya = Inputs.getAxis("move_y");
		if(Math.abs(xa) < 0.3) xa = 0;
		if(Math.abs(ya) < 0.3) ya = 0;

		movement.y += ya*speed;
		movement.x += xa*speed;

		boolean shooting = control.input().canShoot() && control.input().isShooting();

		if(shooting){
			weaponLeft.update(player, true);
			weaponRight.update(player, false);
		}

		if(dashing && timer.get(timerDash, 3) && movement.len() > 0){
			Effects.effect(Fx.dash, x + Angles.trnsx(rotation + 180f, 3f), y + Angles.trnsy(rotation + 180f, 3f));
		}

		movement.limit(speed);

		velocity.add(movement);

		updateVelocityStatus(0.4f, speed);

		if(!movement.isZero()){
			walktime += Timers.delta() * velocity.len()*(1f/0.5f)/speed * getFloorOn().speedMultiplier;
			baseRotation = Mathf.slerpDelta(baseRotation, movement.angle(), 0.13f);
		}

		if(!shooting){
			if(!movement.isZero()) {
				rotation = Mathf.slerpDelta(rotation, movement.angle(), 0.13f);
			}
		}else{
			float angle = Angles.mouseAngle(x, y);
			this.rotation = Mathf.slerpDelta(this.rotation, angle, 0.1f);
		}
	}

	protected void updateFlying(){
		rotation = Mathf.slerpDelta(rotation, targetAngle, 0.2f);
	}

	@Override
	public Player add(){
		return add(playerGroup);
	}

    @Override
    public String toString() {
        return "Player{" + id + ", mech=" + mech.name + ", local=" + isLocal + ", " + x + ", " + y + "}\n";
    }

	@Override
	public void writeSpawn(ByteBuffer buffer) {
		buffer.put((byte)name.getBytes().length);
		buffer.put(name.getBytes());
		buffer.put(weaponLeft.id);
		buffer.put(weaponRight.id);
		buffer.put(mech.id);
		buffer.put(isAdmin ? 1 : (byte)0);
		buffer.putInt(Color.rgba8888(color));
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.put((byte)team.ordinal());
	}

	@Override
	public void readSpawn(ByteBuffer buffer) {
		byte nlength = buffer.get();
		byte[] n = new byte[nlength];
		buffer.get(n);
		name = new String(n);
		weaponLeft = Upgrade.getByID(buffer.get());
		weaponRight = Upgrade.getByID(buffer.get());
		mech = Upgrade.getByID(buffer.get());
		isAdmin = buffer.get() == 1;
		color.set(buffer.getInt());
		x = buffer.getFloat();
		y = buffer.getFloat();
		team = Team.values()[buffer.get()];
		setNet(x, y);
	}

	@Override
	public void write(ByteBuffer data) {
		if(Net.client() || isLocal) {
			data.putFloat(x);
			data.putFloat(y);
		}else{
			data.putFloat(interpolator.target.x);
			data.putFloat(interpolator.target.y);
		}
		data.putFloat(rotation);
		data.putFloat(baseRotation);
		data.putShort((short)health);
		data.put((byte)(dashing ? 1 : 0));
	}

	@Override
	public void read(ByteBuffer data, long time) {
		float x = data.getFloat();
		float y = data.getFloat();
		float rot = data.getFloat();
		float baseRot = data.getFloat();
		short health = data.getShort();
		byte dashing = data.get();

		this.health = health;
		this.dashing = dashing == 1;

		interpolator.read(this.x, this.y, x, y, rot, baseRot, time);
	}

	@Override
	public void interpolate() {
		super.interpolate();

		Interpolator i = interpolator;

		float tx = x + Angles.trnsx(rotation + 180f, 4f);
		float ty = y + Angles.trnsy(rotation + 180f, 4f);

		if(mech.flying && i.target.dst(i.last) > 2f && timer.get(timerDash, 1)){
			Effects.effect(Fx.dash, tx, ty);
		}

		if(dashing && !dead && timer.get(timerDash, 3) && i.target.dst(i.last) > 1f){
			Effects.effect(Fx.dash, tx, ty);
		}
	}

	public Color getColor(){
		return color;
	}
}
