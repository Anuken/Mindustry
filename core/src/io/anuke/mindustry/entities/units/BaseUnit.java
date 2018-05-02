package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.unitGroups;

public class BaseUnit extends Unit{
	public UnitType type;
	public Timer timer = new Timer(5);
	public float walkTime = 0f;
	public StateMachine state = new StateMachine();
	public Entity target;

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
		this.state.set(this, state);
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
		type.update(this);
	}

	@Override
	public void drawSmooth(){
		type.draw(this);
	}

	@Override
	public void drawUnder(){
		type.drawUnder(this);
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
		type.onDeath(this);
	}

	@Override
	public void onRemoteDeath(){
		type.onRemoteDeath(this);
	}

	@Override
	public void removed(){
		type.removed(this);
	}

	@Override
	public void added(){
		maxhealth = type.health;

		hitbox.solid = !isFlying();
		hitbox.setSize(type.hitsize);
		hitboxTile.setSize(type.hitsizeTile);
		state.set(this, type.getStartState());

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
