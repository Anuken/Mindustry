package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.TeamInfo.TeamData;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.impl.DestructibleEntity;
import io.anuke.ucore.entities.trait.DamageTrait;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public abstract class Unit extends DestructibleEntity implements SaveTrait, TargetTrait, SyncTrait, DrawTrait, TeamTrait, CarriableTrait {
    /**total duration of hit flash effect*/
    public static final float hitDuration = 9f;
    /**Percision divisor of velocity, used when writing. For example a value of '2' would mean the percision is 1/2 = 0.5-size chunks.*/
    public static final float velocityPercision = 8f;
    /**Maximum absolute value of a velocity vector component.*/
    public static final float maxAbsVelocity = 127f/velocityPercision;

    public UnitInventory inventory = new UnitInventory(this);
    public float rotation;

    protected Interpolator interpolator = new Interpolator();
    protected StatusController status = new StatusController();
    protected Team team = Team.blue;

    protected CarryTrait carrier;
    protected Vector2 velocity = new Vector2(0f, 0.0001f);
    protected float hitTime;
    protected float drownTime;

    @Override
    public void setCarrier(CarryTrait carrier) {
        this.carrier = carrier;
    }

    @Override
    public CarryTrait getCarrier() {
        return carrier;
    }

    @Override
    public Team getTeam(){
        return team;
    }

    @Override
    public void interpolate() {
        interpolator.update();

        x = interpolator.pos.x;
        y = interpolator.pos.y;

        if(interpolator.values.length > 0){
            rotation = interpolator.values[0];
        }
    }

    @Override
    public Interpolator getInterpolator() {
        return interpolator;
    }

    @Override
    public void damage(float amount){
        super.damage(calculateDamage(amount));
        hitTime = hitDuration;
    }

    @Override
    public boolean collides(SolidTrait other){
        return other instanceof DamageTrait && other
                instanceof TeamTrait && state.teams.areEnemies((((TeamTrait) other).getTeam()), team) && !isDead();
    }

    @Override
    public void onDeath() {
        inventory.clear();
        drownTime = 0f;
        status.clear();
    }

    @Override
    public Vector2 getVelocity() {
        return velocity;
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException {
        writeSave(stream, false);
    }

    @Override
    public void readSave(DataInput stream) throws IOException {
        byte team = stream.readByte();
        float x = stream.readFloat();
        float y = stream.readFloat();
        byte xv = stream.readByte();
        byte yv = stream.readByte();
        float rotation = stream.readShort()/2f;
        int health = stream.readShort();

        this.status.readSave(stream);
        this.inventory.readSave(stream);
        this.team = Team.values()[team];
        this.health = health;
        this.x = x;
        this.y = y;
        this.velocity.set(xv / velocityPercision, yv / velocityPercision);
        this.rotation = rotation;
    }

    public void writeSave(DataOutput stream, boolean net) throws IOException {
        stream.writeByte(team.ordinal());
        stream.writeFloat(net ? interpolator.target.x : x);
        stream.writeFloat(net ? interpolator.target.y : y);
        stream.writeByte((byte)(Mathf.clamp(velocity.x, -maxAbsVelocity, maxAbsVelocity) * velocityPercision));
        stream.writeByte((byte)(Mathf.clamp(velocity.y, -maxAbsVelocity, maxAbsVelocity) * velocityPercision));
        stream.writeShort((short)(rotation*2));
        stream.writeShort((short)health);
        status.writeSave(stream);
        inventory.writeSave(stream);
    }

    public float calculateDamage(float amount){
        return amount * Mathf.clamp(1f-getArmor()/100f*status.getArmorMultiplier());
    }

    public float getDamageMultipler(){
        return status.getDamageMultiplier();
    }

    public boolean hasEffect(StatusEffect effect){
        return status.hasEffect(effect);
    }

    public TileEntity getClosestCore(){
        if(state.teams.has(team)){
            TeamData data = state.teams.get(team);

            Tile tile =  Geometry.findClosest(x, y, data.cores);
            if(tile == null){
                return null;
            }else{
                return tile.entity;
            }
        }else{
            return null;
        }
    }

    public Floor getFloorOn(){
        Tile tile = world.tileWorld(x, y);
        return tile == null ? (Floor) Blocks.air : tile.floor();
    }

    /**Updates velocity and status effects.*/
    public void updateVelocityStatus(float drag, float maxVelocity){
        if(isCarried()){ //carried units do not take into account velocity normally
            set(carrier.getX(), carrier.getY());
            velocity.set(carrier.getVelocity());
            return;
        }

        Floor floor = getFloorOn();
        Tile tile = world.tileWorld(x, y);

        status.update(this);

        velocity.limit(maxVelocity).scl(status.getSpeedMultiplier());

        if(isFlying()) {
            x += velocity.x / getMass() * Timers.delta();
            y += velocity.y / getMass() * Timers.delta();
        }else{
            boolean onLiquid = floor.isLiquid;

            if(tile != null){
                tile.block().unitOn(tile, this);
                if(tile.block() != Blocks.air){
                    onLiquid = false;
                }

                //on slope
                if(tile.elevation == -1){
                    velocity.scl(0.7f);
                }
            }

            if(onLiquid && velocity.len() > 0.4f && Timers.get(this, "flooreffect", 14 - (velocity.len() * floor.speedMultiplier)*2f)){
                Effects.effect(floor.walkEffect, floor.liquidColor, x, y);
            }

            status.handleApply(this, floor.status, floor.statusIntensity);

            if(floor.damageTaken > 0f){
                damagePeriodic(floor.damageTaken);
            }

            if(onLiquid && floor.drownTime > 0){
                drownTime += Timers.delta() * 1f/floor.drownTime;
                if(Timers.get(this, "drowneffect", 15)){
                    Effects.effect(floor.drownUpdateEffect, floor.liquidColor, x, y);
                }
            }else{
                drownTime = Mathf.lerpDelta(drownTime, 0f, 0.03f);
            }

            drownTime = Mathf.clamp(drownTime);

            if(drownTime >= 1f){
                damage(health + 1, false);
            }

            float px = x, py = y;
            move(velocity.x / getMass() * floor.speedMultiplier * Timers.delta(), velocity.y / getMass() * floor.speedMultiplier * Timers.delta());
            if(Math.abs(px - x) <= 0.0001f) velocity.x = 0f;
            if(Math.abs(py - y) <= 0.0001f) velocity.y = 0f;
        }

        velocity.scl(Mathf.clamp(1f-drag* floor.dragMultiplier* Timers.delta()));
    }

    public void applyEffect(StatusEffect effect, float intensity){
        if(dead || Net.client()) return; //effects are synced and thus not applied through clients
        status.handleApply(this, effect, intensity);
    }

    public void damagePeriodic(float amount){
        damage(amount * Timers.delta(), Timers.get(this, "damageeffect", 20));
    }

    public void damage(float amount, boolean withEffect){
        if(withEffect){
            damage(amount);
        }else{
            super.damage(amount);
        }
    }

    public void drawUnder(){}
    public void drawOver(){}

    public boolean isInfiniteAmmo(){
        return false;
    }

    public abstract int getItemCapacity();
    public abstract int getAmmoCapacity();
    public abstract float getArmor();
    public abstract boolean acceptsAmmo(Item item);
    public abstract void addAmmo(Item item);
    public abstract float getMass();
    public abstract boolean isFlying();
    public abstract float getSize();
}
