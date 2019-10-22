package io.anuke.mindustry.entities.type;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.effect.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.units.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.Teams.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public abstract class Unit extends DestructibleEntity implements SaveTrait, TargetTrait, SyncTrait, DrawTrait, TeamTrait{
    /** Total duration of hit flash effect */
    public static final float hitDuration = 9f;
    /** Percision divisor of velocity, used when writing. For example a value of '2' would mean the percision is 1/2 = 0.5-size chunks. */
    public static final float velocityPercision = 8f;
    /** Maximum absolute value of a velocity vector component. */
    public static final float maxAbsVelocity = 127f / velocityPercision;
    public static final int noSpawner = Pos.get(-1, 1);

    private static final Vector2 moveVector = new Vector2();

    public float rotation;

    protected final Interpolator interpolator = new Interpolator();
    protected final Statuses status = new Statuses();
    protected final ItemStack item = new ItemStack(content.item(0), 0);

    protected Team team = Team.sharded;
    protected float drownTime, hitTime;

    @Override
    public boolean collidesGrid(int x, int y){
        return !isFlying();
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
        if(!net.client()){
            super.damage(calculateDamage(amount));
        }
        hitTime = hitDuration;
    }

    @Override
    public boolean collides(SolidTrait other){
        if(isDead()) return false;

        if(other instanceof DamageTrait){
            return other instanceof TeamTrait && state.teams.areEnemies((((TeamTrait)other).getTeam()), team);
        }else{
            return other instanceof Unit && ((Unit)other).isFlying() == isFlying();
        }
    }

    @Override
    public void onDeath(){
        float explosiveness = 2f + item.item.explosiveness * item.amount;
        float flammability = item.item.flammability * item.amount;
        Damage.dynamicExplosion(x, y, flammability, explosiveness, 0f, getSize() / 2f, Pal.darkFlame);

        ScorchDecal.create(x, y);
        Effects.effect(Fx.explosion, this);
        Effects.shake(2f, 2f, this);

        Sounds.bang.at(this);
        item.amount = 0;
        drownTime = 0f;
        status.clear();
        Events.fire(new UnitDestroyEvent(this));

        if(explosiveness > 7f && this == player){
            Events.fire(Trigger.suicideBomb);
        }
    }

    @Override
    public Vector2 velocity(){
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
    public boolean isValid(){
        return !isDead() && isAdded();
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        writeSave(stream, false);
    }

    @Override
    public void readSave(DataInput stream, byte version) throws IOException{
        byte team = stream.readByte();
        boolean dead = stream.readBoolean();
        float x = stream.readFloat();
        float y = stream.readFloat();
        byte xv = stream.readByte();
        byte yv = stream.readByte();
        float rotation = stream.readShort() / 2f;
        int health = stream.readShort();
        byte itemID = stream.readByte();
        short itemAmount = stream.readShort();

        this.status.readSave(stream, version);
        this.item.amount = itemAmount;
        this.item.item = content.item(itemID);
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
        stream.writeByte((byte)(Mathf.clamp(velocity.x, -maxAbsVelocity, maxAbsVelocity) * velocityPercision));
        stream.writeByte((byte)(Mathf.clamp(velocity.y, -maxAbsVelocity, maxAbsVelocity) * velocityPercision));
        stream.writeShort((short)(rotation * 2));
        stream.writeShort((short)health);
        stream.writeByte(item.item.id);
        stream.writeShort((short)item.amount);
        status.writeSave(stream);
    }

    protected void clampPosition(){
        x = Mathf.clamp(x, 0, world.width() * tilesize - tilesize);
        y = Mathf.clamp(y, 0, world.height() * tilesize - tilesize);
    }

    public void kill(){
        health = -1;
        damage(1);
    }

    public boolean isImmune(StatusEffect effect){
        return false;
    }

    public boolean isOutOfBounds(){
        return x < -worldBounds || y < -worldBounds || x > world.width() * tilesize + worldBounds || y > world.height() * tilesize + worldBounds;
    }

    public float calculateDamage(float amount){
        return amount * Mathf.clamp(1f - status.getArmorMultiplier() / 100f);
    }

    public float getDamageMultipler(){
        return status.getDamageMultiplier();
    }

    public boolean hasEffect(StatusEffect effect){
        return status.hasEffect(effect);
    }

    public void avoidOthers(){
        float radScl = 1.5f;
        float fsize = getSize() / radScl;
        moveVector.setZero();
        float cx = x - fsize/2f, cy = y - fsize/2f;

        for(Team team : Team.all){
            if(team != getTeam() || !(this instanceof Player)){
                avoid(unitGroups[team.ordinal()].intersect(cx, cy, fsize, fsize));
            }
        }

        if(!(this instanceof Player)){
            avoid(playerGroup.intersect(cx, cy, fsize, fsize));
        }
        velocity.add(moveVector.x / mass() * Time.delta(), moveVector.y / mass() * Time.delta());
    }

    private void avoid(Array<? extends Unit> arr){
        float radScl = 1.5f;

        for(Unit en : arr){
            if(en.isFlying() != isFlying() || (en instanceof Player && en.getTeam() != getTeam())) continue;
            float dst = dst(en);
            float scl = Mathf.clamp(1f - dst / (getSize()/(radScl*2f) + en.getSize()/(radScl*2f)));
            moveVector.add(Tmp.v1.set((x - en.x) * scl, (y - en.y) * scl).limit(0.4f));
        }
    }

    public @Nullable TileEntity getClosestCore(){
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
        return tile == null ? (Floor)Blocks.air : tile.floor();
    }

    public void onRespawn(Tile tile){
    }

    /** Updates velocity and status effects. */
    public void updateVelocityStatus(){
        Floor floor = getFloorOn();

        Tile tile = world.tileWorld(x, y);

        status.update(this);

        velocity.limit(maxVelocity()).scl(1f + (status.getSpeedMultiplier() - 1f) * Time.delta());

        if(x < -finalWorldBounds || y < -finalWorldBounds || x >= world.width() * tilesize + finalWorldBounds || y >= world.height() * tilesize + finalWorldBounds){
            kill();
        }

        //apply knockback based on spawns
        if(getTeam() != waveTeam){
            float relativeSize = state.rules.dropZoneRadius + getSize()/2f + 1f;
            for(Tile spawn : spawner.getGroundSpawns()){
                if(withinDst(spawn.worldx(), spawn.worldy(), relativeSize)){
                    velocity.add(Tmp.v1.set(this).sub(spawn.worldx(), spawn.worldy()).setLength(0.1f + 1f - dst(spawn) / relativeSize).scl(0.45f * Time.delta()));
                }
            }
        }

        //repel player out of bounds
        final float warpDst = 180f;

        if(x < 0) velocity.x += (-x/warpDst);
        if(y < 0) velocity.y += (-y/warpDst);
        if(x > world.unitWidth()) velocity.x -= (x - world.unitWidth())/warpDst;
        if(y > world.unitHeight()) velocity.y -= (y - world.unitHeight())/warpDst;


        if(isFlying()){
            drownTime = 0f;
            move(velocity.x * Time.delta(), velocity.y * Time.delta());
        }else{
            boolean onLiquid = floor.isLiquid;

            if(tile != null){
                tile.block().unitOn(tile, this);
                if(tile.block() != Blocks.air){
                    onLiquid = false;
                }
            }

            if(onLiquid && velocity.len() > 0.4f && Mathf.chance((velocity.len() * floor.speedMultiplier) * 0.06f * Time.delta())){
                Effects.effect(floor.walkEffect, floor.color, x, y);
            }

            if(onLiquid){
                status.handleApply(this, floor.status, floor.statusDuration);

                if(floor.damageTaken > 0f){
                    damagePeriodic(floor.damageTaken);
                }
            }

            if(onLiquid && floor.drownTime > 0){
                drownTime += Time.delta() * 1f / floor.drownTime;
                if(Mathf.chance(Time.delta() * 0.05f)){
                    Effects.effect(floor.drownUpdateEffect, floor.color, x, y);
                }
            }else{
                drownTime = Mathf.lerpDelta(drownTime, 0f, 0.03f);
            }

            drownTime = Mathf.clamp(drownTime);

            if(drownTime >= 0.999f && !net.client()){
                damage(health + 1);
                if(this == player){
                    Events.fire(Trigger.drown);
                }
            }

            float px = x, py = y;
            move(velocity.x * floor.speedMultiplier * Time.delta(), velocity.y * floor.speedMultiplier * Time.delta());
            if(Math.abs(px - x) <= 0.0001f) velocity.x = 0f;
            if(Math.abs(py - y) <= 0.0001f) velocity.y = 0f;
        }

        velocity.scl(Mathf.clamp(1f - drag() * (isFlying() ? 1f : floor.dragMultiplier) * Time.delta()));
    }

    public boolean acceptsItem(Item item){
        return this.item.amount <= 0 || (this.item.item == item && this.item.amount <= getItemCapacity());
    }

    public void addItem(Item item){
        addItem(item, 1);
    }

    public void addItem(Item item, int amount){
        this.item.amount = this.item.item == item ? this.item.amount + amount : amount;
        this.item.item = item;
    }

    public void clearItem(){
        item.amount = 0;
    }

    public ItemStack item(){
        return item;
    }

    public int maxAccepted(Item item){
        return this.item.item != item && this.item.amount > 0 ? 0 : getItemCapacity() - this.item.amount;
    }

    public void applyEffect(StatusEffect effect, float duration){
        if(dead || net.client()) return; //effects are synced and thus not applied through clients
        status.handleApply(this, effect, duration);
    }

    public void damagePeriodic(float amount){
        damage(amount * Time.delta(), hitTime <= -20 + hitDuration);
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
        Draw.color(Color.black, team.color, healthf() + Mathf.absin(Time.time(), Math.max(healthf() * 5f, 1f), 1f - healthf()));
        Draw.rect(getPowerCellRegion(), x, y, rotation - 90);
        Draw.color();

        drawBackItems(item.amount > 0 ? 1f : 0f, false);
    }

    public void drawBackItems(float itemtime, boolean number){
        //draw back items
        if(itemtime > 0.01f && item.item != null){
            float backTrns = 5f;
            float size = (itemSize + Mathf.absin(Time.time(), 5f, 1f)) * itemtime;

            Draw.mixcol(Pal.accent, Mathf.absin(Time.time(), 5f, 0.5f));
            Draw.rect(item.item.icon(Cicon.medium),
                x + Angles.trnsx(rotation + 180f, backTrns),
                y + Angles.trnsy(rotation + 180f, backTrns),
                size, size, rotation);

            Draw.mixcol();

            Lines.stroke(1f, Pal.accent);
            Lines.circle(
                x + Angles.trnsx(rotation + 180f, backTrns),
                y + Angles.trnsy(rotation + 180f, backTrns),
                (3f + Mathf.absin(Time.time(), 5f, 1f)) * itemtime);

            if(number){
                Fonts.outline.draw(item.amount + "",
                    x + Angles.trnsx(rotation + 180f, backTrns),
                    y + Angles.trnsy(rotation + 180f, backTrns) - 3,
                    Pal.accent, 0.25f * itemtime / Scl.scl(1f), false, Align.center
                );
            }
        }

        Draw.reset();
    }

    public TextureRegion getPowerCellRegion(){
        return Core.atlas.find("power-cell");
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

    public float getSize(){
        hitbox(Tmp.r1);
        return Math.max(Tmp.r1.width, Tmp.r1.height) * 2f;
    }

    public abstract TextureRegion getIconRegion();

    public abstract Weapon getWeapon();

    public abstract int getItemCapacity();

    public abstract float mass();

    public abstract boolean isFlying();
}
