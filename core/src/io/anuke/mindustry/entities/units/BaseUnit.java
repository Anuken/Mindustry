package io.anuke.mindustry.entities.units;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.CallEntity;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Timer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public abstract class BaseUnit extends Unit{
	private static int timerIndex = 0;

	protected static final int timerTarget = timerIndex++;
	protected static final int timerReload = timerIndex++;

	protected UnitType type;
	protected Timer timer = new Timer(5);
	protected StateMachine state = new StateMachine();
	protected TargetTrait target;

	protected boolean isWave;
	protected Squad squad;

	public BaseUnit(UnitType type, Team team){
		this.type = type;
		this.team = team;
	}

	/**internal constructor used for deserialization, DO NOT USE*/
	public BaseUnit(){}

	/**Sets this to a 'wave' unit, which means it has slightly different AI and will not run out of ammo.*/
	public void setWave(){
		isWave = true;
	}

	public void setSquad(Squad squad) {
		this.squad = squad;
		squad.units ++;
	}

	public void rotate(float angle){
		rotation = Mathf.slerpDelta(rotation, angle, type.rotatespeed);
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
		if(target == null || (target instanceof Unit && (target.isDead() || ((Unit)target).getTeam() == team))
				|| (target instanceof TileEntity && ((TileEntity) target).tile.entity == null)){
			target = null;
		}
	}

	public void shoot(AmmoType type, float rotation){
		CallEntity.onUnitShoot(this, type, rotation);
	}

	public void targetClosestAllyFlag(BlockFlag flag){
		if(target != null) return;

		Tile target = Geometry.findClosest(x, y, world.indexer().getAllied(team, flag));
		if (target != null) this.target = target.entity;
	}

	public void targetClosestEnemyFlag(BlockFlag flag){
		if(target != null) return;

		Tile target = Geometry.findClosest(x, y, world.indexer().getEnemy(team, flag));
		if (target != null) this.target = target.entity;
	}

	public void targetClosest(){
		if(target != null) return;

		target = Units.getClosestTarget(team, x, y, inventory.getAmmoRange());
	}

	public UnitState getStartState(){
		return null;
	}

	@Override
	public int getItemCapacity() {
		return type.itemCapacity;
	}

	@Override
	public int getAmmoCapacity() {
		return type.ammoCapacity;
	}

	@Override
	public boolean isInfiniteAmmo() {
		return isWave;
	}

	@Override
	public void interpolate() {
		super.interpolate();

		if(interpolator.values.length > 0){
			rotation = interpolator.values[0];
		}
	}

	@Override
	public float maxHealth() {
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

		avoidOthers(8f);

		if(squad != null){
			squad.update();
		}

		updateTargeting();

		state.update();
		updateVelocityStatus(type.drag, type.maxVelocity);

		if(target != null) behavior();

		if(!isWave) {
			x = Mathf.clamp(x, 0, world.width() * tilesize);
			y = Mathf.clamp(y, 0, world.height() * tilesize);
		}
	}

	@Override
	public void draw(){

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
	public void onDeath(){
		CallEntity.onUnitDeath(this);
	}

	@Override
	public void added(){
		hitbox.setSize(type.hitsize);
		hitboxTile.setSize(type.hitsizeTile);
		state.set(getStartState());

		heal();
	}

	@Override
	public EntityGroup targetGroup() {
		return unitGroups[team.ordinal()];
	}

	@Override
	public void writeSave(DataOutput stream) throws IOException {
		super.writeSave(stream);
		stream.writeByte(type.id);
		stream.writeBoolean(isWave);
	}

	@Override
	public void readSave(DataInput stream) throws IOException {
		super.readSave(stream);
		byte type = stream.readByte();
		this.isWave = stream.readBoolean();

		this.type = UnitType.getByID(type);
		add();
	}

	@Override
	public void write(DataOutput data) throws IOException{
		super.writeSave(data);
		data.writeByte(type.id);
	}

	@Override
	public void read(DataInput data, long time) throws IOException{
		float lastx = x, lasty = y, lastrot = rotation;
		super.readSave(data);
		this.type = UnitType.getByID(data.readByte());

		interpolator.read(lastx, lasty, x, y, time, rotation);
		rotation = lastrot;
	}

	public void onSuperDeath(){
		super.onDeath();
	}

	@Remote(called = Loc.server, in = In.entities)
	public static void onUnitShoot(BaseUnit unit, AmmoType type, float rotation){
		if(unit == null) return;

		Bullet.create(type.bullet, unit,
				unit.x + Angles.trnsx(rotation, unit.type.shootTranslation),
				unit.y + Angles.trnsy(rotation, unit.type.shootTranslation), rotation);
		Effects.effect(type.shootEffect, unit.x + Angles.trnsx(rotation, unit.type.shootTranslation),
				unit.y + Angles.trnsy(rotation, unit.type.shootTranslation), rotation, unit);
		Effects.effect(type.smokeEffect, unit.x + Angles.trnsx(rotation, unit.type.shootTranslation),
				unit.y + Angles.trnsy(rotation, unit.type.shootTranslation), rotation, unit);
	}

	@Remote(called = Loc.server, in = In.entities)
	public static void onUnitDeath(BaseUnit unit){
		if(unit == null) return;

		unit.onSuperDeath();
		UnitDrops.dropItems(unit);

		Effects.effect(ExplosionFx.explosion, unit);
		Effects.shake(2f, 2f, unit);

		//must run afterwards so the unit's group is not null
		threads.runDelay(unit::remove);
	}
}
