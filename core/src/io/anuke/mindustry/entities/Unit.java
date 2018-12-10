package io.anuke.mindustry.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.Teams.TeamData;
import io.anuke.mindustry.net.Interpolator;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.type.StatusEffect;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.world.Pos;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.impl.DestructibleEntity;
import io.anuke.ucore.entities.trait.DamageTrait;
import io.anuke.ucore.entities.trait.DrawTrait;
import io.anuke.ucore.entities.trait.SolidTrait;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public abstract class Unit extends DestructibleEntity implements SaveTrait, TargetTrait, SyncTrait, DrawTrait, TeamTrait, CarriableTrait, InventoryTrait{
    /**Total duration of hit flash effect*/
    public static final float hitDuration = 9f;
    /**Percision divisor of velocity, used when writing. For example a value of '2' would mean the percision is 1/2 = 0.5-size chunks.*/
    public static final float velocityPercision = 8f;
    /**Maximum absolute value of a velocity vector component.*/
    public static final float maxAbsVelocity = 127f / velocityPercision;
    public static final int noSpawner = Pos.get(-1, 1);

    private static final Rectangle queryRect = new Rectangle();
    private static final Vector2 moveVector = new Vector2();

    public final UnitInventory inventory = new UnitInventory(this);
    public float rotation;
    public float hitTime;

    protected final Interpolator interpolator = new Interpolator();
    protected final StatusController status = new StatusController();
    protected Team team = Team.blue;

    protected CarryTrait carrier;
    protected float drownTime;

    @Override
    public boolean movable(){
        return !isDead();
    }

    @Override
    public UnitInventory getInventory(){
        return inventory;
    }

    @Override
    public CarryTrait getCarrier(){
        return carrier;
    }

    @Override
    public void setCarrier(CarryTrait carrier){
        this.carrier = carrier;
    }

    @Override
    public Team getTeam(){
        return team;
    }

    @Override
    public void interpolate(){
        interpolator.update();

        x = interpolator.pos.x;
        y = interpolator.pos.y;

        if(interpolator.values.length > 0){
            rotation = interpolator.values[0];
        }
    }

    @Override
    public Interpolator getInterpolator(){
        return interpolator;
    }

    @Override
    public void damage(float amount){
        super.damage(calculateDamage(amount));
        hitTime = hitDuration;
    }

    @Override
    public boolean collides(SolidTrait other){
        if(isDead()) return false;

        if(other instanceof DamageTrait){
            return other instanceof TeamTrait && state.teams.areEnemies((((TeamTrait) other).getTeam()), team);
        }else{
            return other instanceof Unit && ((Unit) other).isFlying() == isFlying();
        }
    }

    @Override
    public void onDeath(){
        inventory.clear();
        drownTime = 0f;
        status.clear();
    }

    @Override
    public Vector2 getVelocity(){
        return velocity;
    }

    @Override
    public void move(float x, float y){
        if(!isFlying()){
            super.move(x, y);
        }else{
            moveBy(x, y);
        }
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        writeSave(stream, false);
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        byte team = stream.readByte();
        boolean dead = stream.readBoolean();
        float x = stream.readFloat();
        float y = stream.readFloat();
        byte xv = stream.readByte();
        byte yv = stream.readByte();
        float rotation = stream.readShort() / 2f;
        int health = stream.readShort();

        this.status.readSave(stream);
        this.inventory.readSave(stream);
        this.dead = dead;
        this.team = Team.all[team];
        this.health = health;
        this.x = x;
        this.y = y;
        this.velocity.set(xv / velocityPercision, yv / velocityPercision);
        this.rotation = rotation;
    }

    public void writeSave(DataOutput stream, boolean net) throws IOException{
        stream.writeByte(team.ordinal());
        stream.writeBoolean(isDead());
        stream.writeFloat(net ? interpolator.target.x : x);
        stream.writeFloat(net ? interpolator.target.y : y);
        stream.writeByte((byte) (Mathf.clamp(velocity.x, -maxAbsVelocity, maxAbsVelocity) * velocityPercision));
        stream.writeByte((byte) (Mathf.clamp(velocity.y, -maxAbsVelocity, maxAbsVelocity) * velocityPercision));
        stream.writeShort((short) (rotation * 2));
        stream.writeShort((short) health);
        status.writeSave(stream);
        inventory.writeSave(stream);
    }

    public float calculateDamage(float amount){
        return amount * Mathf.clamp(1f - getArmor() / 100f * status.getArmorMultiplier());
    }

    public float getDamageMultipler(){
        return status.getDamageMultiplier();
    }

    public boolean hasEffect(StatusEffect effect){
        return status.hasEffect(effect);
    }

    public void avoidOthers(float scaling){
        getHitbox(queryRect);
        queryRect.setSize(queryRect.getWidth() * scaling);

        Units.getNearby(queryRect, t -> {
            if(t == this || t.getCarrier() == this || getCarrier() == t || t.isFlying() != isFlying()) return;
            float dst = distanceTo(t);
            moveVector.set(x, y).sub(t.getX(), t.getY()).setLength(1f * (1f - (dst / queryRect.getWidth())));
            applyImpulse(moveVector.x, moveVector.y);
        });
    }

    public TileEntity getClosestCore(){
        TeamData data = state.teams.get(team);

        Tile tile = Geometry.findClosest(x, y, data.cores);
        if(tile == null){
            return null;
        }else{
            return tile.entity;
        }
    }

    public Floor getFloorOn(){
        Tile tile = world.tileWorld(x, y);
        return tile == null ? (Floor) Blocks.air : tile.floor();
    }

    @Override
    public boolean isValid(){
        return !isDead() && isAdded();
    }

    /**Updates velocity and status effects.*/
    public void updateVelocityStatus(){
        Floor floor = getFloorOn();

        if(isCarried()){ //carried units do not take into account velocity normally
            set(carrier.getX(), carrier.getY());
            velocity.set(carrier.getVelocity());
            return;
        }

        Tile tile = world.tileWorld(x, y);

        status.update(this);

        velocity.limit(getMaxVelocity()).scl(1f + (status.getSpeedMultiplier()-1f) * Timers.delta());

        if(isFlying()){
            x += velocity.x * Timers.delta();
            y += velocity.y * Timers.delta();
        }else{
            boolean onLiquid = floor.isLiquid;

            if(tile != null){
                tile.block().unitOn(tile, this);
                if(tile.block() != Blocks.air){
                    onLiquid = false;
                }

                //on slope
                if(tile.getElevation() == -1){
                    velocity.scl(0.7f);
                }
            }

            if(onLiquid && velocity.len() > 0.4f && Mathf.chance((velocity.len() * floor.speedMultiplier) * 0.06f * Timers.delta())){
                Effects.effect(floor.walkEffect, floor.liquidColor, x, y);
            }

            if(onLiquid){
                status.handleApply(this, floor.status, floor.statusIntensity);

                if(floor.damageTaken > 0f){
                    damagePeriodic(floor.damageTaken);
                }
            }

            if(onLiquid && floor.drownTime > 0){
                drownTime += Timers.delta() * 1f / floor.drownTime;
                if(Mathf.chance(Timers.delta() * 0.05f)){
                    Effects.effect(floor.drownUpdateEffect, floor.liquidColor, x, y);
                }
            }else{
                drownTime = Mathf.lerpDelta(drownTime, 0f, 0.03f);
            }

            drownTime = Mathf.clamp(drownTime);

            if(drownTime >= 0.999f && !Net.client()){
                damage(health + 1);
            }

            float px = x, py = y;
            move(velocity.x * floor.speedMultiplier * Timers.delta(), velocity.y * floor.speedMultiplier * Timers.delta());
            if(Math.abs(px - x) <= 0.0001f) velocity.x = 0f;
            if(Math.abs(py - y) <= 0.0001f) velocity.y = 0f;
        }

        velocity.scl(Mathf.clamp(1f - getDrag() * (isFlying() ? 1f : floor.dragMultiplier) * Timers.delta()));
    }

    public void applyEffect(StatusEffect effect, float intensity){
        if(dead || Net.client()) return; //effects are synced and thus not applied through clients
        status.handleApply(this, effect, intensity);
    }

    public void damagePeriodic(float amount){
        damage(amount * Timers.delta(), hitTime <= -20 + hitDuration);
    }

    public void damage(float amount, boolean withEffect){
        float pre = hitTime;

        damage(amount);

        if(!withEffect){
            hitTime = pre;
        }
    }

    public void drawUnder(){
    }

    public void drawOver(){
    }

    public void drawStats(){
        Draw.color(Color.BLACK, team.color, healthf() + Mathf.absin(Timers.time(), healthf()*5f, 1f - healthf()));
        Draw.alpha(hitTime);
        Draw.rect(getPowerCellRegion(), x, y, rotation - 90);
        Draw.color();
    }

    public TextureRegion getPowerCellRegion(){
        return Draw.region("power-cell");
    }

    public void drawAll(){
        if(!isDead()){
            draw();
            drawStats();
        }
    }

    public void drawShadow(float offsetX, float offsetY){
        Draw.rect(getIconRegion(), x + offsetX, y + offsetY, rotation - 90);
    }

    public void drawView(){
        Fill.circle(x, y, getViewDistance());
    }

    public float getViewDistance(){
        return 135f;
    }

    public abstract TextureRegion getIconRegion();

    public abstract Weapon getWeapon();

    public abstract int getItemCapacity();

    public abstract float getArmor();

    public abstract float getMass();

    public abstract boolean isFlying();

    public abstract float getSize();
}
