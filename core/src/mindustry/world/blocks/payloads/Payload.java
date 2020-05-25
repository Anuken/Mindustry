package mindustry.world.blocks.payloads;

import arc.util.ArcAnnotate.*;
import arc.util.io.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.content;

public interface Payload{
    int payloadUnit = 0, payloadBlock = 1;

    /** draws this payload at a position. */
    void draw(float x, float y, float rotation);

    /** writes the payload for saving. */
    void write(Writes write);

    /** sets this payload's position on the map. */
    void set(float x, float y);

    /** @return whether this payload was dumped. */
    default boolean dump(float x, float y, float rotation){
        return false;
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
            BlockPayload payload = new BlockPayload(block, Team.derelict);
            byte version = read.b();
            payload.entity.readAll(read, version);
            return (T)payload;
        }else if(type == payloadUnit){
            byte id = read.b();
            Unitc unit = (Unitc)EntityMapping.map(id).get();
            unit.read(read);
            return (T)new UnitPayload(unit);
        }
        throw new IllegalArgumentException("Unknown payload type: " + type);
    }
}
