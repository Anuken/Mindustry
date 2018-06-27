package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Queue;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.effect.ItemDrop;
import io.anuke.mindustry.entities.effect.ScorchDecal;
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

public class Player extends Unit implements BuilderTrait, CarryTrait, ShooterTrait {
	private static final int timerShootLeft = 0;
	private static final int timerShootRight = 1;

	public static final int timerSync = 2;

	//region instance variables, constructor

	public float baseRotation;

	public float pointerX, pointerY;
	public String name = "name";
	public String uuid, usid;
	public boolean isAdmin, isTransferring, isShooting, isBoosting, isMobile;
	public float boostHeat;
	public Color color = new Color();
	public Mech mech;

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
	private Trail trail = new Trail(12);
	private Vector2 movement = new Vector2();
	private boolean moved;
	
	public Player(){
		hitbox.setSize(5);
		hitboxTile.setSize(4f);
	}

	//endregion

	//region unit and event overrides, utility methods


	@Override
	public Timer getTimer() {
		return timer;
	}

	@Override
	public int getShootTimer(boolean left) {
		return left ? timerShootLeft : timerShootRight;
	}

	@Override
	public Weapon getWeapon() {
		return mech.weapon;
	}

	@Override
	public float getMinePower() {
		return mech.mineSpeed;
	}

	@Override
	public TextureRegion getIconRegion() {
		return mech.iconRegion;
	}

	@Override
	public int getItemCapacity() {
		return mech.itemCapacity;
	}

