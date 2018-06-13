package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Queue;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.graphics.Trail;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.storage.CoreBlock.CoreEntity;
import io.anuke.ucore.core.*;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Player extends Unit implements BuilderTrait, CarryTrait {
	private static final float debugSpeed = 1.8f;
	private static final Vector2 movement = new Vector2();

	public static int typeID = -1;

	public static final int timerShootLeft = 0;
	public static final int timerShootRight = 1;
	public static final int timeSync = 2;

	//region instance variables, constructor

	public float baseRotation;

	public float pointerX, pointerY;
	public String name = "name";
	public String uuid, usid;
	public boolean isAdmin, isTransferring, isShooting;
	public Color color = new Color();

	public Array<Upgrade> upgrades = new Array<>();
	public Weapon weapon = Weapons.blaster;
	public Mech mech = Mechs.standard;

	public int clientid = -1;
	public int playerIndex = 0;
	public boolean isLocal = false;
	public Timer timer = new Timer(4);
	public TargetTrait target;
	public CarriableTrait pickupTarget;

	private boolean respawning;
	private float walktime;
	private Queue<BuildRequest> placeQueue = new ThreadQueue<>();
	private Tile mining;
	private CarriableTrait carrying;
	private Trail trail = new Trail(16);
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(4f);

		heal();
	}

	//endregion

	//region unit and event overrides, utility methods

    @Override
    public void interpolate() {
        super.interpolate();

        if(interpolator.values.length > 1){
            baseRotation = interpolator.values[1];
        }

        if(interpolator.target.dst(interpolator.last) > 1f){
            walktime += Timers.delta();
        }
    }

	@Override
	public int getTypeID() {
		return typeID;
	}

	@Override
	public CarriableTrait getCarry() {
		return carrying;
	}

	@Override
	public void setCarry(CarriableTrait unit) {
		this.carrying = unit;
	}

	@Override
	public float getCarryWeight() {
		return mech.carryWeight;
	}

	@Override
	public float getBuildPower(Tile tile) {
		return 1f;
	}

	@Override
	public float maxHealth() {
		return 200;
	}

	@Override
	public Tile getMineTile() {
		return mining;
	}

	@Override
	public void setMineTile(Tile tile) {
		this.mining = tile;
	}

	@Override
	public float getArmor() {
		return 0f;
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
	public float getMass(){
	    return mech.mass;
    }

    @Override
	public boolean isFlying(){
		return mech.flying || noclip || isCarried();
	}

	@Override
	public float getSize() {
		return 8;
	}

	@Override
	public void damage(float amount){
		CallEntity.onPlayerDamage(this, amount);

		if(health <= 0 && !dead && isLocal){
			CallEntity.onPlayerDeath(this);
		}
	}

	@Override
	public boolean collides(SolidTrait other) {
		return super.collides(other) || other instanceof ItemDrop;
	}

	@Remote(in = In.entities, targets = Loc.server, called = Loc.server)
	public static void onPlayerDamage(Player player, float amount){
		if(player == null) return;

		player.hitTime = hitDuration;
		player.health -= amount;
	}

	@Remote(in = In.entities, targets = Loc.server, called = Loc.server)
	public static void onPlayerDeath(Player player){
		if(player == null) return;

		player.dead = true;
		player.respawning = false;
		player.placeQueue.clear();

		player.dropCarry();

		float explosiveness = 2f + (player.inventory.hasItem() ? player.inventory.getItem().item.explosiveness * player.inventory.getItem().amount : 0f);
		float flammability = (player.inventory.hasItem() ? player.inventory.getItem().item.flammability * player.inventory.getItem().amount : 0f);
		Damage.dynamicExplosion(player.x, player.y, flammability, explosiveness, 0f, player.getSize()/2f, Palette.darkFlame);
		Effects.sound("die", player);
		player.onDeath();
	}

	@Override
	public void set(float x, float y){
		this.x = x;
		this.y = y;

		if(isFlying() && isLocal){
			Core.camera.position.set(x, y, 0f);
		}
	}

	@Override
	public void removed() {
        dropCarryLocal();
	}

	@Override
	public EntityGroup targetGroup() {
		return playerGroup;
	}

	//endregion

	//region draw methods

	@Override
	public float drawSize() {
		return 40;
	}

	@Override
	public void draw(){
		if((debug && (!showPlayer || !showUI)) || dead) return;

        boolean snap = snapCamera && isLocal;

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
				Draw.rect(mech.legRegion,
						x + Angles.trnsx(baseRotation, ft * i),
						y + Angles.trnsy(baseRotation, ft * i),
						12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90);
			}

			Draw.rect(mech.baseRegion, x, y, baseRotation- 90);
		}

		if(floor.liquid) {
			Draw.tint(Color.WHITE, floor.liquidColor, drownTime * 0.4f);
		}else {
			Draw.tint(Color.WHITE);
		}

		Draw.rect(mech.region, x, y, rotation -90);

		for (int i : Mathf.signs) {
			float tra = rotation - 90,
					trX = 4*i, trY = 3 - weapon.getRecoil(this, i > 0)*1.5f;
			float w = i > 0 ? -8 : 8;
			Draw.rect(weapon.equipRegion,
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
	    if(!isShooting() && !dead) {
	        drawBuilding(this);
        }

		if(mech.flying){
	    	trail.draw(Palette.lighterOrange, Palette.lightishOrange, 5f);
		}
    }

    public void drawName(){
		GlyphLayout layout = Pools.obtain(GlyphLayout.class);

		Draw.tscl(0.25f/2);
		layout.setText(Core.font, name);
		Draw.color(0f, 0f, 0f, 0.3f);
		Draw.rect("blank", x, y + 8 - layout.height/2, layout.width + 2, layout.height + 2);
		Draw.color();
		Draw.tcolor(color);
		Draw.text(name, x, y + 8);

		if(isAdmin){
			Draw.color(color);
			float s = 3f;
			Draw.rect("icon-admin-small", x + layout.width/2f + 2 + 1, y + 7f, s, s);
		}

		Draw.reset();
		Pools.free(layout);
		Draw.tscl(fontScale);
	}

	/**Draw all current build requests. Does not draw the beam effect, only the positions.*/
	public void drawBuildRequests(){
		for (BuildRequest request : getPlaceQueue()) {

			if(request.remove){
                Block block = world.tile(request.x, request.y).target().block();

				//draw removal request
                Draw.color(Palette.remove);

                Lines.stroke((1f-request.progress));

                Lines.poly(request.x * tilesize + block.offset(),
                        request.y * tilesize + block.offset(),
                        4, block.size * tilesize /2f, 45 + 15);
			}else{
				//draw place request
				Draw.color(Palette.accent);

				Lines.stroke((1f-request.progress));

				Lines.poly(request.x * tilesize + request.recipe.result.offset(),
						request.y * tilesize + request.recipe.result.offset(),
						4, request.recipe.result.size * tilesize /2f, 45 + 15);
			}
		}

		Draw.reset();
	}

	//endregion

	//region update methods

	@Override
	public void update(){
		hitTime = Math.max(0f, hitTime - Timers.delta());

		if(isDead()){
			CoreEntity entity = (CoreEntity)getClosestCore();

			if (!respawning && entity != null) {
				entity.trySetPlayer(this);
			}
			return;
		}

		if(!isLocal){
			interpolate();
			updateBuilding(this); //building happens even with non-locals
			status.update(this); //status effect updating also happens with non locals for effect purposes

			if(Net.server()){
				updateShooting(); //server simulates player shooting
			}
			return;
		}

		if(mech.flying){
			updateFlying();
		}else{
			updateMech();
		}

		float wobblyness = 0.6f;

		trail.update(x + Angles.trnsx(rotation + 180f, 6f) + Mathf.range(wobblyness),
				y + Angles.trnsy(rotation + 180f, 6f) + Mathf.range(wobblyness));

		if(!isShooting()) {
			updateBuilding(this);
		}

		x = Mathf.clamp(x, 0, world.width() * tilesize);
		y = Mathf.clamp(y, 0, world.height() * tilesize);
	}

	protected void updateMech(){
		Tile tile = world.tileWorld(x, y);

		//if player is in solid block
		if(tile != null && tile.solid() && !noclip) {
			damage(health + 1); //die instantly
		}

		if(ui.chatfrag.chatOpen()) return;

		float speed = Inputs.keyDown("dash") && debug ? 5f : mech.speed;
		float carrySlowdown = 0.3f;

		speed *= ((1f-carrySlowdown) +  (inventory.hasItem() ? (float)inventory.getItem().amount/inventory.capacity(): 1f) * carrySlowdown);

		//drop from carrier on key press
		if(Inputs.keyTap("drop_unit") && getCarrier() != null){
			getCarrier().dropCarry();
		}

		movement.set(0, 0);

		String section = "player_" + (playerIndex + 1);

		float xa = Inputs.getAxis(section, "move_x");
		float ya = Inputs.getAxis(section, "move_y");
		if(Math.abs(xa) < 0.3) xa = 0;
		if(Math.abs(ya) < 0.3) ya = 0;

		movement.y += ya*speed;
		movement.x += xa*speed;

		Vector2 vec = Graphics.world(Vars.control.input(playerIndex).getMouseX(),
				Vars.control.input(playerIndex).getMouseY());
		pointerX = vec.x;
		pointerY = vec.y;
		updateShooting();

		movement.limit(speed);

		velocity.add(movement);

		updateVelocityStatus(0.4f, speed);

		if(!movement.isZero()){
			walktime += Timers.delta() * velocity.len()*(1f/0.5f)/speed * getFloorOn().speedMultiplier;
			baseRotation = Mathf.slerpDelta(baseRotation, movement.angle(), 0.13f);
		}

		if(!isShooting()){
			if(!movement.isZero()) {
				rotation = Mathf.slerpDelta(rotation, movement.angle(), 0.13f);
			}
		}else{
			float angle = control.input(playerIndex).mouseAngle(x, y);
			this.rotation = Mathf.slerpDelta(this.rotation, angle, 0.1f);
		}
	}

	protected void updateShooting(){
		if(isShooting()){
			weapon.update(this, true, pointerX, pointerY);
			weapon.update(this, false, pointerX, pointerY);
		}
	}

	protected void updateFlying(){
		if(Units.invalidateTarget(target, this)){
			target = null;
		}

		float targetX = Core.camera.position.x, targetY = Core.camera.position.y;
		float attractDst = 15f;

		if(pickupTarget != null && !pickupTarget.isDead()){
			targetX = pickupTarget.getX();
			targetY = pickupTarget.getY();
			attractDst = 0f;

			if(distanceTo(pickupTarget) < 2f){
				carry(pickupTarget);

				pickupTarget = null;
			}
		}else{
			pickupTarget = null;
		}

		movement.set(targetX - x, targetY - y).limit(mech.speed);
		movement.setAngle(Mathf.slerpDelta(movement.angle(), velocity.angle(), 0.05f));

		if(distanceTo(targetX, targetY) < attractDst){
			movement.setZero();
		}

		velocity.add(movement);
		updateVelocityStatus(0.1f, mech.maxSpeed);

		//hovering effect
		x += Mathf.sin(Timers.time() + id * 999, 25f, 0.08f);
		y += Mathf.cos(Timers.time() + id * 999, 25f, 0.08f);

		if(velocity.len() <= 0.2f){
			rotation += Mathf.sin(Timers.time() + id * 99, 10f, 1f);
		}else{
			rotation = Mathf.slerpDelta(rotation, velocity.angle(), velocity.len()/10f);
		}

		//update shooting if not building, not mining and there's ammo left
		if(!isBuilding() && inventory.hasAmmo() && getMineTile() == null){

			//autofire: mobile only!
			if(mobile) {
				boolean lastShooting = isShooting;

				if (target == null) {
					isShooting = false;
					target = Units.getClosestTarget(team, x, y, inventory.getAmmoRange());
				} else {
					//rotate toward and shoot the target
					rotation = Mathf.slerpDelta(rotation, angleTo(target), 0.2f);

					Vector2 intercept =
							Predict.intercept(x, y, target.getX(), target.getY(), target.getVelocity().x - velocity.x, target.getVelocity().y - velocity.y, inventory.getAmmo().bullet.speed);

					pointerX = intercept.x;
					pointerY = intercept.y;

					updateShooting();
					isShooting = true;
				}

				//update status of shooting to server
				if(lastShooting != isShooting){
					CallEntity.setShooting(isShooting);
				}
			}else if(isShooting()){ //desktop shooting, TODO
				Vector2 vec = Graphics.world(Vars.control.input(playerIndex).getMouseX(),
						Vars.control.input(playerIndex).getMouseY());
				pointerX = vec.x;
				pointerY = vec.y;

				updateShooting();
			}
		}
	}

	//endregion

	//region utility methods

	public void toggleTeam(){
		team = (team == Team.blue ? Team.red : Team.blue);
	}

	/**Resets all values of the player.*/
	public void reset(){
		weapon = Weapons.blaster;
		team = Team.blue;
		inventory.clear();
		upgrades.clear();
		placeQueue.clear();
		dead = true;
		respawning = false;
		trail.clear();
		health = maxHealth();

		add();
	}

	public boolean isShooting(){
		return isShooting && inventory.hasAmmo();
	}

	public void setRespawning(){
		respawning = true;
	}

	@Override
	public Queue<BuildRequest> getPlaceQueue(){
		return placeQueue;
	}

    @Override
    public String toString() {
        return "Player{" + id + ", mech=" + mech.name + ", local=" + isLocal + ", " + x + ", " + y + "}\n";
    }

    //endregion

    //region read and write methods

	@Override
	public void writeSave(DataOutput stream) throws IOException {
		stream.writeBoolean(isLocal);

		if(isLocal){
			stream.writeInt(playerIndex);
			super.writeSave(stream, false);

			stream.writeByte(upgrades.size);
			for(Upgrade u : upgrades){
				stream.writeByte(u.id);
			}
		}
	}

	@Override
	public void readSave(DataInput stream) throws IOException {
		boolean local = stream.readBoolean();

		if(local){
			int index = stream.readInt();
			players[index].readSaveSuper(stream);
		}
	}

	private void readSaveSuper(DataInput stream) throws IOException {
		super.readSave(stream);
		byte uamount = stream.readByte();
		for (int i = 0; i < uamount; i++) {
			upgrades.add(Upgrade.getByID(stream.readByte()));
		}

		add();
	}

	@Override
	public void write(DataOutput buffer) throws IOException {
		super.writeSave(buffer, !isLocal);
		buffer.writeUTF(name);
		buffer.writeBoolean(isAdmin);
		buffer.writeInt(Color.rgba8888(color));
		buffer.writeBoolean(dead);
		buffer.writeByte(weapon.id);
		buffer.writeByte(mech.id);
	}

	@Override
	public void read(DataInput buffer, long time) throws IOException {
		float lastx = x, lasty = y, lastrot = rotation;
		super.readSave(buffer);
		name = buffer.readUTF();
		isAdmin = buffer.readBoolean();
		color.set(buffer.readInt());
		dead = buffer.readBoolean();
		weapon = Upgrade.getByID(buffer.readByte());
		mech = Upgrade.getByID(buffer.readByte());
		interpolator.read(lastx, lasty, x, y, time, rotation);
		rotation = lastrot;

		if(isLocal){
			x = lastx;
			y = lasty;
		}
	}

	//endregion
}
