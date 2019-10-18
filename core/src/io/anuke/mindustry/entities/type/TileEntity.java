package io.anuke.mindustry.entities.type;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.traits.HealthTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.game.EventType.BlockDestroyEvent;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.modules.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class TileEntity extends BaseEntity implements TargetTrait, HealthTrait{
    public static final float timeToSleep = 60f * 4; //4 seconds to fall asleep
    private static final ObjectSet<Tile> tmpTiles = new ObjectSet<>();
    /** This value is only used for debugging. */
    public static int sleepingEntities = 0;

    public Tile tile;
    public Block block;
    public Interval timer;
    public float health;
    public float timeScale = 1f, timeScaleDuration;

    public PowerModule power;
    public ItemModule items;
    public LiquidModule liquids;
    public ConsumeModule cons;

    /** List of (cached) tiles with entities in proximity, used for outputting to */
    private Array<Tile> proximity = new Array<>(8);
    private boolean dead = false;
    private boolean sleeping;
    private float sleepTime;
    private @Nullable SoundLoop sound;

    @Remote(called = Loc.server, unreliable = true)
    public static void onTileDamage(Tile tile, float health){
        if(tile.entity != null){
            tile.entity.health = health;

            if(tile.entity.damaged()){
                indexer.notifyTileDamaged(tile.entity);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void onTileDestroyed(Tile tile){
        if(tile.entity == null) return;
        tile.entity.onDeath();
    }

    /** Sets this tile entity data to this tile, and adds it if necessary. */
    public TileEntity init(Tile tile, boolean shouldAdd){
        this.tile = tile;
        x = tile.drawx();
        y = tile.drawy();
        block = tile.block();
        if(block.activeSound != Sounds.none){
            sound = new SoundLoop(block.activeSound, block.activeSoundVolume);
        }

        health = block.health;
        timer = new Interval(block.timers);

        if(shouldAdd){
            add();
        }

        return this;
    }

    /** Scaled delta. */
    public float delta(){
        return Time.delta() * timeScale;
    }

    /** Call when nothing is happening to the entity. This increments the internal sleep timer. */
    public void sleep(){
        sleepTime += Time.delta();
        if(!sleeping && sleepTime >= timeToSleep){
            remove();
            sleeping = true;
            sleepingEntities++;
        }
    }

    /** Call when this entity is updating. This wakes it up. */
    public void noSleep(){
        sleepTime = 0f;
        if(sleeping){
            add();
            sleeping = false;
            sleepingEntities--;
        }
    }

    public boolean isSleeping(){
        return sleeping;
    }

    public boolean isDead(){
        return dead || tile.entity != this;
    }

    @CallSuper
    public void write(DataOutput stream) throws IOException{
        stream.writeShort((short)health);
        stream.writeByte(Pack.byteByte(tile.getTeamID(), tile.rotation())); //team + rotation
        if(items != null) items.write(stream);
        if(power != null) power.write(stream);
        if(liquids != null) liquids.write(stream);
        if(cons != null) cons.write(stream);
    }

    @CallSuper
    public void read(DataInput stream, byte revision) throws IOException{
        health = stream.readUnsignedShort();
        byte tr = stream.readByte();
        byte team = Pack.leftByte(tr);
        byte rotation = Pack.rightByte(tr);

        tile.setTeam(Team.all[team]);
        tile.rotation(rotation);

        if(items != null) items.read(stream);
        if(power != null) power.read(stream);
        if(liquids != null) liquids.read(stream);
        if(cons != null) cons.read(stream);
    }

    /** Returns the version of this TileEntity IO code.*/
    public byte version(){
        return 0;
    }

    public boolean collide(Bullet other){
        return true;
    }

    public void collision(Bullet other){
        block.handleBulletHit(this, other);
    }

    public void kill(){
        Call.onTileDestroyed(tile);
    }

    public void damage(float damage){
        if(dead) return;

        float preHealth = health;

        Call.onTileDamage(tile, health - block.handleDamage(tile, damage));

        if(health <= 0){
            Call.onTileDestroyed(tile);
        }

        if(preHealth >= maxHealth() - 0.00001f && health < maxHealth() && world != null){ //when just damaged
            indexer.notifyTileDamaged(this);
        }
    }

    public Tile getTile(){
        return tile;
    }

    public void removeFromProximity(){
        block.onProximityRemoved(tile);

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tile other = world.ltile(tile.x + point.x, tile.y + point.y);
            //remove this tile from all nearby tile's proximities
            if(other != null){
                other.block().onProximityUpdate(other);

                if(other.entity != null){
                    other.entity.proximity.removeValue(tile, true);
                }
            }
        }
    }

    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tile other = world.ltile(tile.x + point.x, tile.y + point.y);

            if(other == null) continue;
            if(other.entity == null || !(other.interactable(tile.getTeam()))) continue;

            //add this tile to proximity of nearby tiles
            if(!other.entity.proximity.contains(tile, true)){
                other.entity.proximity.add(tile);
            }

            tmpTiles.add(other);
        }

        //using a set to prevent duplicates
        for(Tile tile : tmpTiles){
            proximity.add(tile);
        }

        block.onProximityAdded(tile);
        block.onProximityUpdate(tile);

        for(Tile other : tmpTiles){
            other.block().onProximityUpdate(other);
        }
    }

    public Array<Tile> proximity(){
        return proximity;
    }

    /** Tile configuration. Defaults to 0. Used for block rebuilding. */
    public int config(){
        return 0;
    }

    @Override
    public void removed(){
        if(sound != null){
            sound.stop();
        }
    }

    @Override
    public void health(float health){
        this.health = health;
    }

    @Override
    public float health(){
        return health;
    }

    @Override
    public float maxHealth(){
        return block.health;
    }

    @Override
    public void setDead(boolean dead){
        this.dead = dead;
    }

    @Override
    public void onDeath(){
        if(!dead){
            dead = true;

            Events.fire(new BlockDestroyEvent(tile));
            block.breakSound.at(tile);
            block.onDestroyed(tile);
            world.removeBlock(tile);
            remove();
        }
    }

    @Override
    public Team getTeam(){
        return tile.getTeam();
    }

    @Override
    public Vector2 velocity(){
        return Vector2.ZERO;
    }

    @Override
    public void update(){
        timeScaleDuration -= Time.delta();
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(health <= 0){
            onDeath();
            return; //no need to update anymore
        }

        if(sound != null){
            sound.update(x, y, block.shouldActiveSound(tile));
        }

        if(block.idleSound != Sounds.none && block.shouldIdleSound(tile)){
            loops.play(block.idleSound, this, block.idleSoundVolume);
        }

        Block previous = block;
        block.update(tile);
        if(block == previous && cons != null){
            cons.update();
        }

        if(block == previous && power != null){
            power.graph.update();
        }
    }

    @Override
    public boolean isValid(){
        return !isDead() && tile.entity == this;
    }

    @Override
    public EntityGroup targetGroup(){
        return tileGroup;
    }

    @Override
    public String toString(){
        return "TileEntity{" +
        "tile=" + tile +
        ", health=" + health +
        '}';
    }
}
