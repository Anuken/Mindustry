package io.anuke.mindustry.entities.effect;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.entities.type.TimedEntity;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.world.*;

import java.io.*;

import static io.anuke.mindustry.Vars.*;

public class Fire extends TimedEntity implements SaveTrait, SyncTrait{
    private static final IntMap<Fire> map = new IntMap<>();
    private static final float baseLifetime = 1000f, spreadChance = 0.05f, fireballChance = 0.07f;

    private int loadedPosition = -1;
    private Tile tile;
    private Block block;
    private float baseFlammability = -1, puddleFlammability;
    private float lifetime;

    /** Deserialization use only! */
    public Fire(){
    }

    @Remote
    public static void onRemoveFire(int fid){
        fireGroup.removeByID(fid);
    }

    /** Start a fire on the tile. If there already is a file there, refreshes its lifetime. */
    public static void create(Tile tile){
        if(net.client() || tile == null) return; //not clientside.

        Fire fire = map.get(tile.pos());

        if(fire == null){
            fire = new Fire();
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
        if(!Structs.inBounds(x, y, world.width(), world.height()) || !map.containsKey(Pos.get(x, y))){
            return false;
        }
        Fire fire = map.get(Pos.get(x, y));
        return fire.isAdded() && fire.fin() < 1f && fire.tile != null && fire.tile.x == x && fire.tile.y == y;
    }

    /**
     * Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.
     */
    public static void extinguish(Tile tile, float intensity){
        if(tile != null && map.containsKey(tile.pos())){
            Fire fire = map.get(tile.pos());
            fire.time += intensity * Time.delta();
            if(fire.time >= fire.lifetime()){
                Events.fire(Trigger.fireExtinguish);
            }
        }
    }

    @Override
    public TypeID getTypeID(){
        return TypeIDs.fire;
    }

    @Override
    public byte version(){
        return 0;
    }

    @Override
    public float lifetime(){
        return lifetime;
    }

    @Override
    public void update(){
        if(Mathf.chance(0.1 * Time.delta())){
            Effects.effect(Fx.fire, x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Mathf.chance(0.05 * Time.delta())){
            Effects.effect(Fx.fireSmoke, x + Mathf.range(4f), y + Mathf.range(4f));
        }

        if(Mathf.chance(0.001 * Time.delta())){
            Sounds.fire.at(this);
        }

        time = Mathf.clamp(time + Time.delta(), 0, lifetime());
        map.put(tile.pos(), this);

        if(net.client()){
            return;
        }

        if(time >= lifetime() || tile == null){
            remove();
            return;
        }

        TileEntity entity = tile.link().entity;
        boolean damage = entity != null;

        float flammability = baseFlammability + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Time.delta() * 8;
        }

        if(baseFlammability < 0 || block != tile.block()){
            baseFlammability = tile.block().getFlammability(tile);
            block = tile.block();
        }

        if(damage){
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Time.delta();
        }

        if(flammability > 1f && Mathf.chance(spreadChance * Time.delta() * Mathf.clamp(flammability / 5f, 0.3f, 2f))){
            Point2 p = Geometry.d4[Mathf.random(3)];
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            create(other);

            if(Mathf.chance(fireballChance * Time.delta() * Mathf.clamp(flammability / 10f))){
                Call.createBullet(Bullets.fireball, x, y, Mathf.random(360f));
            }
        }

        if(Mathf.chance(0.1 * Time.delta())){
            Puddle p = Puddle.getPuddle(tile);
            if(p != null){
                puddleFlammability = p.getFlammability() / 3f;
            }else{
                puddleFlammability = 0;
            }

            if(damage){
                entity.damage(0.4f);
            }
            Damage.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, 3f,
            unit -> !unit.isFlying() && !unit.isImmune(StatusEffects.burning),
            unit -> unit.applyEffect(StatusEffects.burning, 60 * 5));
        }
    }

    @Override
    public void writeSave(DataOutput stream) throws IOException{
        stream.writeInt(tile.pos());
        stream.writeFloat(lifetime);
        stream.writeFloat(time);
    }

    @Override
    public void readSave(DataInput stream, byte version) throws IOException{
        this.loadedPosition = stream.readInt();
        this.lifetime = stream.readFloat();
        this.time = stream.readFloat();
        add();
    }

    @Override
    public void write(DataOutput data) throws IOException{
        data.writeInt(tile.pos());
        data.writeFloat(lifetime);
    }

    @Override
    public void read(DataInput data) throws IOException{
        int pos = data.readInt();
        this.lifetime = data.readFloat();

        x = Pos.x(pos) * tilesize;
        y = Pos.y(pos) * tilesize;
        tile = world.tile(pos);
    }

    @Override
    public void reset(){
        loadedPosition = -1;
        tile = null;
        baseFlammability = -1;
        puddleFlammability = 0f;
        incrementID();
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
            Call.onRemoveFire(id);
            map.remove(tile.pos());
        }
    }

    @Override
    public EntityGroup targetGroup(){
        return fireGroup;
    }
}
