package mindustry.entities.def;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

@Component
abstract class TileComp implements Posc, Teamc, Healthc, Tilec, Timerc{
    static final float timeToSleep = 60f * 1;
    static final ObjectSet<Tile> tmpTiles = new ObjectSet<>();
    static int sleepingEntities = 0;

    Tile tile;
    Block block;
    Array<Tile> proximity = new Array<>(8);

    PowerModule power;
    ItemModule items;
    LiquidModule liquids;
    ConsumeModule cons;

    private float timeScale = 1f, timeScaleDuration;

    private @Nullable SoundLoop sound;

    private boolean sleeping;
    private float sleepTime;

    /** Sets this tile entity data to this tile, and adds it if necessary. */
    @Override
    public Tilec init(Tile tile, boolean shouldAdd){
        this.tile = tile;
        this.block = tile.block();

        set(tile.drawx(), tile.drawy());
        if(block.activeSound != Sounds.none){
            sound = new SoundLoop(block.activeSound, block.activeSoundVolume);
        }

        health(block.health);
        maxHealth(block.health);
        timer(new Interval(block.timers));

        if(shouldAdd){
            add();
        }

        return this;
    }

    @Override
    public void applyBoost(float intensity, float  duration){
        timeScale = Math.max(timeScale, intensity);
        timeScaleDuration = Math.max(timeScaleDuration, duration);
    }

    @Override
    public float timeScale(){
        return timeScale;
    }

    @Override
    public boolean consValid(){
        return cons.valid();
    }

    @Override
    public void consume(){
        cons.trigger();
    }

    /** Scaled delta. */
    @Override
    public float delta(){
        return Time.delta() * timeScale;
    }

    /** Base efficiency. If this entity has non-buffered power, returns the power %, otherwise returns 1. */
    @Override
    public float efficiency(){
        return power != null && (block.consumes.has(ConsumeType.power) && !block.consumes.getPower().buffered) ? power.status : 1f;
    }

    /** Call when nothing is happening to the entity. This increments the internal sleep timer. */
    @Override
    public void sleep(){
        sleepTime += Time.delta();
        if(!sleeping && sleepTime >= timeToSleep){
            remove();
            sleeping = true;
            sleepingEntities++;
        }
    }

    /** Call when this entity is updating. This wakes it up. */
    @Override
    public void noSleep(){
        sleepTime = 0f;
        if(sleeping){
            add();
            sleeping = false;
            sleepingEntities--;
        }
    }

    /** Returns the version of this TileEntity IO code.*/
    @Override
    public byte version(){
        return 0;
    }

    @Override
    public boolean collide(Bulletc other){
        return true;
    }

    @Override
    public void collision(Bulletc other){
        block.handleBulletHit(this, other);
    }

    //TODO Implement damage!

    @Override
    public void removeFromProximity(){
        block.onProximityRemoved(tile);

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tile other = world.ltile(tile.x + point.x, tile.y + point.y);
            //remove this tile from all nearby tile's proximities
            if(other != null){
                other.block().onProximityUpdate(other);

                if(other.entity != null){
                    other.entity.proximity().remove(tile, true);
                }
            }
        }
    }

    @Override
    public void updateProximity(){
        tmpTiles.clear();
        proximity.clear();

        Point2[] nearby = Edges.getEdges(block.size);
        for(Point2 point : nearby){
            Tile other = world.ltile(tile.x + point.x, tile.y + point.y);

            if(other == null) continue;
            if(other.entity == null || !(other.interactable(tile.team()))) continue;

            //add this tile to proximity of nearby tiles
            if(!other.entity.proximity().contains(tile, true)){
                other.entity.proximity().add(tile);
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

    @Override
    public Array<Tile> proximity(){
        return proximity;
    }

    /** Tile configuration. Defaults to 0. Used for block rebuilding. */
    @Override
    public int config(){
        return 0;
    }

    @Override
    public void remove(){
        if(sound != null){
            sound.stop();
        }
    }

    @Override
    public void killed(){
        Events.fire(new BlockDestroyEvent(tile));
        block.breakSound.at(tile);
        block.onDestroyed(tile);
        tile.remove();
    }

    @Override
    public void update(){
        timeScaleDuration -= Time.delta();
        if(timeScaleDuration <= 0f || !block.canOverdrive){
            timeScale = 1f;
        }

        if(sound != null){
            sound.update(x(), y(), block.shouldActiveSound(tile));
        }

        if(block.idleSound != Sounds.none && block.shouldIdleSound(tile)){
            loops.play(block.idleSound, this, block.idleSoundVolume);
        }

        block.update(tile);

        if(liquids != null){
            liquids.update();
        }

        if(cons != null){
            cons.update();
        }

        if(power != null){
            power.graph.update();
        }
    }
}
