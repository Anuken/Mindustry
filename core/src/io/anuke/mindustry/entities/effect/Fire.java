package io.anuke.mindustry.entities.effect;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.Pools;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.content.fx.EnvironmentFx;
import io.anuke.mindustry.entities.SerializableEntity;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.entities.TimedEntity;
import io.anuke.ucore.util.Geometry;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Fire extends TimedEntity implements SerializableEntity, Poolable{
    private static final IntMap<Fire> map = new IntMap<>();
    private static final float baseLifetime = 1000f;

    private int loadedPosition = -1;
    private Tile tile;
    private Block block;
    private float baseFlammability = -1, puddleFlammability;

    /**Start a fire on the tile. If there already is a file there, refreshes its lifetime..*/
    public static void create(Tile tile){
        Fire fire = map.get(tile.packedPosition());

        if(fire == null){
            fire = Pools.obtain(Fire.class);
            fire.tile = tile;
            fire.lifetime = baseLifetime;
            map.put(tile.packedPosition(), fire.add());
        }else{
            fire.lifetime = baseLifetime;
            fire.time = 0f;
        }
    }

    /**Attempts to extinguish a fire by shortening its life. If there is no fire here, does nothing.*/
    public static void extinguish(Tile tile, float intensity){
        if(map.containsKey(tile.packedPosition())){
            map.get(tile.packedPosition()).time += intensity * Timers.delta();
        }
    }

    /**Deserialization use only!*/
    private Fire(){}

    @Override
    public void update() {
        super.update();

        TileEntity entity = tile.target().entity;
        boolean damage = entity != null;

        float flammability = baseFlammability + puddleFlammability;

        if(!damage && flammability <= 0){
            time += Timers.delta()*8;
        }

        if (baseFlammability < 0 || block != tile.block()){
            baseFlammability = tile.block().getFlammability(tile);
            block = tile.block();
        }

        if(damage) {
            lifetime += Mathf.clamp(flammability / 8f, 0f, 0.6f) * Timers.delta();
        }

        if (flammability > 1f && Mathf.chance(0.03 * Timers.delta() * Mathf.clamp(flammability/5f, 0.3f, 2f))) {
            GridPoint2 p = Mathf.select(Geometry.d4);
            Tile other = world.tile(tile.x + p.x, tile.y + p.y);
            create(other);
        }

        if(Mathf.chance(0.1 * Timers.delta())){
            Effects.effect(EnvironmentFx.fire, tile.worldx() + Mathf.range(4f), tile.worldy() + Mathf.range(4f));

            Puddle p = Puddle.getPuddle(tile);
            if(p != null){
                puddleFlammability = p.getFlammability()/3f;
            }else{
                puddleFlammability = 0;
            }

            if(damage){
                entity.damage(0.4f);
            }
            DamageArea.damageUnits(null, tile.worldx(), tile.worldy(), tilesize, 3f, unit -> unit.applyEffect(StatusEffects.burning, 0.8f));
        }

        if(Mathf.chance(0.05 * Timers.delta())){
            Effects.effect(EnvironmentFx.smoke, tile.worldx() + Mathf.range(4f), tile.worldy() + Mathf.range(4f));
        }
    }

    @Override
    public void writeSave(DataOutputStream stream) throws IOException {
        stream.writeInt(tile.packedPosition());
        stream.writeFloat(lifetime);
        stream.writeFloat(time);
    }

    @Override
    public void readSave(DataInputStream stream) throws IOException {
        this.loadedPosition = stream.readInt();
        this.lifetime = stream.readFloat();
        this.time = stream.readFloat();
        add();
    }

    @Override
    public void reset() {
        loadedPosition = -1;
        tile = null;
        baseFlammability = -1;
        puddleFlammability = 0f;
    }

    @Override
    public void added() {
        if(loadedPosition != -1){
            map.put(loadedPosition, this);
            tile = world.tile(loadedPosition);
            set(tile.worldx(), tile.worldy());
        }
    }

    @Override
    public void removed() {
        map.remove(tile.packedPosition());
        reset();
    }

    @Override
    public Fire add(){
        return add(airItemGroup);
    }
}
