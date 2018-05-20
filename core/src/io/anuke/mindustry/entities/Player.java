package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.mindustry.resource.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public class Player extends Unit implements BlockBuilder {
	private static final float speed = 1.1f;
	private static final float dashSpeed = 1.8f;
	private static final Vector2 movement = new Vector2();

	public String name = "name";
	public String uuid;
	public boolean isAdmin;
	public Color color = new Color();

	public Array<Upgrade> upgrades = new Array<>();
	public Weapon weapon = Weapons.blaster;
	public Mech mech = Mechs.standard;

	public float targetAngle = 0f;
	public boolean dashing = false;

	public int clientid = -1;
	public int playerIndex = 0;
	public boolean isLocal = false;
	public Timer timer = new Timer(4);

	private float walktime;
	private float respawntime;
	private Queue<BuildRequest> placeQueue = new Queue<>();
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(4f);
		
		maxhealth = 200;
		heal();
	}

	@Override
	public void onRemoteShoot(BulletType type, float x, float y, float rotation, short data) {
		Weapon weapon = Upgrade.getByID(Bits.getLeftByte(data));
		weapon.shoot(this, x, y, rotation, Bits.getRightByte(data) == 1);
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
		dead = true;
		if(Net.active()){
			NetEvents.handleUnitDeath(this);
		}

		float explosiveness = 2f + (inventory.hasItem() ? inventory.getItem().item.explosiveness * inventory.getItem().amount : 0f);
		float flammability = (inventory.hasItem() ? inventory.getItem().item.flammability * inventory.getItem().amount : 0f);
		DamageArea.dynamicExplosion(x, y, flammability, explosiveness, 0f, getSize()/2f, Palette.darkFlame);
		Effects.sound("die", this);

		respawntime = respawnduration;
		super.onDeath();
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
				Draw.rect(mname + "-leg",
						x + Angles.trnsx(baseRotation, ft * i),
						y + Angles.trnsy(baseRotation, ft * i),
						12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
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
			float tra = rotation - 90,
					trX = 4*i, trY = 3 - weapon.getRecoil(this, i > 0)*1.5f;
			float w = i > 0 ? -8 : 8;
			Draw.rect(weapon.name + "-equip",
					x + Angles.trnsx(tra, trX, trY),
					y + Angles.trnsy(tra, trX, trY), w, 8, rotation - 90);
		}

		float backTrns = 4f, itemSize = 5f;
		if(inventory.hasItem()){
			ItemStack stack = inventory.getItem();
			int stored = Mathf.clamp(stack.amount / 6, 1, 8);

			for(int i = 0; i < stored; i ++) {
				float angT = i == 0 ? 0 : Mathf.randomSeedRange(i + 1, 60f);
				float lenT = i == 0 ? 0 : Mathf.randomSeedRange(i + 2, 1f) - 1f;
				Draw.rect(stack.item.region,
						x + Angles.trnsx(rotation + 180f + angT, backTrns + lenT),
						y + Angles.trnsy(rotation + 180f + angT, backTrns + lenT),
						itemSize, itemSize, rotation);
			}
		}

		Draw.alpha(1f);

		x = px;
		y = py;
	}

	@Override
	public void drawOver(){
	    if(!isShooting() && getCurrentRequest() != null) {
	        drawBuilding(this);
        }
    }

    public void drawName(){
		GlyphLayout layout = Pools.obtain(GlyphLayout.class);

		Draw.tscl(0.25f/2);
		layout.setText(Core.font, name);
		Draw.color(0f, 0f, 0f, 0.3f);
		Draw.rect("blank", getDrawPosition().x, getDrawPosition().y + 8 - layout.height/2, layout.width + 2, layout.height + 2);
		Draw.color();
		Draw.tcolor(color);
		Draw.text(name, getDrawPosition().x, getDrawPosition().y + 8);

		if(isAdmin){
			Draw.color(color);
			float s = 3f;
			Draw.rect("icon-admin-small", getDrawPosition().x + layout.width/2f + 2 + 1, getDrawPosition().y + 7f, s, s);
		}

		Draw.reset();
		Pools.free(layout);
		Draw.tscl(fontscale);
	}

	public void drawBuildRequests(){
		for (BuildRequest request : getPlaceQueue()) {
			if(request.remove){
				Draw.color("placeInvalid");
				Tile tile = world.tile(request.x, request.y);

				Lines.poly(tile.drawx(), tile.drawy(),
						4, tile.block().size * tilesize /2f + Mathf.absin(Timers.time(), 3f, 1f));
			}else{
				Draw.color("accent");
				Lines.poly(request.x * tilesize + request.recipe.result.getPlaceOffset().x,
						request.y * tilesize + request.recipe.result.getPlaceOffset().y,
						4, request.recipe.result.size * tilesize /2f + Mathf.absin(Timers.time(), 3f, 1f));
			}
		}
	}
	
	@Override
	public void update(){
		hitTime = Math.max(0f, hitTime - Timers.delta());

		if(!isLocal){
			interpolate();
			return;
		}

        if(respawntime > 0){

            respawntime -= Timers.delta();

            if(respawntime <= 0){
                set(world.getSpawnX(), world.getSpawnY());
                heal();
                add();
                Effects.sound("respawn");
            }
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

	/**Resets all values of the player.*/
	public void reset(){
		weapon = Weapons.blaster;
		team = Team.blue;
		respawntime = -1;
		inventory.clear();
		upgrades.clear();
		placeQueue.clear();

		add();
		heal();
	}

	public boolean isShooting(){
	    return control.input(playerIndex).canShoot() && control.input(playerIndex).isShooting() && inventory.hasAmmo();
    }

    public Queue<BuildRequest> getPlaceQueue(){
	    return placeQueue;
    }

	protected void updateMech(){
		Tile tile = world.tileWorld(x, y);

		//if player is in solid block
		if(tile != null && tile.solid()) {
			damage(health + 1); //die instantly
		}

		if(!isShooting()) {
		    updateBuilding(this);
        }

		if(ui.chatfrag.chatOpen()) return;

		dashing = Inputs.keyDown("dash");

		float speed = dashing ? (debug ? Player.dashSpeed * 5f : Player.dashSpeed) : Player.speed ;

		float carrySlowdown = 0.3f;

		speed *= ((1f-carrySlowdown) +  (inventory.hasItem() ? (float)inventory.getItem().amount/inventory.capacity(): 1f) * carrySlowdown);

		movement.set(0, 0);

		String section = "player_" + (playerIndex + 1);

		float xa = Inputs.getAxis(section, "move_x");
		float ya = Inputs.getAxis(section, "move_y");
		if(Math.abs(xa) < 0.3) xa = 0;
		if(Math.abs(ya) < 0.3) ya = 0;

		movement.y += ya*speed;
		movement.x += xa*speed;

		boolean shooting = isShooting();

		if(shooting){
			weapon.update(this, true);
			weapon.update(this, false);
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
			float angle = control.input(playerIndex).mouseAngle(x, y);
			this.rotation = Mathf.slerpDelta(this.rotation, angle, 0.1f);
		}
	}

	protected void updateFlying(){
		rotation = Mathf.slerpDelta(rotation, targetAngle, 0.2f);
	}

	@Override
	public boolean acceptsAmmo(Item item) {
		return weapon.getAmmoType(item) != null && inventory.canAcceptAmmo(weapon.getAmmoType(item));
	}

	@Override
	public void addAmmo(Item item) {
		inventory.addAmmo(weapon.getAmmoType(item));
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
	public void writeSave(DataOutputStream stream) throws IOException {
		stream.writeBoolean(isLocal);

		if(isLocal){
			stream.writeInt(playerIndex);
			super.writeSave(stream);

			stream.writeByte(upgrades.size);
			for(Upgrade u : upgrades){
				stream.writeByte(u.id);
			}
		}
	}

	@Override
	public void readSave(DataInputStream stream) throws IOException {
		boolean local = stream.readBoolean();

		if(local){
			int index = stream.readInt();
			players[index].readSaveSuper(stream);
		}
	}

	private void readSaveSuper(DataInputStream stream) throws IOException {
		super.readSave(stream);

		byte uamount = stream.readByte();
		for (int i = 0; i < uamount; i++) {
			upgrades.add(Upgrade.getByID(stream.readByte()));
		}

		add();
	}

	@Override
	public void writeSpawn(ByteBuffer buffer) {
		IOUtils.writeString(buffer, name);
		buffer.put(weapon.id);
		buffer.put(mech.id);
		buffer.put(isAdmin ? 1 : (byte)0);
		buffer.putInt(Color.rgba8888(color));
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.put((byte)team.ordinal());
	}

	@Override
	public void readSpawn(ByteBuffer buffer) {
		name = IOUtils.readString(buffer);
		weapon = Upgrade.getByID(buffer.get());
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
}
