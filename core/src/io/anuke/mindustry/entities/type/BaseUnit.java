package io.anuke.mindustry.entities.type;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Core;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Angles;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.util.Interval;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.traits.ShooterTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.units.UnitFactory.UnitFactoryEntity;
import io.anuke.mindustry.world.meta.BlockFlag;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

/** Base class for AI units. */
public abstract class BaseUnit extends Unit implements ShooterTrait{

    protected static int timerIndex = 0;

    protected static final int timerTarget = timerIndex++;
    protected static final int timerTarget2 = timerIndex++;
    protected static final int timerShootLeft = timerIndex++;
    protected static final int timerShootRight = timerIndex++;

    protected UnitType type;
    protected Interval timer = new Interval(5);
    protected StateMachine state = new StateMachine();
    protected TargetTrait target;

    protected int spawner = noSpawner;

    /** internal constructor used for deserialization, DO NOT USE */
    public BaseUnit(){
    }

    @Remote(called = Loc.server)
    public static void onUnitDeath(BaseUnit unit){
        if(unit == null) return;

        if(Net.server() || !Net.active()){
            UnitDrops.dropItems(unit);
        }

        unit.onSuperDeath();

        //visual only.
        if(Net.client()){
            Tile tile = world.tile(unit.spawner);
            if(tile != null && !Net.client()){
                tile.block().unitRemoved(tile, unit);
            }

            unit.spawner = noSpawner;
        }

        //must run afterwards so the unit's group is not null when sending the removal packet
        Core.app.post(unit::remove);
    }

    @Override
    public float drag(){
        return type.drag;
    }

    public Tile getSpawner(){
        return world.tile(spawner);
    }

    /** Initialize the type and team of this unit. Only call once! */
    public void init(UnitType type, Team team){
        if(this.type != null) throw new RuntimeException("This unit is already initialized!");

        this.type = type;
        this.team = team;
    }

    public UnitType getType(){
        return type;
    }

    public void setSpawner(Tile tile){
        this.spawner = tile.pos();
    }

    public void rotate(float angle){
        rotation = Mathf.slerpDelta(rotation, angle, type.rotatespeed);
    }

    public boolean targetHasFlag(BlockFlag flag){
        return target instanceof TileEntity && ((TileEntity)target).tile.block().flags.contains(flag);
    }

    public void setState(UnitState state){
        this.state.set(state);
    }

    public void retarget(Runnable run){
        if(timer.get(timerTarget, 20)){
            run.run();
        }
    }

    /** Only runs when the unit has a target. */
    public void behavior(){

    }

    public void updateTargeting(){
        if(target == null || (target instanceof Unit && (target.isDead() || target.getTeam() == team))
        || (target instanceof TileEntity && ((TileEntity)target).tile.entity == null)){
            target = null;
        }
    }

    public void targetClosestAllyFlag(BlockFlag flag){
        Tile target = Geometry.findClosest(x, y, world.indexer.getAllied(team, flag));
        if(target != null) this.target = target.entity;
    }

    public void targetClosestEnemyFlag(BlockFlag flag){
        Tile target = Geometry.findClosest(x, y, world.indexer.getEnemy(team, flag));
        if(target != null) this.target = target.entity;
    }

    public void targetClosest(){
        TargetTrait newTarget = Units.closestTarget(team, x, y, Math.max(getWeapon().bullet.range(), type.range), u -> type.targetAir || !u.isFlying());
        if(newTarget != null){
            target = newTarget;
        }
    }

    public TileEntity getClosestEnemyCore(){

        for(Team enemy : Vars.state.teams.enemiesOf(team)){
            Tile tile = Geometry.findClosest(x, y, Vars.state.teams.get(enemy).cores);
            if(tile != null){
                return tile.entity;
            }
        }

        return null;
    }

    public UnitState getStartState(){
        return null;
    }

    protected void drawItems(){
        float backTrns = 4f;
        if(item.amount > 0){
            int stored = Mathf.clamp(item.amount / 6, 1, 8);

            for(int i = 0; i < stored; i++){
                float angT = i == 0 ? 0 : Mathf.randomSeedRange(i + 2, 60f);
                float lenT = i == 0 ? 0 : Mathf.randomSeedRange(i + 3, 1f) - 1f;
                Draw.rect(item.item.icon(Item.Icon.large),
                x + Angles.trnsx(rotation + 180f + angT, backTrns + lenT),
                y + Angles.trnsy(rotation + 180f + angT, backTrns + lenT),
                itemSize, itemSize, rotation);
            }
        }
    }

