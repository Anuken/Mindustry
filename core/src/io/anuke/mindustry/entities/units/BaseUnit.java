package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.Team;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.unitGroups;

public class BaseUnit extends Unit {
	public UnitType type;
	public Timer timer = new Timer(5);
	public float walkTime = 0f;
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

	//TODO
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
	public void drawOver(){
		type.drawOver(this);
	}

	@Override
	public float drawSize(){
		return 14;
	}

	@Override
	public void onRemoteShoot(BulletType type, float x, float y, float rotation, short data) {
		new Bullet(type, this, x, y, rotation).add().damage = data;
	}

	@Override
	public void onDeath(){
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

		hitbox.solid = true;
		hitbox.setSize(type.hitsize);
		hitboxTile.setSize(type.hitsizeTile);

		heal();
	}
	
	@Override
	public BaseUnit add(){
		return add(unitGroups[team.ordinal()]);
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
