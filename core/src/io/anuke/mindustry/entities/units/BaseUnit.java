package io.anuke.mindustry.entities.units;

import io.anuke.mindustry.entities.Bullet;
import io.anuke.mindustry.entities.Unit;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Timer;

import java.nio.ByteBuffer;

import static io.anuke.mindustry.Vars.unitGroups;

public class BaseUnit extends Unit {
	public UnitType type;
	public Timer timer = new Timer(5);
	public Entity target;

	public BaseUnit(UnitType type){
		this.type = type;
	}

	/**internal constructor used for deserialization, DO NOT USE*/
	public BaseUnit(){}

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
	public boolean collides(SolidEntity other){
		return (other instanceof Bullet) && !(((Bullet) other).owner instanceof BaseUnit);
	}

	@Override
	public void damage(int amount){
		super.damage(amount);
		hitTime = hitDuration;
	}

	@Override
	public void onDeath(){
		type.onDeath(this, false);
	}

	@Override
	public void removed(){
		type.removed(this);
	}

	@Override
	public void added(){
		hitbox.setSize(type.hitsize);
		hitboxTile.setSize(type.hitsizeTile);
	}
	
	@Override
	public BaseUnit add(){
		return add(unitGroups[team.ordinal()]);
	}

	@Override
	public void writeSpawn(ByteBuffer buffer) {
		buffer.put(type.id);
		buffer.putFloat(x);
		buffer.putFloat(y);
		buffer.putShort((short)health);
	}

	@Override
	public void readSpawn(ByteBuffer buffer) {
		type = UnitType.getByID(buffer.get());
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
		data.putShort((short)health);
	}

	@Override
	public void read(ByteBuffer data, long time) {
		float x = data.getFloat();
		float y = data.getFloat();
		short angle = data.getShort();
		short health = data.getShort();

		this.health = health;
		interpolator.read(this.x, this.y, x, y, angle/2f, time);
	}
}
