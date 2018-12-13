package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pool.Poolable;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.bullets.TurretBullets;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.Damage;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.traits.SaveTrait;
import io.anuke.mindustry.entities.traits.SyncTrait;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.impl.TimedEntity;
import io.anuke.ucore.util.Structs;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Pooling;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Fire extends TimedEntity implements SaveTrait, SyncTrait, Poolable{
    private static final IntMap<Fire> map = new IntMap<>();
    private static final float baseLifetime = 1000f, spreadChance = 0.05f, fireballChance = 0.07f;

    private int loadedPosition = -1;
    private Tile tile;
    private Block block;
    private float baseFlammability = -1, puddleFlammability;
    private float lifetime;

    /**Deserialization use only!*/
    public Fire(){}

    /**Start a fire on the tile. If there already is a file there, refreshes its lifetime.*/
    public static void create(Tile tile){
        if(Net.client() || tile == null) return; //not clientside.

        Fire fire = map.get(tile.pos());

        if(fire == null){
            fire = Pooling.obtain(Fire.class, Fire::new);
            fire.tile = tile;
            fire.lifetime = baseLifetime;
            fire.set(tile.worldx(), tile.worldy());
            fire.add();
            map.put(tile.pos(), fire);
        }else{
            fire.lifetime = baseLifetime;
            fire.time = 0f;
        }
    }

    public static boolean has(int x, int y){
        if(!Structs.inBounds(x, y, world.width(), world.height()) || !map.containsKey(x + y * world.width())){
            return false;
        }
        Fire fire = map.get(x + y * world.width());
        return fire.isAdded() && fire.fin() < 1f && fire.tile != null && fire.tile.x == x && fire.tile.y == y;
    }

    /**
     * Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.
     */
    public static void extinguish(Tile tile, float intensity){
        if(tile != null && map.containsKey(tile.pos())){
            map.get(tile.pos()).time += intensity * Timers.delta();
        }
    }

    @Remote(called = Loc.server)
    public static void onFireRemoved(int fireid){
        fireGroup.removeByID(fireid);
    }

    @Override
    public float lifetime(){
        return lifetime;
    }

    @Override
    public void update(){
        if(Mathf.chance(0.1 * Timers.delta())){
            Effects.effect(EnvironmentFx.fire, x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Mathf.chance(0.05 * Timers.delta())){
            Effects.effect(EnvironmentFx.smoke, x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Net.client()){
            return;
        }

        time = Mathf.clamp(time + Timers.delta(), 0, lifetime());

        if(time >= lifetime() || tile == null){
            Call.onFireRemoved(getID());
            remove();
            return;
        }

        TileEntity entity = tile.target().entity;
        boolean damage = entity != null;

        float flammability = baseFlammability + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Timers.delta() * 8;
        }

        if(baseFlammability < 0 || block != tile.block()){
            baseFlammability = tile.block().getFlammability(tile);
            block = tile.block();
        }

        if(damage){
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Timers.delta();
        }

        if(flammability > 1f && Mathf.chance(spreadChance * Timers.delta() * Mathf.clamp(flammability / 5f, 0.3f, 2f))){
            GridPoint2 p = Mathf.select(Geometry.d4);
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            create(other);

            if(Mathf.chance(fireballChance * Timers.delta() * Mathf.clamp(flammability / 10.0))){
                Call.createBullet(TurretBullets.fireball, x, y, Mathf.random(360f));
            }
        }

        if(Mathf.chance(0.1 * Timers.delta())){
            Puddle p = Puddle.getPuddle(tile);
            if(p != null){
                puddleFlammability = p.getFlammability() / 3f;
            }else{
                puddleFlammability = 0;
            }

            if(damage){
                entity.damage(0.4f);
            }
            Damage.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, 3f, unit -> !unit.isFlying(), unit -> unit.applyEffect(StatusEffects.burning, 0.8f));
        }
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeInt(tile.pos());
        stream.writeFloat(lifetime);
        stream.writeFloat(time);
    }

    @Override
    public void readSave(DataInput stream) throws IOException{
        this.loadedPosition = stream.readInt();
        this.lifetime = stream.readFloat();
        this.time = stream.readFloat();
        add();
    }

    @Override
    public void write(DataOutput data) throws IOException{
        data.writeFloat(x);
        data.writeFloat(y);
    }

    @Override
    public void read(DataInput data, long time) throws IOException{
        x = data.readFloat();
        y = data.readFloat();
    }

    @Override
    public void reset(){
        loadedPosition = -1;
        tile = null;
        baseFlammability = -1;
        puddleFlammability = 0f;
    }

    @Override
    public void added(){
        if(loadedPosition != -1){
            map.put(loadedPosition, this);
            tile = world.tile(loadedPosition);
            set(tile.worldx(), tile.worldy());
        }
    }

    @Override
    public void removed(){
        if(tile != null){
            map.remove(tile.pos());
        }
        reset();
    }

    @Override
    public EntityGroup targetGroup(){
        return fireGroup;
    }
}