	@Override
	public int getAmmoCapacity() {
		return mech.ammoCapacity;
	}

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
		return mech.buildPower;
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
		return mech.armor;
	}

	@Override
	public boolean acceptsAmmo(Item item) {
		return mech.weapon.getAmmoType(item) != null && inventory.canAcceptAmmo(mech.weapon.getAmmoType(item));
	}

	@Override
	public void addAmmo(Item item) {
		inventory.addAmmo(mech.weapon.getAmmoType(item));
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
		CallEntity.onPlayerDamage(this, calculateDamage(amount));

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
		ScorchDecal.create(player.x, player.y);
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
		return isLocal ? Float.MAX_VALUE : 40;
	}

	@Override
	public void draw(){
		if((debug && (!showPlayer || !showUI)) || dead) return;

		if(!movement.isZero() && moved){
			walktime += Timers.delta() * movement.len()/0.7f * getFloorOn().speedMultiplier;
			baseRotation = Mathf.slerpDelta(baseRotation, movement.angle(), 0.13f);
		}

		boostHeat = Mathf.lerpDelta(boostHeat, isBoosting && ((!movement.isZero() && moved) || !isLocal) ? 1f : 0f, 0.08f);

        boolean snap = snapCamera && isLocal;

		float px = x, py =y;

		if(snap){
			x = (int)x;
			y = (int)y;
		}

		float ft = Mathf.sin(walktime, 6f, 2f) * (1f-boostHeat);

		Floor floor = getFloorOn();

		Draw.color();
		Draw.alpha(hitTime / hitDuration);

		if(!mech.flying) {
			if(floor.isLiquid){
				Draw.tint(Color.WHITE, floor.liquidColor, 0.5f);
			}

			float boostTrnsY = -boostHeat * 3f;
			float boostTrnsX = boostHeat * 3f;
			float boostAng = boostHeat*40f;

			for (int i : Mathf.signs) {
				Draw.rect(mech.legRegion,
						x + Angles.trnsx(baseRotation, ft * i + boostTrnsY, -boostTrnsX*i),
						y + Angles.trnsy(baseRotation, ft * i + boostTrnsY, -boostTrnsX*i),
						12f * i, 12f - Mathf.clamp(ft * i, 0, 2), baseRotation - 90 + boostAng*i);
			}

			Draw.rect(mech.baseRegion, x, y, baseRotation- 90);
		}

		if(floor.isLiquid) {
			Draw.tint(Color.WHITE, floor.liquidColor, drownTime * 0.4f);
		}else {
			Draw.tint(Color.WHITE);
		}

		Draw.rect(mech.region, x, y, rotation -90);

		for (int i : Mathf.signs) {
			float tra = rotation - 90, trY = - mech.weapon.getRecoil(this, i > 0) + mech.weaponOffsetY;
			float w = i > 0 ? -12 : 12;
			Draw.rect(mech.weapon.equipRegion,
					x + Angles.trnsx(tra, mech.weaponOffsetX * i, trY),
					y + Angles.trnsy(tra, mech.weaponOffsetX * i, trY), w, 12, rotation - 90);
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
		if(dead) return;

	    if(!isShooting()) {
	        drawBuilding(this);
        }

		if(mech.flying || boostHeat > 0.001f){
			float wobblyness = 0.6f;
			trail.update(x + Angles.trnsx(rotation + 180f, 5f) + Mathf.range(wobblyness),
					y + Angles.trnsy(rotation + 180f, 5f) + Mathf.range(wobblyness));
	    	trail.draw(mech.trailColor, mech.trailColor, 5f * (isFlying() ? 1f : boostHeat));
		}else{
	    	trail.clear();
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
			isBoosting = false;
			boostHeat = 0f;
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

		if(mobile){
			updateFlying();
		}else{
			updateMech();
		}

		avoidOthers(8f);

		if(!isShooting()) {
			updateBuilding(this);
		}

		x = Mathf.clamp(x, 0, world.width() * tilesize);
		y = Mathf.clamp(y, 0, world.height() * tilesize);
	}

	protected void updateMech(){
		Tile tile = world.tileWorld(x, y);

		//if player is in solid block
		if(!mech.flying && tile != null && tile.solid() && !noclip) {
			damage(health + 1); //die instantly
		}

		if(ui.chatfrag.chatOpen()) return;

		float speed = isBoosting && !mech.flying ? mech.boostSpeed : mech.speed;
		//fraction of speed when at max load
		float carrySlowdown = 0.7f;

		speed *= ((inventory.hasItem() ? Mathf.lerp(1f, carrySlowdown, (float)inventory.getItem().amount/inventory.capacity()) : 1f));

		if(mech.flying){
			//prevent strafing backwards, have a penalty for doing so
			float angDist = Angles.angleDist(rotation, velocity.angle()) / 180f;
			float penalty = 0.2f; //when going 180 degrees backwards, reduce speed to 0.2x
			speed *= Mathf.lerp(1f, penalty, angDist);
		}

		//drop from carrier on key press
		if(Inputs.keyTap("drop_unit")){
			if(!mech.flying) {
				if (getCarrier() != null) {
					CallEntity.dropSelf(this);
				}
			}else if(getCarry() != null){
				dropCarry();
			}else{
				Unit unit = Units.getClosest(team, x, y, 8f,
						u -> !u.isFlying() && u.getMass() <= mech.carryWeight);

				if(unit != null){
					carry(unit);
				}
			}
		}

		movement.setZero();

		String section = control.input(playerIndex).section;

		float xa = Inputs.getAxis(section, "move_x");
		float ya = Inputs.getAxis(section, "move_y");

		movement.y += ya*speed;
		movement.x += xa*speed;

		Vector2 vec = Graphics.world(Vars.control.input(playerIndex).getMouseX(),
				Vars.control.input(playerIndex).getMouseY());
		pointerX = vec.x;
		pointerY = vec.y;
		updateShooting();

		movement.limit(speed);

		velocity.add(movement);

		float prex = x, prey = y;
		updateVelocityStatus(mech.drag, 10f);
		moved = distanceTo(prex, prey) > 0.01f;

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
			mech.weapon.update(this, pointerX, pointerY);
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
		updateVelocityStatus(mech.drag, mech.maxSpeed);

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
		team = Team.blue;
		inventory.clear();
		placeQueue.clear();
		dead = true;
		respawning = false;
		trail.clear();
		health = maxHealth();

		add();
	}

	public boolean isShooting(){
		return isShooting && inventory.hasAmmo() && (!isBoosting || mech.flying);
	}

	public void setRespawning(){
		respawning = true;
	}

	public void setRespawning(boolean respawning){
		this.respawning = respawning;
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
			stream.writeByte(mech.id);
			stream.writeByte(playerIndex);
			super.writeSave(stream, false);
		}
	}

	@Override
	public void readSave(DataInput stream) throws IOException {
		boolean local = stream.readBoolean();

		if(local){
			byte mechid = stream.readByte();
			int index = stream.readByte();
			players[index].readSaveSuper(stream);
			players[index].mech = Upgrade.getByID(mechid);
			players[index].dead = false;
		}
	}

	private void readSaveSuper(DataInput stream) throws IOException {
		super.readSave(stream);

		add();
	}

	@Override
	public void write(DataOutput buffer) throws IOException {
		super.writeSave(buffer, !isLocal);
		buffer.writeUTF(name); //TODO writing strings is very inefficient
		buffer.writeBoolean(isAdmin);
		buffer.writeInt(Color.rgba8888(color));
		buffer.writeBoolean(dead);
		buffer.writeByte(mech.id);
		buffer.writeBoolean(isBoosting);
		buffer.writeInt(mining == null ? -1 : mining.packedPosition());
	}

	@Override
	public void read(DataInput buffer, long time) throws IOException {
		float lastx = x, lasty = y, lastrot = rotation;
		super.readSave(buffer);
		name = buffer.readUTF();
		isAdmin = buffer.readBoolean();
		color.set(buffer.readInt());
		dead = buffer.readBoolean();
		mech = Upgrade.getByID(buffer.readByte());
		boolean boosting = buffer.readBoolean();
		int mine = buffer.readInt();
		interpolator.read(lastx, lasty, x, y, time, rotation);
		rotation = lastrot;

		if(isLocal){
			x = lastx;
			y = lasty;
		}else{
			mining = world.tile(mine);
			isBoosting = boosting;
		}
	}

	//endregion
}