    public boolean isBoss(){
        return hasEffect(StatusEffects.boss);
    }

    @Override
    public float getDamageMultipler(){
        return status.getDamageMultiplier() * Vars.state.rules.unitDamageMultiplier;
    }

    @Override
    public boolean isImmune(StatusEffect effect){
        return type.immunities.contains(effect);
    }

    @Override
    public boolean isValid(){
        return super.isValid() && isAdded();
    }

    @Override
    public Interval getTimer(){
        return timer;
    }

    @Override
    public int getShootTimer(boolean left){
        return left ? timerShootLeft : timerShootRight;
    }

    @Override
    public Weapon getWeapon(){
        return type.weapon;
    }

    @Override
    public TextureRegion getIconRegion(){
        return type.iconRegion;
    }

    @Override
    public int getItemCapacity(){
        return type.itemCapacity;
    }

    @Override
    public void interpolate(){
        super.interpolate();

        if(interpolator.values.length > 0){
            rotation = interpolator.values[0];
        }
    }

    @Override
    public float maxHealth(){
        return type.health * Vars.state.rules.unitHealthMultiplier;
    }

    @Override
    public float mass(){
        return type.mass;
    }

    @Override
    public boolean isFlying(){
        return type.isFlying;
    }

    @Override
    public void update(){
        if(isDead()){
            //dead enemies should get immediately removed
            remove();
            return;
        }

        hitTime -= Time.delta();

        if(Net.client()){
            interpolate();
            status.update(this);
            return;
        }

        if(!isFlying() && (world.tileWorld(x, y) != null && world.tileWorld(x, y).solid())){
            kill();
        }

        avoidOthers();

        if(spawner != noSpawner && (world.tile(spawner) == null || !(world.tile(spawner).entity instanceof UnitFactoryEntity))){
            kill();
        }

        updateTargeting();

        state.update();
        updateVelocityStatus();

        if(target != null) behavior();

        if(!isFlying()){
            clampPosition();
        }
    }

    @Override
    public void draw(){

    }

    @Override
    public float maxVelocity(){
        return type.maxVelocity;
    }

    @Override
    public void removed(){
        super.removed();
        Tile tile = world.tile(spawner);
        if(tile != null && !Net.client()){
            tile.block().unitRemoved(tile, this);
        }

        spawner = noSpawner;
    }

    @Override
    public float drawSize(){
        return type.hitsize * 10;
    }

    @Override
    public void onDeath(){
        Call.onUnitDeath(this);
    }

    @Override
    public void added(){
        state.set(getStartState());

        health(maxHealth());
    }

    @Override
    public void hitbox(Rectangle rectangle){
        rectangle.setSize(type.hitsize).setCenter(x, y);
    }

    @Override
    public void hitboxTile(Rectangle rectangle){
        rectangle.setSize(type.hitsizeTile).setCenter(x, y);
    }

    @Override
    public EntityGroup targetGroup(){
        return unitGroups[team.ordinal()];
    }

    @Override
    public byte version(){
        return 0;
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        super.writeSave(stream);
        stream.writeByte(type.id);
        stream.writeInt(spawner);
    }

    @Override
    public void readSave(DataInput stream, byte version) throws IOException{
        super.readSave(stream, version);
        byte type = stream.readByte();
        this.spawner = stream.readInt();

        this.type = content.getByID(ContentType.unit, type);
        add();
    }

    @Override
    public void write(DataOutput data) throws IOException{
        super.writeSave(data);
        data.writeByte(type.id);
        data.writeInt(spawner);
    }

    @Override
    public void read(DataInput data) throws IOException{
        float lastx = x, lasty = y, lastrot = rotation;

        super.readSave(data, version());

        this.type = content.getByID(ContentType.unit, data.readByte());
        this.spawner = data.readInt();

        interpolator.read(lastx, lasty, x, y, rotation);
        rotation = lastrot;
        x = lastx;
        y = lasty;
    }

    public void onSuperDeath(){
        super.onDeath();
    }
}