package io.anuke.mindustry.entities.type;

import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Interval;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.EntityGroup;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.impl.BaseEntity;
import io.anuke.mindustry.entities.traits.HealthTrait;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.EventType.BlockDestroyEvent;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.modules.*;

import java.io.*;

import static io.anuke.mindustry.Vars.tileGroup;
import static io.anuke.mindustry.Vars.world;

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

    @Remote(called = Loc.server, unreliable = true)
    public static void onTileDamage(Tile tile, float health){
        if(tile.entity != null){
            tile.entity.health = health;

            if(tile.entity.damaged()){
                world.indexer.notifyTileDamaged(tile.entity);
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

    public void write(DataOutput stream) throws IOException{
    }

    public void writeConfig(DataOutput stream) throws IOException{
    }

    public void read(DataInput stream) throws IOException{
    }

    public void readConfig(DataInput stream) throws IOException{
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
            world.indexer.notifyTileDamaged(this);
        }
    }

    public boolean damaged(){
        return health < maxHealth() - 0.00001f;
    }

    public Tile getTile(){
        return tile;
    }

    public void removeFromProximity(){
        block.onProximityRemoved(tile);

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tile other = world.tile(tile.x + point.x, tile.y + point.y);
            //remove this tile from all nearby tile's proximities
            if(other != null){
                other = other.target();
                other.block().onProximityUpdate(other);
            }
            if(other != null && other.entity != null){
                other.entity.proximity.removeValue(tile, true);
            }
        }
    }

    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tile other = world.tile(tile.x + point.x, tile.y + point.y);

            if(other == null) continue;
            other = other.target();
            if(other.entity == null || !(other.interactable(tile.getTeam()))) continue;

            other.block().onProximityUpdate(other);

            tmpTiles.add(other);

            //add this tile to proximity of nearby tiles
            if(!other.entity.proximity.contains(tile, true)){
                other.entity.proximity.add(tile);
            }
        }

        //using a set to prevent duplicates
        for(Tile tile : tmpTiles){
            proximity.add(tile);
        }

        block.onProximityAdded(tile);
        block.onProximityUpdate(tile);
    }

    public Array<Tile> proximity(){
        return proximity;
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
            block.onDestroyed(tile);
            world.removeBlock(tile);
            block.afterDestroyed(tile, this);
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
        //TODO better smoke effect, this one is awful
        /*if(health != 0 && health < block.health && !(block instanceof Wall) &&
        Mathf.chance(0.009f * Time.delta() * (1f - health / block.health))){
            Effects.effect(Fx.smoke, x + Mathf.range(4), y + Mathf.range(4));
        }*/

        timeScaleDuration -= Time.delta();
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(health <= 0){
            onDeath();
            return; //no need to update anymore
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
