package io.anuke.mindustry.entities.units;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.effect.ScorchDecal;
import io.anuke.mindustry.entities.traits.ShooterTrait;
import io.anuke.mindustry.entities.traits.SpawnerTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.units.CommandCenter.CommandCenterEntity;
import io.anuke.mindustry.world.meta.BlockFlag;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

/**Base class for AI units.*/
public abstract class BaseUnit extends Unit implements ShooterTrait{

    protected static int timerIndex = 0;

    protected static final int timerTarget = timerIndex++;
    protected static final int timerShootLeft = timerIndex++;
    protected static final int timerShootRight = timerIndex++;

    protected UnitType type;
    protected Timer timer = new Timer(5);
    protected StateMachine state = new StateMachine();
    protected TargetTrait target;

    protected boolean isWave;
    protected Squad squad;
    protected int spawner = -1;

    /**internal constructor used for deserialization, DO NOT USE*/
    public BaseUnit(){
    }

    @Remote(called = Loc.server)
    public static void onUnitDeath(BaseUnit unit){
        if(unit == null) return;

        if(Net.server() || !Net.active()){
            UnitDrops.dropItems(unit);
        }

        float explosiveness = 2f + (unit.inventory.hasItem() ? unit.inventory.getItem().item.explosiveness * unit.inventory.getItem().amount : 0f);
        float flammability = (unit.inventory.hasItem() ? unit.inventory.getItem().item.flammability * unit.inventory.getItem().amount : 0f);
        Damage.dynamicExplosion(unit.x, unit.y, flammability, explosiveness, 0f, unit.getSize() / 2f, Palette.darkFlame);

        unit.onSuperDeath();

        ScorchDecal.create(unit.x, unit.y);
        Effects.effect(ExplosionFx.explosion, unit);
        Effects.shake(2f, 2f, unit);

        //must run afterwards so the unit's group is not null
        threads.runDelay(unit::remove);
    }

    @Override
    public float getDrag(){
        return type.drag;
    }

    /**Called when a command is recieved from the command center.*/
    public abstract void onCommand(UnitCommand command);

    /**Initialize the type and team of this unit. Only call once!*/
    public void init(UnitType type, Team team){
        if(this.type != null) throw new RuntimeException("This unit is already initialized!");

        this.type = type;
        this.team = team;
    }

    public boolean isCommanded(){
        return !isWave && world.indexer.getAllied(team, BlockFlag.comandCenter).size != 0 && world.indexer.getAllied(team, BlockFlag.comandCenter).first().entity instanceof CommandCenterEntity;
    }

    public UnitCommand getCommand(){
        if(isCommanded()){
            return world.indexer.getAllied(team, BlockFlag.comandCenter).first().<CommandCenterEntity>entity().command;
        }
        return null;
    }

    public UnitType getType(){
        return type;
    }

    public Tile getSpawner(){
        return world.tile(spawner);
    }

    public void setSpawner(Tile tile){
        this.spawner = tile.packedPosition();
    }

    public void setIntSpawner(int pos){
        this.spawner = pos;
    }

    /**Sets this to a 'wave' unit, which means it has slightly different AI and will not run out of ammo.*/
    public void setWave(){
        isWave = true;
    }

    public void setSquad(Squad squad){
        this.squad = squad;
        squad.units++;
    }

    public void rotate(float angle){
        rotation = Mathf.slerpDelta(rotation, angle, type.rotatespeed);
    }

    public boolean targetHasFlag(BlockFlag flag){
        return target instanceof TileEntity && ((TileEntity) target).tile.block().flags != null &&
            ((TileEntity) target).tile.block().flags.contains(flag);
    }

    public void updateRespawning(){
        if(spawner == -1) return;

        Tile tile = world.tile(spawner);
        if(tile != null && tile.entity != null){
            if(tile.entity instanceof SpawnerTrait){
                ((SpawnerTrait) tile.entity).updateSpawning(this);
            }
        }else{
            spawner = -1;
        }
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
        if(target == null || (target instanceof Unit && (target.isDead() || target.getTeam() == team))
        || (target instanceof TileEntity && ((TileEntity) target).tile.entity == null)){
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
        target = Units.getClosestTarget(team, x, y, Math.max(getWeapon().getAmmo().getRange(), type.range), u -> type.targetAir || !u.isFlying());
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
        float backTrns = 4f, itemSize = 5f;
        if(inventory.hasItem()){
            ItemStack stack = inventory.getItem();
            int stored = Mathf.clamp(stack.amount / 6, 1, 8);

            for(int i = 0; i < stored; i++){
                float angT = i == 0 ? 0 : Mathf.randomSeedRange(i + 2, 60f);
                float lenT = i == 0 ? 0 : Mathf.randomSeedRange(i + 3, 1f) - 1f;
                Draw.rect(stack.item.region,
                    x + Angles.trnsx(rotation + 180f + angT, backTrns + lenT),
                    y + Angles.trnsy(rotation + 180f + angT, backTrns + lenT),
                    itemSize, itemSize, rotation);
            }
        }
    }

    @Override
    public boolean isValid(){
        return super.isValid() && isAdded();
    }

    @Override
    public Timer getTimer(){
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
        return type.health;
    }

    @Override
    public float getArmor(){
        return type.armor;
    }

    @Override
    public float getSize(){
        return 8;
    }

    @Override
    public float getMass(){
        return type.mass;
    }

    @Override
    public boolean isFlying(){
        return type.isFlying;
    }

    @Override
    public void update(){
        hitTime -= Timers.delta();

        if(isDead()){
            updateRespawning();
            return;
        }

        if(Net.client()){
            interpolate();
            status.update(this);
            return;
        }

        avoidOthers(1.25f);

        if(spawner != -1 && (world.tile(spawner) == null || world.tile(spawner).entity == null)){
            damage(health);
        }

        if(squad != null){
            squad.update();
        }

        updateTargeting();

        state.update();
        updateVelocityStatus();

        if(target != null) behavior();

        x = Mathf.clamp(x, tilesize, world.width() * tilesize - tilesize);
        y = Mathf.clamp(y, tilesize, world.height() * tilesize - tilesize);
    }

    @Override
    public void draw(){

    }

    @Override
    public float getMaxVelocity(){
        return type.maxVelocity;
    }

    @Override
    public void removed(){
        spawner = -1;
    }

    @Override
    public float drawSize(){
        return 14;
    }

    @Override
    public void onDeath(){
        Call.onUnitDeath(this);
    }

    @Override
    public void added(){
        state.set(getStartState());

        health(maxHealth());

        if(isCommanded()){
            onCommand(getCommand());
        }
    }

    @Override
    public void getHitbox(Rectangle rectangle){
        rectangle.setSize(type.hitsize).setCenter(x, y);
    }

    @Override
    public void getHitboxTile(Rectangle rectangle){
        rectangle.setSize(type.hitsizeTile).setCenter(x, y);
    }

    @Override
    public EntityGroup targetGroup(){
        return unitGroups[team.ordinal()];
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        super.writeSave(stream);
        stream.writeByte(type.id);
        stream.writeBoolean(isWave);
        stream.writeInt(spawner);
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        super.readSave(stream);
        byte type = stream.readByte();
        this.isWave = stream.readBoolean();
        this.spawner = stream.readInt();

        this.type = content.getByID(ContentType.unit, type);
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
        this.type = content.getByID(ContentType.unit, data.readByte());

        interpolator.read(lastx, lasty, x, y, time, rotation);
        rotation = lastrot;
    }

    public void onSuperDeath(){
        super.onDeath();
    }
}