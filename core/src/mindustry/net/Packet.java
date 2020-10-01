package mindustry.net;

import arc.util.pooling.Pool.Poolable;

import java.nio.ByteBuffer;

public interface Packet extends Poolable{
    default void read(ByteBuffer buffer){}
    default void write(ByteBuffer buffer){}
    default void reset(){}

    default boolean isImportant(){
        return false;
    }

    default boolean isUnimportant(){
        return false;
    }
}
