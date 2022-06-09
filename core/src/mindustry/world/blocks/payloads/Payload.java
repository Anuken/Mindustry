package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public interface Payload extends Position{
    int payloadUnit = 0, payloadBlock = 1;

    /** sets this payload's position on the map. */
    void set(float x, float y, float rotation);

    /** draws this payload at a position. */
    void draw();

    void drawShadow(float alpha);

    /** @return hitbox size of the payload. */
    float size();

    float x();

    float y();

    /** @return the items needed to make this payload; may be empty. */
    ItemStack[] requirements();

    /** @return the time taken to build this payload. */
    float buildTime();

    /** update this payload inside a container unit or building. either can be null. */
    default void update(@Nullable Unit unitHolder, @Nullable Building buildingHolder){}

    /** @return whether this payload was dumped. */
    default boolean dump(){
        return false;
    }

    /** @return whether this payload fits in a given size. 3 is the max for a standard 3x3 conveyor. */
    default boolean fits(float s){
        return size() / tilesize <= s;
    }

    /** @return rotation of this payload. */
    default float rotation(){
        return 0f;
    }

    /** writes the payload for saving. */
    void write(Writes write);

    /** @return icon describing the contents. */
    TextureRegion icon();

    /** @return content describing this payload (block or unit) */
    UnlockableContent content();

    @Override
    default float getX(){
        return x();
    }

    @Override
    default float getY(){
        return y();
    }

    static void write(@Nullable Payload payload, Writes write){
        if(payload == null){
            write.bool(false);
        }else{
            write.bool(true);
            payload.write(write);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static <T extends Payload> T read(Reads read){
        boolean exists = read.bool();
        if(!exists) return null;

        byte type = read.b();
        if(type == payloadBlock){
            Block block = content.block(read.s());
            BuildPayload payload = new BuildPayload(block, Team.derelict);
            byte version = read.b();
            payload.build.readAll(read, version);
            payload.build.tile = emptyTile;
            return (T)payload;
        }else if(type == payloadUnit){
            byte id = read.b();
            if(EntityMapping.map(id) == null) throw new RuntimeException("No type with ID " + id + " found.");
            Unit unit = (Unit)EntityMapping.map(id).get();
            unit.read(read);
            return (T)new UnitPayload(unit);
        }
        throw new IllegalArgumentException("Unknown payload type: " + type);
    }
}
