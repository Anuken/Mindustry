package io.anuke.mindustry.entities;

import com.badlogic.gdx.math.Vector2;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.SolidEntity;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.mindustry.Vars.world;

public abstract class Unit extends SyncEntity implements SerializableEntity {
    //total duration of hit effect
    public static final float hitDuration = 9f;

    public StatusController status = new StatusController();
    public UnitInventory inventory = new UnitInventory(100);
    public Team team = Team.blue;
    public Vector2 velocity = new Vector2();
    public float hitTime;
    public float drownTime;

    @Override
    public void damage(float amount){
        super.damage(amount);
        hitTime = hitDuration;
    }

    @Override
    public boolean collides(SolidEntity other){
        return other instanceof Bullet && state.teams.areEnemies((((Bullet) other).team), team);
    }

    @Override
    public void onDeath() {
        inventory.clear();
        drownTime = 0f;
        status.clear();
    }

    @Override
    public void writeSave(DataOutputStream stream) throws IOException {
        stream.writeByte(team.ordinal());
        stream.writeFloat(x);
        stream.writeFloat(y);
        stream.writeShort((short)health);
        stream.writeByte(status.current().id);
        stream.writeFloat(status.getTime());
    }

    @Override
    public void readSave(DataInputStream stream) throws IOException {
        byte team = stream.readByte();
        float x = stream.readFloat();
        float y = stream.readFloat();
        int health = stream.readShort();
        byte effect = stream.readByte();
        float etime = stream.readFloat();

        this.team = Team.values()[team];
        this.health = health;
        this.x = x;
        this.y = y;
        this.status.set(StatusEffect.getByID(effect), etime);
    }

    public Floor getFloorOn(){
        Tile tile = world.tileWorld(x, y);
        return (Floor)(tile == null || (!(tile.floor() instanceof Floor)) ? Blocks.defaultFloor : tile.floor());
    }

    public void updateVelocityStatus(float drag, float maxVelocity){
        Floor floor = getFloorOn();
        Tile tile = world.tileWorld(x, y);

        velocity.limit(maxVelocity);

        status.update(this);

        if(isFlying()) {
            x += velocity.x / getMass() * Timers.delta();
            y += velocity.y / getMass() * Timers.delta();
        }else{
            boolean onLiquid = floor.liquid;

            if(tile != null){
                tile.block().unitOn(tile, this);
                if(tile.block() != Blocks.air){
                    onLiquid = false;
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

            move(velocity.x / getMass() * floor.speedMultiplier * Timers.delta(), velocity.y / getMass() * floor.speedMultiplier * Timers.delta());
        }

        velocity.scl(Mathf.clamp(1f-drag* floor.dragMultiplier* Timers.delta()));
    }

    public void applyEffect(StatusEffect effect, float intensity){
        if(dead) return;
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

    public abstract float getMass();
    public abstract boolean isFlying();
    public abstract float getSize();
}
