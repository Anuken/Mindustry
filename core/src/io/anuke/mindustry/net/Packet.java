package io.anuke.mindustry.net;

import com.badlogic.gdx.utils.Pool.Poolable;

import java.nio.ByteBuffer;

public interface Packet extends Poolable{
    void read(ByteBuffer buffer);
    void write(ByteBuffer buffer);

    default void reset() {}

    interface ImportantPacket{}
    interface UnimportantPacket{}
}
