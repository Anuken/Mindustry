package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.entities.Targetable;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.*;

public abstract class BaseUnit extends Unit{
	private static int timerIndex = 0;

	protected static final int timerTarget = timerIndex++;
	protected static final int timerBoost = timerIndex++;
	protected static final int timerReload = timerIndex++;

	protected UnitType type;
	protected Timer timer = new Timer(5);
	protected StateMachine state = new StateMachine();
	protected Targetable target;

	public BaseUnit(UnitType type, Team team){
		this.type = type;
		this.team = team;
	}

	/**internal constructor used for deserialization, DO NOT USE*/
	public BaseUnit(){}

	public void rotate(float angle){
		rotation = Mathf.slerpDelta(rotation, angle, type.rotatespeed);
	}

	public void effectAt(Effect effect, float rotation, float dx, float dy){
		Effects.effect(effect,
				x + Angles.trnsx(rotation, dx, dy),
				y + Angles.trnsy(rotation, dx, dy), Mathf.atan2(dx, dy) + rotation);
	}

	public boolean targetHasFlag(BlockFlag flag){
		return target instanceof TileEntity &&
				((TileEntity)target).tile.block().flags.contains(flag);
	}

	public void setState(UnitState state){
		this.state.set(state);
	}

	public void retarget(Runnable run){
		if(timer.get(timerTarget, 20)){
			run.run();
		}
	}

	/**Only runs when the unit has a target.*/
	public void behavior(){

	}

	public void updateTargeting(){
		if(target == null || (target instanceof Unit && (target.isDead() || ((Unit)target).team == team))
				|| (target instanceof TileEntity && ((TileEntity) target).tile.entity == null)){
			target = null;
		}
	}

	public void shoot(AmmoType type, float rotation, float translation){
		Bullet.create(type.bullet, this,
				x + Angles.trnsx(rotation, translation),
				y + Angles.trnsy(rotation, translation), rotation);
		Effects.effect(type.shootEffect, x + Angles.trnsx(rotation, translation),
				y + Angles.trnsy(rotation, translation), rotation, this);
		Effects.effect(type.smokeEffect, x + Angles.trnsx(rotation, translation),
				y + Angles.trnsy(rotation, translation), rotation, this);
	}

	public void targetClosestAllyFlag(BlockFlag flag){
		Tile target = Geometry.findClosest(x, y, world.indexer().getAllied(team, flag));
		if (target != null) this.target = target.entity;
	}

	public UnitState getStartState(){
		return null;
	}

	@Override
	public float getMaxHealth() {
		return type.health;
	}

	@Override
	public float getArmor() {
		return type.armor;
	}

	@Override
	public boolean acceptsAmmo(Item item) {
		return type.ammo.containsKey(item) && inventory.canAcceptAmmo(type.ammo.get(item));
	}

	@Override
	public void addAmmo(Item item) {
		inventory.addAmmo(type.ammo.get(item));
	}

	@Override
	public float getSize() {
		return 8;
	}

	@Override
	public void move(float x, float y){
		baseRotation = Mathf.slerpDelta(baseRotation, Mathf.atan2(x, y), type.baseRotateSpeed);
		super.move(x, y);
	}

	@Override
	public float getMass() {
		return type.mass;
	}

	@Override
	public boolean isFlying() {
		return type.isFlying;
	}

	@Override
	public void update(){
		if(hitTime > 0){
			hitTime -= Timers.delta();
		}

		if(hitTime < 0) hitTime = 0;

		if(Net.client()){
			interpolate();
			return;
		}

		updateTargeting();

		state.update();
		updateVelocityStatus(type.drag, type.maxVelocity);

		if(target != null) behavior();

		x = Mathf.clamp(x, 0, world.width() * tilesize);
		y = Mathf.clamp(y, 0, world.height() * tilesize);
	}

	@Override
	public void drawSmooth(){

	}

	@Override
	public void drawUnder(){

	}

	@Override
	public void drawOver(){

	}

	@Override
	public float drawSize(){
		return 14;
	}

	@Override
	public void onRemoteShoot(BulletType type, float x, float y, float rotation, short data) {
		Bullet.create(type, this, x, y, rotation).damage = data;
	}

	@Override
	public void onDeath(){
		super.onDeath();

		Effects.effect(ExplosionFx.explosion, this);
		Effects.shake(2f, 2f, this);

		remove();
	}

	@Override
	public void onRemoteDeath(){
		onDeath();
	}

	@Override
	public void removed(){

	}

	@Override
	public void added(){
		hitbox.solid = !isFlying();
		hitbox.setSize(type.hitsize);
		hitboxTile.setSize(type.hitsizeTile);
		state.set(getStartState());

		heal();
	}
	
	@Override
	public BaseUnit add(){
		return add(unitGroups[team.ordinal()]);
	}

	@Override
	public void writeSave(DataOutputStream stream) throws IOException {
		super.writeSave(stream);
		stream.writeByte(type.id);
	}

	@Override
	public void readSave(DataInputStream stream) throws IOException {
		super.readSave(stream);
		byte type = stream.readByte();

		this.type = UnitType.getByID(type);
		add(unitGroups[team.ordinal()]);
	}

	@Override
	public void writeSpawn(ByteBuffer buffer) {
		buffer.put(type.id);
		buffer.put((byte)team.ordinal());
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.putShort((short)health);
	}

	@Override
	public void readSpawn(ByteBuffer buffer) {
		type = UnitType.getByID(buffer.get());
		team = Team.values()[buffer.get()];
		x = buffer.getFloat();
		y = buffer.getFloat();
		health = buffer.getShort();
		setNet(x, y);
	}

	@Override
	public void write(ByteBuffer data) {
		data.putFloat(x);
		data.putFloat(y);
		data.putShort((short)(rotation *2));
		data.putShort((short)(baseRotation *2));
		data.putShort((short)health);
	}

	@Override
	public void read(ByteBuffer data, long time) {
		float x = data.getFloat();
		float y = data.getFloat();
		short rotation = data.getShort();
		short baserotation = data.getShort();
		short health = data.getShort();

		interpolator.read(this.x, this.y, x, y, rotation/2f, baserotation/2f, time);
		this.health = health;
	}
}
